package ru.practicum.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.HitDto;
import ru.practicum.HitStatDto;
import ru.practicum.StatClient;
import ru.practicum.client.RequestServiceClient;
import ru.practicum.client.UserServiceClient;
import ru.practicum.controller.params.EventGetByIdParams;
import ru.practicum.controller.params.EventUpdateParams;
import ru.practicum.controller.params.search.EventSearchParams;
import ru.practicum.controller.params.search.PublicSearchParams;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.entity.*;
import ru.practicum.enums.EventState;
import ru.practicum.enums.RequestStatus;
import ru.practicum.enums.StateAction;
import ru.practicum.exception.AccessException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.entity.QEvent.event;
import static ru.practicum.utils.Constants.TIMESTAMP_PATTERN;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserServiceClient userServiceClient;
    private final EventMapper eventMapper;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final RequestServiceClient requestServiceClient;
    private final LocationMapper locationMapper;

    private final StatClient statClient;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    @Override
    public EventFullDto create(long userId, NewEventDto newEventDto) {
        UserDto initiator = userServiceClient.getById(userId);
        Category category = categoryRepository.findById(newEventDto.category())
                .orElseThrow(() -> new NotFoundException("Category with id " + newEventDto.category() + " not found"));
        Location location = locationRepository.save(
                locationMapper.locationDtoToLocation(newEventDto.location()));
        Event event = eventMapper.newEventDtoToEvent(
                newEventDto, initiator.id(), category, location, LocalDateTime.now());
        Event savedEvent = eventRepository.save(event);
        savedEvent.setInitiator(initiator);
        savedEvent.setLocation(location);
        return eventMapper.eventToEventFullDto(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getAllByInitiator(EventSearchParams searchParams) {
        long initiatorId = searchParams.getPrivateSearchParams().getInitiatorId();
        UserDto userDto = userServiceClient.getById(initiatorId);
        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        List<Event> receivedEvents = eventRepository.findAllByInitiatorId(initiatorId, page);

        List<Long> eventIds = receivedEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> likesData = eventRepository.findLikesCountByEventIds(eventIds);
        Map<Long, Long> likesMap = new HashMap<>();
        for (Object[] data : likesData) {
            Long eventId = (Long) data[0];
            Long count = (Long) data[1];
            likesMap.put(eventId, count);
        }

        for (Event event : receivedEvents) {
            event.setLikes(likesMap.getOrDefault(event.getId(), 0L));
        }

        return receivedEvents.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getAllByPublic(EventSearchParams searchParams, HitDto hitDto) {

        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());

        BooleanExpression booleanExpression = event.isNotNull();

        PublicSearchParams publicSearchParams = searchParams.getPublicSearchParams();

        if (publicSearchParams.getText() != null) { //наличие поиска по тексту
            booleanExpression = booleanExpression.andAnyOf(
                    event.annotation.likeIgnoreCase(publicSearchParams.getText()),
                    event.description.likeIgnoreCase(publicSearchParams.getText())
            );
        }

        if (publicSearchParams.getCategories() != null) { // наличие поиска по категориям
            booleanExpression = booleanExpression.and(
                    event.category.id.in((publicSearchParams.getCategories())));
        }

        if (publicSearchParams.getPaid() != null) { // наличие поиска по категориям
            booleanExpression = booleanExpression.and(
                    event.paid.eq(publicSearchParams.getPaid()));
        }

        LocalDateTime rangeStart = publicSearchParams.getRangeStart();
        LocalDateTime rangeEnd = publicSearchParams.getRangeEnd();

        if (rangeStart != null && rangeEnd != null) { // наличие поиска дате события
            booleanExpression = booleanExpression.and(
                    event.eventDate.between(rangeStart, rangeEnd)
            );
        } else if (rangeStart != null) {
            booleanExpression = booleanExpression.and(
                    event.eventDate.after(rangeStart)
            );
            rangeEnd = rangeStart.plusYears(100);
        } else if (publicSearchParams.getRangeEnd() != null) {
            booleanExpression = booleanExpression.and(
                    event.eventDate.before(rangeEnd)
            );
            rangeStart = LocalDateTime.parse(LocalDateTime.now().format(dateTimeFormatter), dateTimeFormatter);
        }

        if (rangeEnd == null && rangeStart == null) {
            booleanExpression = booleanExpression.and(
                    event.eventDate.after(LocalDateTime.now())
            );
            rangeStart = LocalDateTime.parse(LocalDateTime.now().format(dateTimeFormatter), dateTimeFormatter);
            rangeEnd = rangeStart.plusYears(100);
        }

        List<Event> eventListBySearch = eventRepository.findAll(booleanExpression, page).getContent();

        statClient.saveHit(hitDto);

        if (eventListBySearch.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = new ArrayList<>();

        for (Event event : eventListBySearch) {
            List<HitStatDto> hitStatDtoList = statClient.getStats(
                    rangeStart.format(dateTimeFormatter),
                    rangeEnd.format(dateTimeFormatter),
                    List.of("/event/" + event.getId()),
                    false);
            Long view = 0L;
            for (HitStatDto hitStatDto : hitStatDtoList) {
                view += hitStatDto.getHits();
            }
            eventIds.add(event.getId());
            event.setViews(view);
        }

        Map<Long, Long> confirmedRequestsMap = requestServiceClient.countByStatusAndEventsIds(
                        RequestStatus.CONFIRMED, eventIds);

        Map<Long, Long> likesMap = eventRepository.findLikesCountByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        data -> (Long) data[0],
                        data -> (Long) data[1]));

        for (Event event : eventListBySearch) {
            event.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
            event.setLikes(likesMap.getOrDefault(event.getId(), 0L));
        }

        return eventListBySearch.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getTopEvent(Integer count, HitDto hitDto) {

        String rangeEnd = LocalDateTime.now().format(dateTimeFormatter);
        String rangeStart = LocalDateTime.now().minusYears(100).format(dateTimeFormatter);

        List<Event> eventListBySearch = eventRepository.findTop(count);
        if (eventListBySearch.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = new ArrayList<>();

        statClient.saveHit(hitDto);

        for (Event event : eventListBySearch) {
            List<HitStatDto> hitStatDtoList = statClient.getStats(
                    rangeStart,
                    rangeEnd,
                    List.of("/event/" + event.getId()),
                    true);
            Long view = 0L;
            for (HitStatDto hitStatDto : hitStatDtoList) {
                view += hitStatDto.getHits();
            }
            eventIds.add(event.getId());
            event.setViews(view);
        }

        Map<Long, Long> confirmedRequestsMap = requestServiceClient.countByStatusAndEventsIds(
                        RequestStatus.CONFIRMED, eventIds);

        Map<Long, Long> likesMap = eventRepository.findLikesCountByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        data -> (Long) data[0],
                        data -> (Long) data[1]));

        for (Event event : eventListBySearch) {
            event.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
            event.setLikes(likesMap.getOrDefault(event.getId(), 0L));
        }

        return eventListBySearch.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getTopViewEvent(Integer count, HitDto hitDto) {

        String rangeEnd = LocalDateTime.now().format(dateTimeFormatter);
        String rangeStart = LocalDateTime.now().minusYears(100).format(dateTimeFormatter);

        statClient.saveHit(hitDto);

        List<HitStatDto> hitStatDtoList = statClient.getStats(
                rangeStart,
                rangeEnd,
                null,
                true);

        Map<Long, Long> idsMap = hitStatDtoList.stream().filter(it -> it.getUri().matches("\\/events\\/\\d+$"))
                        .collect((Collectors.groupingBy(dto ->
                                Long.parseLong(dto.getUri().replace("/events/", "")),
                                Collectors.summingLong(HitStatDto::getHits))));

        Set<Long> ids = idsMap.keySet();
        List<Event> eventListBySearch = eventRepository.findAllById(ids);
        List<Event> result = new ArrayList<>();
        idsMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(count)
                .forEach(it -> {
                            Optional<Event> e = eventListBySearch.stream().filter(event ->
                                    event.getId() == it.getKey()).findFirst();
                            if (e.isPresent()) {
                                Event eventRes = e.get();
                                eventRes.setViews(it.getValue());
                                result.add(eventRes);
                            }
                        }
                );
        return result.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAllByAdmin(EventSearchParams searchParams) {
        Pageable page = PageRequest.of(
                searchParams.getFrom(), searchParams.getSize());

        BooleanExpression booleanExpression = event.isNotNull();

        if (searchParams.getAdminSearchParams().getUsers() != null) {
            booleanExpression = booleanExpression.and(
                    event.initiatorId.in(searchParams.getAdminSearchParams().getUsers()));
        }

        if (searchParams.getAdminSearchParams().getCategories() != null) {
            booleanExpression = booleanExpression.and(
                    event.category.id.in(searchParams.getAdminSearchParams().getCategories()));
        }

        if (searchParams.getAdminSearchParams().getStates() != null) {
            booleanExpression = booleanExpression.and(
                    event.state.in(searchParams.getAdminSearchParams().getStates()));
        }

        LocalDateTime rangeStart = searchParams.getAdminSearchParams().getRangeStart();
        LocalDateTime rangeEnd = searchParams.getAdminSearchParams().getRangeEnd();

        if (rangeStart != null && rangeEnd != null) {
            booleanExpression = booleanExpression.and(
                    event.eventDate.between(rangeStart, rangeEnd));
        } else if (rangeStart != null) {
            booleanExpression = booleanExpression.and(
                    event.eventDate.after(rangeStart));
        } else if (rangeEnd != null) {
            booleanExpression = booleanExpression.and(
                    event.eventDate.before(rangeEnd));
        }

        List<Event> receivedEventList = eventRepository.findAll(booleanExpression, page).stream().toList();
        if (receivedEventList.isEmpty()) return Collections.emptyList();

        List<Long> eventIds = new ArrayList<>();

        for (Event event : receivedEventList) {
            eventIds.add(event.getId());
        }

        Map<Long, Long> confirmedRequestsMap = requestServiceClient.countByStatusAndEventsIds(
                RequestStatus.CONFIRMED, eventIds);

        Map<Long, Long> likesMap = eventRepository.findLikesCountByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        data -> (Long) data[0],
                        data -> (Long) data[1]));

        for (Event event : receivedEventList) {
            event.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
            event.setLikes(likesMap.getOrDefault(event.getId(), 0L));
        }

        return receivedEventList
                .stream()
                .map(eventMapper::eventToEventFullDto)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getById(EventGetByIdParams params, HitDto hitDto) {
        Event receivedEvent;
        if (params.initiatorId() != null) {
            userServiceClient.checkExistence(params.initiatorId());
            receivedEvent = eventRepository.findByInitiatorIdAndId(params.initiatorId(), params.eventId())
                    .orElseThrow(() -> new NotFoundException(
                            "Event with id " + params.eventId() +
                                    " created by user with id " + params.initiatorId() + " not found"));
        } else {
            receivedEvent = eventRepository.findById(params.eventId())
                    .orElseThrow(() -> new NotFoundException("Event with id " + params.eventId() + " not found"));
            statClient.saveHit(hitDto);

            List<HitStatDto> hitStatDtoList = statClient.getStats(
                        "", "", List.of("/events/" + params.eventId()), true
            );
            Long view = 0L;
            for (HitStatDto hitStatDto : hitStatDtoList) {
                view += hitStatDto.getHits();
            }
            receivedEvent.setViews(view);
            receivedEvent.setConfirmedRequests(
                    requestServiceClient.countByStatusAndEventId(RequestStatus.CONFIRMED, receivedEvent.getId()));
            receivedEvent.setLikes(eventRepository.countLikesByEventId(receivedEvent.getId()));
        }
        return eventMapper.eventToEventFullDto(receivedEvent);
    }

    @Override
    public EventFullDto update(long eventId, EventUpdateParams updateParams) {
        Event event = findEventOrThrow(eventId);

        if (updateParams.updateEventUserRequest() != null) {
            handleUserUpdate(event, updateParams);
        }

        if (updateParams.updateEventAdminRequest() != null) {
            handleAdminUpdate(event, updateParams);
        }

        event.setId(eventId);
        Event updatedEvent = saveAndUpdateLikes(event);

        log.debug("Событие возвращенное из базы: {} ; {}", event.getId(), event.getState());
        return eventMapper.eventToEventFullDto(updatedEvent);
    }

    @Override
    public EventShortDto addLike(long userId, long eventId) {
        Event event = findEventOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event with id " + eventId + " is not published");
        }
        eventRepository.addLike(userId, eventId);
        event.setLikes(eventRepository.countLikesByEventId(eventId));
        return eventMapper.eventToEventShortDto(event);
    }

    @Override
    public void deleteLike(long userId, long eventId) {
        Event event = findEventOrThrow(eventId);
        boolean isLikeExist = eventRepository.checkLikeExisting(userId, eventId);
        if (isLikeExist) {
            eventRepository.deleteLike(userId, eventId);
        } else {
            throw new NotFoundException("Like for event: " + eventId + " by user: " + userId + " not exist");
        }
    }

    @Override
    public EventFullDto getByIdInternal(long eventId) {
        Event savedEvent = findEventOrThrow(eventId);
        savedEvent.setInitiator(userServiceClient.getById(savedEvent.getInitiatorId()));
        return eventMapper.eventToEventFullDto(savedEvent);
    }

    private Event findEventOrThrow(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));
    }

    private void handleUserUpdate(Event event, EventUpdateParams updateParams) {
        UpdateEventUserRequest userData = updateParams.updateEventUserRequest();
        validateUserExists(updateParams.userId());
        validateUserIsInitiator(event, updateParams.userId());

        updateCategory(event, userData.category());
        validateEventStateForUser(event);
        validateEventDate(userData.eventDate(), 2, "User");
        applyUserStateAction(event, userData.stateAction());

        eventMapper.updateEventUserRequestToEvent(event, userData);
        log.debug("Private. Событие после мапинга: {}", event);
    }

    private void handleAdminUpdate(Event event, EventUpdateParams updateParams) {
        UpdateEventAdminRequest adminData = updateParams.updateEventAdminRequest();
        updateCategory(event, adminData.category());

        validateEventStateForAdmin(event);
        validateEventDate(adminData.eventDate(), 1, "Admin");

        eventMapper.updateEventAdminRequestToEvent(event, adminData);
        log.debug("Admin. Событие после мапинга: {}", event.getId(), event.getState());
    }

    private void validateUserExists(Long userId) {
        userServiceClient.checkExistence(userId);
    }

    private void validateUserIsInitiator(Event event, Long userId) {
        if (!userId.equals(event.getInitiatorId())) {
            throw new AccessException("User with id = " + userId + " is not the initiator");
        }
    }

    private void validateEventStateForUser(Event event) {
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("User. Only pending or canceled events can be changed");
        }
    }

    private void validateEventStateForAdmin(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Admin. Only pending events can be changed");
        }
    }

    private void validateEventDate(LocalDateTime eventDate, int hours, String role) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new ConflictException(role + ". Event date must be at least " + hours + " hours from now");
        }
    }

    private void applyUserStateAction(Event event, StateAction stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
        }
    }

    private void updateCategory(Event event, Long categoryId) {
        if (categoryId == null) return;

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " not found"));
        event.setCategory(category);
    }

    private Event saveAndUpdateLikes(Event event) {
        Event savedEvent = eventRepository.save(event);
        savedEvent.setLikes(eventRepository.countLikesByEventId(savedEvent.getId()));
        return savedEvent;
    }
}
