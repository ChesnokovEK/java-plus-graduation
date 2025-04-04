package ru.practicum.controller.params.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.controller.params.search.AdminSearchParams;
import ru.practicum.controller.params.search.EventSearchParams;
import ru.practicum.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.controller.params.mapper.AdminSearchParamsMapper.mapToAdminSearchParams;

@Component
public class AdminEventSearchParamsMapper {
    public static EventSearchParams mapToEventSearchParams(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Integer from,
            Integer size
    ) {
        AdminSearchParams adminSearchParams = mapToAdminSearchParams(
                users, states, categories, rangeStart, rangeEnd
        );

        return EventSearchParams.builder()
                .adminSearchParams(adminSearchParams)
                .from(from)
                .size(size)
                .build();
    }

    public static EventSearchParams mapToEventSearchParams(
            AdminSearchParams adminSearchParams,
            Integer from,
            Integer size
    ) {
        return EventSearchParams.builder()
                .adminSearchParams(adminSearchParams)
                .from(from)
                .size(size)
                .build();
    }
}
