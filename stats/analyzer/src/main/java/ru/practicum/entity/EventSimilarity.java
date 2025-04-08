package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "events_similarity")
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventA;
    private Long eventB;
    private Float score;
    private Instant timestamp;
}