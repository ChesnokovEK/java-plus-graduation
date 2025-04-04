package ru.practicum.controller.params.search;

import lombok.*;

@Data
@RequiredArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class EventSearchParams {

    AdminSearchParams adminSearchParams;
    PrivateSearchParams privateSearchParams;
    PublicSearchParams publicSearchParams;

    Integer from;
    Integer size;

}
