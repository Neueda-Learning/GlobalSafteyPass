package com.travelassistant.repository;
import com.travelassistant.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CardRepository extends JpaRepository<Card,String> { List<Card> findByCustomerId(String customerId); }
