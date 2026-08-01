package com.pm.notificationservice.repository;

import com.pm.notificationservice.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    Optional<UserPreference> findByUserIdAndEventType(UUID userId, String eventType);

    List<UserPreference> findByUserId(UUID userId);
}
