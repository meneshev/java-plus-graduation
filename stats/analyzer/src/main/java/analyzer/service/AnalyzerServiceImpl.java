package analyzer.service;

import analyzer.dal.entity.Interaction;
import analyzer.dal.entity.Similarity;
import analyzer.dal.repository.EventRatingSum;
import analyzer.dal.repository.InteractionRepository;
import analyzer.dal.repository.SimilarityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {
    private final InteractionRepository interactionRepository;
    private final SimilarityRepository similarityRepository;

    @Override
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<EventRatingSum> rows = interactionRepository.getInteractionRating(eventIds);
        if (rows == null ||  rows.isEmpty()) {
            log.info("No interactions found for {} eventIds", eventIds.size());
            return Map.of();
        }

        Map<Long, Double> result = rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getEventId() != null && r.getSum() != null)
                .collect(Collectors.toMap(EventRatingSum::getEventId, EventRatingSum::getSum));

        log.info("Interactions count calculated for {} events (requestedIds={})", result.size(), eventIds.size());
        return result;
    }

    @Override
    public Map<Long, Double> getSimilarEvents(Long eventId, Long userId, Integer limit) {
        if (eventId == null || userId == null || limit == null || limit <= 0) {
            return Map.of();
        }

        List<Long> interactedEventIds = interactionRepository.findAllInteractedEventsByUserId(userId, limit);

        List<Similarity> similarities = similarityRepository.findSimilarNotInteracted(
                interactedEventIds,
                eventId,
                PageRequest.of(0, limit)
        );

        Map<Long, Double> result = similarities.stream()
                .collect(Collectors.toMap(
                        s -> s.getEvent1().equals(eventId) ? s.getEvent2() : s.getEvent1(),
                        Similarity::getSimilarity,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        log.info("Similar events for eventId={} userId={} -> {} results", eventId, userId, result.size());

        return result;
    }

    @Override
    public Map<Long, Double> getRecommendations(Long userId, Integer limit) {
        if (userId == null || limit == null || limit <= 0) {
            return Map.of();
        }

        List<Long> interactedEventIds = interactionRepository.findAllInteractedEventsByUserId(userId, limit);

        if (interactedEventIds.isEmpty()) {
            log.info("No interactions found for userId={}, returning empty recommendations", userId);
            return Map.of();
        }

        List<Similarity> candidates = similarityRepository.getNotInteractedEvents(
                interactedEventIds,
                PageRequest.of(0, limit)
        );

        Map<Long, Double> recommendations = new LinkedHashMap<>();

        log.info("Building recommendations: userId={}, limit={}, recentInteractions={}, candidatesFetched={}",
                userId, limit, interactedEventIds.size(), candidates.size());

        for (Similarity similarity : candidates) {
            Long candidateEvnId = interactedEventIds.contains(similarity.getEvent1())
                    ? similarity.getEvent2()
                    : similarity.getEvent1();

            if (interactedEventIds.contains(candidateEvnId) || recommendations.containsKey(candidateEvnId)) {
                continue;
            }

            double possibleRating = getPossibleRating(candidateEvnId, userId, interactedEventIds);

            if (possibleRating < 0.0) {
                continue;
            }

            recommendations.put(candidateEvnId, possibleRating);

            if (recommendations.size() >= limit) {
                break;
            }
        }

        log.info("Recommendations built: userId={}, requestedLimit={}, actualReturned={}",
                userId, limit, recommendations.size());

        return recommendations;
    }

    private double getPossibleRating(Long candidateEvnId, Long userId, List<Long> interactedEventIds) {
        List<Similarity> interacted = similarityRepository.findInteractedEvents(
                candidateEvnId,
                interactedEventIds,
                PageRequest.of(0, 10)
        );

        if (interacted.isEmpty()) {
            log.info("No interacted events for candidateEventId={} userId={}, skip", candidateEvnId, userId);
            return -1.0;
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
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Interaction::getEventId,
                        Interaction::getRating,
                        (a, b) -> a
                ));


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

        if (simSum == 0.0) {
            log.info("Similarity sum is 0 for candidateEventId={} userId={}, skip", candidateEvnId, userId);
            return -1.0;
        }

        double result = weightedSum / simSum;

        log.info("Predicted rating: userId={}, candidateEventId={}, predicted={}, simSum={}, similarities={}",
                userId, candidateEvnId, result, simSum, similarities.size());

        return result;
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
        Interaction interaction = interactionRepository.findInteractionByUserIdAndEventId(
                userActionAvro.getUserId(),
                userActionAvro.getEventId()
        );

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
