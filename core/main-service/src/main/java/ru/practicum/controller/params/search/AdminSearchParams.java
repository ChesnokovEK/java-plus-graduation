package ru.practicum.controller.params.search;

import lombok.*;
import ru.practicum.entity.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Data
@RequiredArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class AdminSearchParams {

    private List<Long> users;
    private List<EventState> states;
    private List<Long> categories;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
}
