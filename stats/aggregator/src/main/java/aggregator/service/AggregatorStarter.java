package aggregator.service;

import aggregator.kafka.AggregatorKafkaClient;
import config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorStarter {
    private final KafkaProperties kafkaProperties;
    private final AggregatorKafkaClient kafkaClient;
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightsSum = new HashMap<>();
    private final MinWeightSum minEventWeightSum = new MinWeightSum();


    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaClient.getConsumer()::wakeup));
        log.info("Aggregator started. Consuming topic='{}', producing topic='{}'",
                kafkaProperties.getTopics().getUserActions(),
                kafkaProperties.getTopics().getEventsSimilarity());

        try {
            while (true) {
                List<EventSimilarityAvro> userActions = process(kafkaClient.getConsumer().poll(kafkaProperties.getConsumer().getTimeoutMs()));

                if (!userActions.isEmpty()) {
                    userActions.forEach(eventSimilarity -> {
                        log.info("Sending event similarity: {}", eventSimilarity);
                        kafkaClient.getProducer().send(
                                new ProducerRecord<>(kafkaProperties.getTopics().getEventsSimilarity(), null, eventSimilarity)
                        );
                    });
                }
            }
        } catch (WakeupException ignore) {

        } catch (Exception e) {
            log.error("Error during process topic:{}", kafkaProperties.getTopics().getEventsSimilarity(), e);
        } finally {
            try {
                kafkaClient.getProducer().flush();
                kafkaClient.getConsumer().commitSync();
            } finally {
                log.info("Closing aggregator consumer and producer...");
                kafkaClient.stop();
            }
        }
    }

    private List<EventSimilarityAvro> process(ConsumerRecords<Void, UserActionAvro> userActions) {
        List<EventSimilarityAvro> eventsSimilarities = new ArrayList<>();

        for (ConsumerRecord<Void, UserActionAvro> userAction : userActions) {
            Long userId = userAction.value().getUserId();
            Long eventId_A = userAction.value().getEventId();
            double eventA_w = getWeight(userAction.value().getActionType());

            if (eventUserWeights.containsKey(eventId_A)) {
                Map<Long, Double> eventAUserWeights = eventUserWeights.get(eventId_A);
                double oldWeight = eventAUserWeights.getOrDefault(userId, 0.0);
                double newWeight = Math.max(oldWeight, eventA_w);

                if (newWeight != oldWeight) {
                    eventAUserWeights.put(userId, newWeight);
                    double delta = newWeight - oldWeight;
                    eventWeightsSum.put(eventId_A, eventWeightsSum.getOrDefault(eventId_A, 0.0) + delta);

                    for (Long eventId_B : eventUserWeights.keySet()) {
                        if (!eventId_A.equals(eventId_B)) {
                            Double eventB_w = eventUserWeights.get(eventId_B).get(userId);
                            if (eventB_w != null) {
                                double minOld = Math.min(oldWeight, eventB_w);
                                double minNew = Math.min(newWeight, eventB_w);
                                double minDelta = minNew - minOld;
                                double S_min = minEventWeightSum.get(eventId_A, eventId_B) + minDelta;
                                minEventWeightSum.put(eventId_A, eventId_B, S_min);
                                double sqrtWeightsSumA = Math.sqrt(eventWeightsSum.get(eventId_A));
                                double sqrtWeightsSumB = Math.sqrt(eventWeightsSum.get(eventId_B));
                                double score = S_min / (sqrtWeightsSumA * sqrtWeightsSumB);

                                long first = Math.min(eventId_A, eventId_B);
                                long second = Math.max(eventId_A, eventId_B);

                                eventsSimilarities.add(EventSimilarityAvro.newBuilder()
                                        .setEventA(first)
                                        .setEventB(second)
                                        .setScore(score)
                                        .setTimestamp(Instant.now())
                                        .build()
                                );
                            }
                        }
                    }
                }
            } else { // новое мероприятие
                eventsSimilarities.addAll(newEventInteraction(userId, eventId_A, eventA_w));
            }
        }
        return eventsSimilarities;
    }

    private List<EventSimilarityAvro> newEventInteraction(Long userId, Long eventId_A, double eventA_w) {
        log.info("New event interaction for userId: {}, eventId: {}", userId, eventId_A);
        eventUserWeights.computeIfAbsent(eventId_A, k -> new HashMap<>(Map.of(userId, eventA_w)));
        eventWeightsSum.put(eventId_A, eventA_w);
        List<EventSimilarityAvro> eventSimilarities = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long eventId_B = entry.getKey();
            if (eventId_A != eventId_B) {
                Double eventB_w = entry.getValue().getOrDefault(userId, 0.0);
                if (eventB_w != 0.0) {
                    double S_min = Math.min(eventA_w, eventB_w);
                    minEventWeightSum.put(eventId_A, eventId_B, S_min);

                    double sqrtWeightsSumA = Math.sqrt(eventWeightsSum.get(eventId_A));
                    double sqrtWeightsSumB = Math.sqrt(eventWeightsSum.get(eventId_B));
                    double score = S_min / (sqrtWeightsSumA * sqrtWeightsSumB);

                    long first = Math.min(eventId_A, eventId_B);
                    long second = Math.max(eventId_A, eventId_B);

                    eventSimilarities.add(EventSimilarityAvro.newBuilder()
                            .setEventA(first)
                            .setEventB(second)
                            .setScore(score)
                            .setTimestamp(Instant.now())
                            .build()
                    );
                }
            }
        }

        return eventSimilarities;
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }


    static class MinWeightSum {
        private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

        public void put(long eventA, long eventB, double sum) {
            long first  = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);

            minWeightsSums
                    .computeIfAbsent(first, e -> new HashMap<>())
                    .put(second, sum);
        }

        public double get(long eventA, long eventB) {
            long first  = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);

            return minWeightsSums
                    .computeIfAbsent(first, e -> new HashMap<>())
                    .getOrDefault(second, 0.0);
        }
    }
}