package ru.practicum.controller.params.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.controller.params.search.EventSearchParams;
import ru.practicum.controller.params.search.PublicSearchParams;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.controller.params.mapper.PublicSearchParamsMapper.mapToPublicSearchParams;

@Component
public class PublicEventSearchParamsMapper {
    public static EventSearchParams mapToEventSearchParams(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Integer from,
            Integer size
    ) {
        PublicSearchParams publicSearchParams = mapToPublicSearchParams(
                text, categories, paid, rangeStart, rangeEnd
        );

        return EventSearchParams.builder()
                .publicSearchParams(publicSearchParams)
                .from(from)
                .size(size)
                .build();
    }

    public static EventSearchParams mapToEventSearchParams(
            PublicSearchParams publicSearchParams,
            Integer from,
            Integer size
    ) {
        return EventSearchParams.builder()
                .publicSearchParams(publicSearchParams)
                .from(from)
                .size(size)
                .build();
    }
}
