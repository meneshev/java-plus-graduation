package analyzer.dal.repository;

import analyzer.dal.entity.Similarity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {

    Similarity findSimilarityByEvent1AndEvent2(Long event1, Long event2);
}
