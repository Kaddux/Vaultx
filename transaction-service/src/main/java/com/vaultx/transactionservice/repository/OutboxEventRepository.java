package com.vaultx.transactionservice.repository;

import com.vaultx.transactionservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
        SELECT o FROM OutboxEvent o
        WHERE o.published = false
        ORDER BY o.createdAt ASC
        LIMIT 100
    """)
    List<OutboxEvent> findUnpublished();
}
