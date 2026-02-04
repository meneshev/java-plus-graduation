package analyzer.service;

import analyzer.dal.entity.Interaction;
import analyzer.dal.entity.Similarity;
import analyzer.dal.repository.EventRatingSum;
import analyzer.dal.repository.InteractionRepository;
import analyzer.dal.repository.SimilarityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {
    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @Override
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        return interactionRepository.getInteractionRating(eventIds).stream()
                .collect(
                        Collectors.toMap(EventRatingSum::getEventId, EventRatingSum::getSum)
                );
    }

    @Override
    public Map<Long, Double> getSimilarEvents(Long eventId, Long userId, Integer limit) {
        List<Long> interactedEventIds = interactionRepository.findAllInteractedEventsByUserId(userId, limit);
        List<Similarity> similarities = similarityRepository.getSimilarEvents(interactedEventIds, List.of(eventId), limit);
        return similarities.stream()
                .collect(Collectors.toMap(
                        s -> s.getEvent1().equals(eventId) ? s.getEvent2() : s.getEvent1(),
                        Similarity::getSimilarity,
                        Math::max,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<Long, Double> getRecommendations(Long userId, Integer limit) {
        List<Long> interactedEventIds = interactionRepository.findAllInteractedEventsByUserId(userId, limit);
        if (interactedEventIds.isEmpty()) {
            return Map.of();
        }

        List<Similarity> candidates = similarityRepository.getNotInteractedEvents(interactedEventIds, limit);

        Map<Long, Double> recommendations = new LinkedHashMap<>();

        for (Similarity similarity : candidates) {
            Long candidateEvnId = interactedEventIds.contains(similarity.getEvent1()) ? similarity.getEvent2() : similarity.getEvent1();

            if (interactedEventIds.contains(candidateEvnId) || recommendations.containsKey(candidateEvnId)) {
                continue;
            }

            double possibleRating = getPossibleRating(candidateEvnId, userId, interactedEventIds);
            recommendations.put(candidateEvnId, possibleRating);

            if (recommendations.size() >= limit) {
                break;
            }
        }

        return recommendations;
    }

    private double getPossibleRating(Long candidateEvnId, Long userId, List<Long> interactedEventIds) {
        List<Similarity> interacted = similarityRepository.findInteractedEvents(candidateEvnId, interactedEventIds);

        if (interacted.isEmpty()) {
            return 0.0;
        }

        Map<Long, Double> similarities = interacted.stream()
                .collect(Collectors.toMap(
                        s -> s.getEvent1().equals(candidateEvnId) ? s.getEvent2() : s.getEvent1(),
                        Similarity::getSimilarity,
                        (a, b) -> a
                ));


        List<Interaction> interactions =
                interactionRepository.findByUserIdAndEventIds(userId, similarities.keySet().stream().toList());

        Map<Long, Double> ratings = interactions.stream()
                .collect(Collectors.toMap(Interaction::getEventId, Interaction::getRating));

        double weightedSum = 0.0;
        double simSum = 0.0;

        for (var e : similarities.entrySet()) {
            Long eventId = e.getKey();
            double sim = e.getValue();
            Double rating = ratings.get(eventId);
            if (rating == null) continue;

            weightedSum += sim * rating;
            simSum += sim;
        }

        return simSum == 0.0 ? 0.0 : (weightedSum / simSum);
    }


    @Override
    public void processEventSimilarity(EventSimilarityAvro avro) {
        Similarity similarity = similarityRepository.findSimilarityByEvent1AndEvent2(avro.getEventA(),  avro.getEventB());
        if (similarity != null) {
            similarity.setSimilarity(avro.getScore());
            similarity.setTs(avro.getTimestamp().atOffset(ZoneOffset.UTC));
            similarityRepository.save(similarity);
            log.info("Similarity updated: {}", similarity);
        } else {
            similarity = similarityRepository.save(Similarity.builder()
                            .event1(avro.getEventA())
                            .event2(avro.getEventB())
                            .similarity(avro.getScore())
                            .ts(avro.getTimestamp().atOffset(ZoneOffset.UTC))
                            .build()
            );
            log.info("Similarity created: {}", similarity);
        }
    }

    @Override
    public void processUserAction(UserActionAvro userActionAvro) {
        Interaction interaction = interactionRepository.findInteractionByUserIdAndEventId(userActionAvro.getUserId(), userActionAvro.getEventId());
        double newRating = getWeight(userActionAvro.getActionType());
        if (interaction != null) {
            if (interaction.getRating() < newRating) {
                interaction.setRating(newRating);
                interaction.setTs(userActionAvro.getTimestamp().atOffset(ZoneOffset.UTC));
                interactionRepository.save(interaction);
                log.info("Interaction updated: {}", interaction);
            }
        } else {
            interaction = Interaction.builder()
                    .userId(userActionAvro.getUserId())
                    .eventId(userActionAvro.getEventId())
                    .rating(newRating)
                    .ts(userActionAvro.getTimestamp().atOffset(ZoneOffset.UTC))
                    .build();
            interactionRepository.save(interaction);
            log.info("Interaction created: {}", interaction);
        }
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
