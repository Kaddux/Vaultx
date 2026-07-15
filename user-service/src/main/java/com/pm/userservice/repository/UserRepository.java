package com.pm.userservice.repository;

import com.pm.userservice.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users,UUID> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findById(UUID id);

    boolean existsByEmail(String email);


}
