package ru.practicum.mainservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.mainservice.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    @Query("select req " +
            "from ParticipationRequest req " +
            "where req.event.initiator.id = :userId and req.event.id = :eventId")
    List<ParticipationRequest> findRequestsByEventIdAndUserId(Long userId, Long eventId);

    List<ParticipationRequest> findParticipationRequestByEventIdAndIdIn(Long eventId, List<Long> requestIds);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);
}
