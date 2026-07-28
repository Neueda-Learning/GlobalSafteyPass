package com.travelassistant.repository;
import com.travelassistant.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,String> {}
