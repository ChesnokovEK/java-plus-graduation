package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatServiceClientAdapter {

    private final StatClient statClient;

    public void saveHit(HitDto dto) {
        statClient.saveHit(dto);
    }

    public List<HitStatDto> getStats(String start,
                                     String end,
                                     List<String> uris,
                                     Boolean unique) {
        try {
            return statClient.getStats(start, end, uris, unique);
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}