package com.globalsafetypass.repository;
import com.globalsafetypass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, Long> {}

