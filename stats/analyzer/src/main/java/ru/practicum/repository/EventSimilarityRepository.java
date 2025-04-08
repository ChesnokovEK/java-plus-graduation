package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.entity.EventSimilarity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    List<EventSimilarity> findByEventAOrEventB(Long eventA, Long eventB);

    @Query(value = "SELECT e from EventSimilarity e WHERE e.eventA IN ?1 OR e.eventB IN ?1")
    List<EventSimilarity> findByEventAInOrEventBIn(List<Long> eventIds);
}
