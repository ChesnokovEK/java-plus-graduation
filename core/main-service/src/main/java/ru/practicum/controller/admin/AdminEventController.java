package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.controller.params.EventUpdateParams;
import ru.practicum.controller.params.search.AdminSearchParams;
import ru.practicum.controller.params.search.EventSearchParams;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.entity.EventState;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private final EventService eventService;

    @PatchMapping("{eventId}")
    public EventFullDto update(
            @PathVariable long eventId,
            @RequestBody @Valid UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("==> PATCH /admin/events/{}; Update event by admin: {}", eventId, updateEventAdminRequest);
        EventFullDto updatedEvent = eventService.update(
                eventId, new EventUpdateParams(null, null, updateEventAdminRequest));
        log.info("<== PATCH /admin/events/{}; Updated event by admin: {}", eventId, updatedEvent);
        return updatedEvent;
    }

    @GetMapping
    public List<EventFullDto> getAll(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        log.info("==> GET /admin/events Searching events with params: " +
                "users {}, states: {}, categories: {}, rangeStart: {}, rangeEnd: {}, from: {}, size: {}",
                users, states, categories, rangeStart, rangeEnd, from, size);
        AdminSearchParams adminSearchParams = AdminSearchParams.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();
        EventSearchParams eventSearchParams = EventSearchParams.builder()
                .adminSearchParams(adminSearchParams)
                .from(from)
                .size(size)
                .build();
        List<EventFullDto> receivedEventSearch = eventService.getAllByAdmin(eventSearchParams);
        log.info("==> GET /admin/events Searching events with params: " +
                        "users {}, states: {}, categories: {}, rangeStart: {}, rangeEnd: {}, from: {}, size: {}",
                users, states, categories, rangeStart, rangeEnd, from, size);
        return receivedEventSearch;
    }

}
