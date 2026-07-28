package com.travelassistant.repository;
import com.travelassistant.model.ReadinessAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ReadinessAssessmentRepository extends JpaRepository<ReadinessAssessment,String> { Optional<ReadinessAssessment> findByTripId(String tripId); }
