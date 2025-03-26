package ru.practicum.controller.params.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.controller.params.search.AdminSearchParams;
import ru.practicum.controller.params.search.EventSearchParams;
import ru.practicum.entity.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AdminEventSearchParamsMapper {

    private final AdminSearchParamsMapper adminSearchParamsMapper;

    public AdminEventSearchParamsMapper(AdminSearchParamsMapper adminSearchParamsMapper) {
        this.adminSearchParamsMapper = adminSearchParamsMapper;
    }

    public EventSearchParams mapToEventSearchParams(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Integer from,
            Integer size
    ) {
        AdminSearchParams adminSearchParams = adminSearchParamsMapper.mapToAdminSearchParams(
                users, states, categories, rangeStart, rangeEnd
        );

        return EventSearchParams.builder()
                .adminSearchParams(adminSearchParams)
                .from(from)
                .size(size)
                .build();
    }

    public EventSearchParams mapToEventSearchParams(
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
