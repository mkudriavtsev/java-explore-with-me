package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.event.*;
import ru.practicum.mainservice.dto.participationRequest.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.dto.participationRequest.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.dto.participationRequest.ParticipationRequestDto;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface EventService {
    EventFullDto create(NewEventDto dto, Long userId);

    List<EventShortDto> getEventsByUserId(Long userId, Integer from, Integer size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto patch(UpdateEventUserRequest request, Long userId, Long eventId);

    List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request);

    List<EventShortDto> getAll(GetEventsRequest request, Integer from, Integer size, HttpServletRequest httpRequest);

    EventFullDto getById(Long id, HttpServletRequest request);

    List<EventFullDto> getAllAdmin(GetEventsRequestAdmin request, Integer from, Integer size);

    EventFullDto confirmOrReject(UpdateEventAdminRequest request, Long eventId);
}
