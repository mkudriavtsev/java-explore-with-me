package ru.practicum.mainservice.service.impl;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.mainservice.dto.event.*;
import ru.practicum.mainservice.dto.participationRequest.EventRequestStatusUpdateRequest;
import ru.practicum.mainservice.dto.participationRequest.EventRequestStatusUpdateResult;
import ru.practicum.mainservice.dto.participationRequest.ParticipationRequestDto;
import ru.practicum.mainservice.dto.participationRequest.ParticipationRequestStatus;
import ru.practicum.mainservice.exception.ConflictException;
import ru.practicum.mainservice.exception.EventDateNotValidException;
import ru.practicum.mainservice.exception.NotFoundException;
import ru.practicum.mainservice.exception.WrongStateException;
import ru.practicum.mainservice.mapper.EventMapper;
import ru.practicum.mainservice.mapper.ParticipationRequestMapper;
import ru.practicum.mainservice.model.*;
import ru.practicum.mainservice.model.QEvent;
import ru.practicum.mainservice.repository.EventRepository;
import ru.practicum.mainservice.repository.ParticipationRequestRepository;
import ru.practicum.mainservice.service.CategoryService;
import ru.practicum.mainservice.service.EventService;
import ru.practicum.mainservice.service.StatsService;
import ru.practicum.mainservice.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final Sort SORT_BY_EVENT_DATE = Sort.by(Sort.Direction.ASC, "eventDate");

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;
    private final ParticipationRequestMapper requestMapper;
    private final StatsService statsService;

    @Override
    @Transactional
    public EventFullDto create(NewEventDto dto, Long userId) {
        checkEventDate(dto.getEventDate());
        User initiator = userService.getEntityById(userId);
        Category category = categoryService.getEntityById(dto.getCategory());
        Event event = eventMapper.toEntity(dto);
        event.setInitiator(initiator);
        event.setCategory(category);
        Event savedEvent = eventRepository.save(event);
        log.info("Event with id {} created", savedEvent.getId());
        return eventMapper.toFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getEventsByUserId(Long userId, Integer from, Integer size) {
        userService.getEntityById(userId);
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findEventsByInitiatorId(userId, pageRequest).getContent();
        List<EventShortDto> dtos = eventMapper.toShortDtoList(events);
        statsService.setStatsViewsToEventShortDtos(dtos);
        return dtos;
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findEventByIdAndInitiatorId(eventId, userId).orElseThrow(() -> {
            throw new NotFoundException("Event with id " + eventId + " not found");
        });
        EventFullDto dto = eventMapper.toFullDto(event);
        statsService.setStatsViewsToEventFullDtos(List.of(dto));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto patch(UpdateEventUserRequest request, Long userId, Long eventId) {
        if (Objects.nonNull(request.getEventDate())) {
            checkEventDate(request.getEventDate());
        }
        Event event = eventRepository.findEventByIdAndInitiatorId(eventId, userId).orElseThrow(() -> {
            throw new NotFoundException("Event with id " + eventId + " not found");
        });
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        eventMapper.patchEventByUserRequest(request, event);
        if (Objects.nonNull(request.getStateAction())) {
            if (request.getStateAction().equals(UserStateAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            }
            if (request.getStateAction().equals(UserStateAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELED);
            }
        }
        if (Objects.nonNull(request.getCategory())) {
            Category category = categoryService.getEntityById(request.getCategory());
            event.setCategory(category);
        }
        return eventMapper.toFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        List<ParticipationRequest> requests = requestRepository.findRequestsByEventIdAndUserId(userId, eventId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        Event event = eventRepository.findEventByIdAndInitiatorId(eventId, userId).orElseThrow(() -> {
            throw new NotFoundException("Event with id " + eventId + " not found");
        });
        if (event.getParticipantLimit() != 0 && (event.getParticipantLimit() - event.getConfirmedRequests()) <= 0) {
            throw new ConflictException("No space available to participate in the event");
        }
        List<ParticipationRequest> requests = requestRepository.findParticipationRequestByEventIdAndIdIn(
                eventId, dto.getRequestIds());
        if (event.getParticipantLimit().equals(0)) {
            return setRequestStatusEventWithoutLimit(dto.getStatus(), requests, event);
        } else {
            return setRequestStatusEventWithLimit(dto.getStatus(), requests, event);
        }
    }

    @Override
    public List<EventShortDto> getAll(
            GetEventsRequest request, Integer from, Integer size, HttpServletRequest httpRequest) {
        PageRequest page = PageRequest.of(from / size, size, SORT_BY_EVENT_DATE);
        BooleanBuilder predicate = createPredicate(request);
        if (Objects.isNull(request.getRangeStart()) && Objects.isNull(request.getRangeEnd())) {
            predicate.and(QEvent.event.eventDate.after(LocalDateTime.now()));
        }
        List<EventShortDto> dtos = eventMapper.toShortDtoList(eventRepository.findAll(predicate, page).getContent());
        statsService.saveStats(httpRequest);
        statsService.setStatsViewsToEventShortDtos(dtos);
        if (Objects.nonNull(request.getSort())) {
            if (request.getSort().equalsIgnoreCase("views")) {
                dtos.sort(Comparator.comparing(EventShortDto::getViews));
            }
        }
        return dtos;
    }

    @Override
    public EventFullDto getById(Long id, HttpServletRequest request) {
        Event event = eventRepository.findById(id).orElseThrow(() -> {
            throw new NotFoundException("Event with id " + id + " not found");
        });
        statsService.saveStats(request);
        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventFullDto> getAllAdmin(GetEventsRequestAdmin request, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size);
        BooleanBuilder predicate = createPredicateAdmin(request);
        return eventMapper.toFullDtoList(eventRepository.findAll(predicate, page).getContent());
    }

    @Override
    @Transactional
    public EventFullDto confirmOrReject(UpdateEventAdminRequest request, Long eventId) {
        checkEventDate(request.getEventDate());
        Event event = eventRepository.findById(eventId).orElseThrow(() -> {
            throw new NotFoundException("Event with id " + eventId + " not found");
        });
        if (!event.getState().equals(EventState.PUBLISHED)) {
            if (Objects.nonNull(request.getStateAction())) {
                if (request.getStateAction().equals(AdminStateAction.PUBLISH_EVENT) &&
                        event.getState().equals(EventState.PENDING)) {
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                } else if (request.getStateAction().equals(AdminStateAction.REJECT_EVENT)) {
                    event.setState(EventState.CANCELED);
                } else {
                    throw new WrongStateException(
                            "Cannot publish the event because it's not in the right state: " + event.getState());
                }
            }
            eventMapper.patchEventByAdminRequest(request, event);
            return eventMapper.toFullDto(eventRepository.save(event));
        }
        throw new WrongStateException(
                "Cannot publish the event because it's not in the right state: " + event.getState());
    }

    private void checkEventDate(LocalDateTime dateTime) {
        if (Objects.nonNull(dateTime)) {
            if (dateTime.minusHours(2).isBefore(LocalDateTime.now())) {
                throw new EventDateNotValidException("Event time cannot be earlier than two hours from now");
            }
        }
    }

    private EventRequestStatusUpdateResult setRequestStatusEventWithoutLimit(
            ParticipationRequestStatus status,
            List<ParticipationRequest> requests,
            Event event) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        for (ParticipationRequest req : requests) {
            if (req.getStatus().equals(RequestStatus.PENDING)) {
                if (status.equals(ParticipationRequestStatus.CONFIRMED)) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    result.getConfirmedRequests().add(requestMapper.toDto(req));
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(requestMapper.toDto(req));
                }
            } else {
                throw new ConflictException("Can only confirm PENDING requests");
            }
        }
        return result;
    }

    private EventRequestStatusUpdateResult setRequestStatusEventWithLimit(
            ParticipationRequestStatus status,
            List<ParticipationRequest> requests,
            Event event) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        for (ParticipationRequest req : requests) {
            if (req.getStatus().equals(RequestStatus.PENDING)) {
                if (status.equals(ParticipationRequestStatus.CONFIRMED)) {
                    long limit = event.getParticipantLimit() - event.getConfirmedRequests();
                    if (limit > 0) {
                        req.setStatus(RequestStatus.CONFIRMED);
                        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                        result.getConfirmedRequests().add(requestMapper.toDto(req));
                    } else {
                        req.setStatus(RequestStatus.REJECTED);
                        result.getRejectedRequests().add(requestMapper.toDto(req));
                    }
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(requestMapper.toDto(req));
                }
            } else {
                throw new ConflictException("Can only confirm PENDING requests");
            }
        }
        return result;
    }

    private BooleanBuilder createPredicate(GetEventsRequest req) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (Objects.nonNull(req.getText())) {
            predicate.and(QEvent.event.annotation.likeIgnoreCase(req.getText())
                    .or(QEvent.event.description.likeIgnoreCase(req.getText())));
        }
        if (!CollectionUtils.isEmpty(req.getCategories())) {
            predicate.and(QEvent.event.category.id.in(req.getCategories()));
        }
        if (Objects.nonNull(req.getPaid())) {
            predicate.and(QEvent.event.paid.eq(req.getPaid()));
        }
        if (Objects.nonNull(req.getRangeStart())) {
            predicate.and(QEvent.event.eventDate.after(req.getRangeStart()));
        }
        if (Objects.nonNull(req.getRangeEnd())) {
            predicate.and(QEvent.event.eventDate.before(req.getRangeEnd()));
        }
        if (Boolean.TRUE.equals(req.getOnlyAvailable())) {
            predicate.and(QEvent.event.participantLimit.eq(0)
                    .or(QEvent.event.participantLimit.gt(QEvent.event.confirmedRequests)));
        }
        return predicate;
    }

    private BooleanBuilder createPredicateAdmin(GetEventsRequestAdmin req) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (!CollectionUtils.isEmpty(req.getUsers())) {
            predicate.and(QEvent.event.initiator.id.in(req.getUsers()));
        }
        if (!CollectionUtils.isEmpty(req.getStates())) {
            predicate.and(QEvent.event.state.in(req.getStates()));
        }
        if (!CollectionUtils.isEmpty(req.getCategories())) {
            predicate.and(QEvent.event.category.id.in(req.getCategories()));
        }
        if (Objects.nonNull(req.getRangeStart())) {
            predicate.and(QEvent.event.eventDate.after(req.getRangeStart()));
        }
        if (Objects.nonNull(req.getRangeEnd())) {
            predicate.and(QEvent.event.eventDate.before(req.getRangeEnd()));
        }
        return predicate;
    }
}
