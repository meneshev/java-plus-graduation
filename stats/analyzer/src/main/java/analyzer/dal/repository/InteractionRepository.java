package analyzer.dal.repository;

import analyzer.dal.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Interaction findInteractionByUserIdAndEventId(long userId, long eventId);

    @Query("""
        select
             i.eventId,
             sum(i.rating)
        from Interaction i
             where i.eventId in :eventIds
        group by i.eventId
       """)
    List<EventRatingSum> getInteractionRating(List<Long> eventIds);
}
