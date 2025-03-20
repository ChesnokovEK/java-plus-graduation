package ru.practicum.controller.params.search;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Builder
public class EventSearchParams {

    private AdminSearchParams adminSearchParams;
    private PrivateSearchParams privateSearchParams;
    private PublicSearchParams publicSearchParams;

    private Integer from;
    private Integer size;

}
