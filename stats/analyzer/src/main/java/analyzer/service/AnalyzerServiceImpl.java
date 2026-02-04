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
        return Map.of();
    }

    @Override
    public Map<Long, Double> getRecommendations(Long userId, Integer limit) {
        return Map.of();
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
