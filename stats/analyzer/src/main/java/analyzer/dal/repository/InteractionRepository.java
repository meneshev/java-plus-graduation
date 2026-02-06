package analyzer.dal.repository;

import analyzer.dal.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Interaction findInteractionByUserIdAndEventId(long userId, long eventId);

    @Query("""
        select
             i.eventId as eventId,
             sum(i.rating) as sum
        from Interaction i
             where i.eventId in :eventIds
        group by i.eventId
       """)
    List<EventRatingSum> getInteractionRating(List<Long> eventIds);

    @Query("""
        select
             i.eventId
        from Interaction i
             where i.userId = :userId
        order by i.ts desc
        limit :limit
        """)
    List<Long> findAllInteractedEventsByUserId(Long userId, Integer limit);

    @Query("""
        select i
        from Interaction i
        where i.userId = :userId
          and i.eventId in :eventIds
        """)
    List<Interaction> findByUserIdAndEventIds(Long userId, List<Long> eventIds);
}
