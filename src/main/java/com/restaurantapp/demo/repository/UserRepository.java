package com.restaurantapp.demo.repository;

import com.restaurantapp.demo.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "staff")
    @Query("select u from User u")
    List<User> findAllWithStaff();
}
