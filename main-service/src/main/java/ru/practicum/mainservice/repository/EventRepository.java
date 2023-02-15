package ru.practicum.mainservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.mainservice.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    Page<Event> findEventsByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findEventByIdAndInitiatorId(Long eventId, Long initiatorId);

    Set<Event> findByIdIn(List<Long> ids);
}
