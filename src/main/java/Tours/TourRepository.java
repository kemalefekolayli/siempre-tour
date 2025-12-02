package Tours;


import Tours.Models.Tour;
import Tours.Models.TourCategory;
import Tours.Models.TourStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {

    List<Tour> findByIsActiveTrue();

    List<Tour> findByStatus(TourStatus status);

    List<Tour> findByCategory(TourCategory category);

    List<Tour> findByCreatedBy(String userId);

    List<Tour> findByStartDateAfter(LocalDateTime date);

    List<Tour> findByIsActiveTrueAndStatusAndStartDateAfter(
            TourStatus status, LocalDateTime date);
}
