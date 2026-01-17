package com.ankur.outbox.repository;

import com.ankur.outbox.model.OutboxEvent;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends CrudRepository<OutboxEvent, Long> {

    @Query("SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit")
    List<OutboxEvent> findPendingEvents(int limit);
}