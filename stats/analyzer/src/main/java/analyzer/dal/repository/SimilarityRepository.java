package analyzer.dal.repository;

import analyzer.dal.entity.Similarity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {

    Similarity findSimilarityByEvent1AndEvent2(Long event1, Long event2);

    @Query("""
        select
             s
        from Similarity s
        where (s.event1 = :eventId and s.event2 not in :interactedIds)
             or (s.event2 = :eventId and s.event1 not in :interactedIds)
        order by s.similarity desc
        """)
    List<Similarity> findSimilarNotInteracted(List<Long> interactedIds, Long eventId, Pageable pageable);

    @Query("""
        select s
        from Similarity s
        where (s.event1 in :interactedIds and s.event2 not in :interactedIds)
             or (s.event2 in :interactedIds and s.event1 not in :interactedIds)
        order by s.similarity desc
        """)
    List<Similarity> getNotInteractedEvents(List<Long> interactedIds, Pageable pageable);

    @Query("""
        select s
        from Similarity s
        where (s.event1 = :candidateId and s.event2 in :interactedIds)
           or (s.event2 = :candidateId and s.event1 in :interactedIds)
        order by s.similarity desc
        """)
    List<Similarity> findInteractedEvents(Long candidateId, List<Long> interactedIds, Pageable pageable);
}
