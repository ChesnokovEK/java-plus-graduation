package ru.practicum.controller.params.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.controller.params.search.EventSearchParams;
import ru.practicum.controller.params.search.PublicSearchParams;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PublicEventSearchParamsMapper {

    private final PublicSearchParamsMapper publicSearchParamsMapper;

    public PublicEventSearchParamsMapper(PublicSearchParamsMapper publicSearchParamsMapper) {
        this.publicSearchParamsMapper = publicSearchParamsMapper;
    }

    public EventSearchParams mapToEventSearchParams(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Integer from,
            Integer size
    ) {
        PublicSearchParams publicSearchParams = publicSearchParamsMapper.mapToPublicSearchParams(
                text, categories, paid, rangeStart, rangeEnd
        );

        return EventSearchParams.builder()
                .publicSearchParams(publicSearchParams)
                .from(from)
                .size(size)
                .build();
    }

    public EventSearchParams mapToEventSearchParams(
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
