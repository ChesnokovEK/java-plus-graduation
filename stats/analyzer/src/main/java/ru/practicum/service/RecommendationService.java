package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.RecommendationsMessages;
import ru.practicum.entity.EventSimilarity;
import ru.practicum.entity.RecommendedEvent;
import ru.practicum.entity.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepo;
    private final EventSimilarityRepository similarityRepo;

    public List<RecommendedEvent> getSimilarEvents(RecommendationsMessages.SimilarEventsRequestProto request) {
        long eventId = request.getEventId();
        long userId  = request.getUserId();
        int maxRes   = request.getMaxResults();

        Set<Long> interacted = userInteracted(userId);
        List<RecommendedEvent> result, recList = new ArrayList<>();

        similarityRepo.findByEventAOrEventB(eventId, eventId)
                .forEach(e -> {
                    long other = (e.getEventA() == eventId) ? e.getEventB() : e.getEventA();
                    if (!interacted.contains(other)) {
                        recList.add(new RecommendedEvent(other, e.getScore()));
                    }
                });
        result = recList.stream()
                .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed()).toList();

        return result.size() <= maxRes ? result : result.subList(0, maxRes);
    }

    public List<RecommendedEvent> getRecommendationsForUser(RecommendationsMessages.UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int maxRes  = request.getMaxResults();

        List<UserAction> all = userActionRepo.findByUserId(userId);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        all.sort((a,b) -> b.getLastInteraction().compareTo(a.getLastInteraction()));

        int min = Math.min(5, all.size());
        List<UserAction> recent = all.subList(0, min);

        Set<Long> interacted = userInteracted(userId);

        List<Long> events = recent.stream()
                .map(UserAction::getEventId)
                .toList();

        List<EventSimilarity> allSimilarities = similarityRepo.findByEventAInOrEventBIn(events);

        Map<Long, Float> bestScoreMap = new HashMap<>();

        for (EventSimilarity similarity : allSimilarities) {
            long eventA = similarity.getEventA();
            long eventB = similarity.getEventB();
            float score = similarity.getScore();

            if (events.contains(eventA)) {
                long other = eventB;
                if (!interacted.contains(other)) {
                    updateBestScore(bestScoreMap, other, score);
                }
            }
            if (events.contains(eventB)) {
                long other = eventA;
                if (!interacted.contains(other)) {
                    updateBestScore(bestScoreMap, other, score);
                }
            }
        }

        return bestScoreMap.entrySet().stream()
                .map(e -> new RecommendedEvent(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                .limit(maxRes)
                .collect(Collectors.toList());
    }

    public List<RecommendedEvent> getInteractionsCount(RecommendationsMessages.InteractionsCountRequestProto request) {
        List<Long> events = request.getEventIdList();

        List<UserAction> allUserActions = userActionRepo.findByEventIdIn(events);

        Map<Long, Double> sumMap = new HashMap<>();
        for (UserAction ua : allUserActions) {
            long eventId = ua.getEventId();
            sumMap.merge(eventId, ua.getMaxWeight(), Double::sum);
        }

        List<RecommendedEvent> result = new ArrayList<>();
        for (Long eventId : events) {
            double sum = sumMap.getOrDefault(eventId, 0.0);
            result.add(new RecommendedEvent(eventId, (float) sum));
        }
        return result;
    }

    private Set<Long> userInteracted(long userId) {
        return userActionRepo.findByUserId(userId)
                .stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());
    }

    private void updateBestScore(Map<Long, Float> bestScoreMap, long other, float score) {
        float currentScore = bestScoreMap.getOrDefault(other, 0f);
        if (score > currentScore) {
            bestScoreMap.put(other, score);
        }
    }
}
