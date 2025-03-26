package ru.practicum.controller.params.search;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Getter
@Builder
public class EventSearchParams {

    AdminSearchParams adminSearchParams;
    PrivateSearchParams privateSearchParams;
    PublicSearchParams publicSearchParams;

    Integer from;
    Integer size;

}
