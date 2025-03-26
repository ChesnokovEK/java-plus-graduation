package ru.practicum.controller.params.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.controller.params.search.AdminSearchParams;
import ru.practicum.entity.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AdminSearchParamsMapper {

    public AdminSearchParams mapToAdminSearchParams(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        return AdminSearchParams.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();
    }
}
