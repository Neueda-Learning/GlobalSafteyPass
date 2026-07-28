package travelassistant.repository;

import travelassistant.model.Alert;
import travelassistant.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    List<Alert> findAllByOrderByCreatedAtDesc();
}
