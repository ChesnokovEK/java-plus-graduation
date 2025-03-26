package ru.practicum.controller.params.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.controller.params.search.PublicSearchParams;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PublicSearchParamsMapper {

    public PublicSearchParams mapToPublicSearchParams(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        return PublicSearchParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();
    }
}
