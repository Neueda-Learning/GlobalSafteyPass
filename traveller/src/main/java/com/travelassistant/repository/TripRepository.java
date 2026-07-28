package com.travelassistant.repository;
import com.travelassistant.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TripRepository extends JpaRepository<Trip,String> { List<Trip> findByCustomerIdOrderByStartDateDesc(String customerId); }
