package alexspeal.service;

import alexspeal.config.ApplicationConfig;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.models.ParticipantSchedule;
import alexspeal.models.TimeInterval;
import alexspeal.repositories.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantScheduleService {
    private final MeetingRepository meetingRepository;
    private final TimeIntervalService intervalService;
    private final ApplicationConfig applicationConfig;

    public ParticipantSchedule build(EventParticipantEntity participant, int duration) {

        Set<LocalDate> days = participant.getDays().stream()
                .map(DayEntity::getDate)
                .collect(Collectors.toSet());

        Map<LocalDate, List<TimeInterval>> busy = loadBusy(participant, days);
        Map<LocalDate, List<TimeInterval>> availability = new HashMap<>();

        for (LocalDate day : days) {
            List<TimeInterval> free = intervalService.subtract(
                    new TimeInterval(applicationConfig.workStart(), applicationConfig.workEnd()),
                    busy.getOrDefault(day, List.of())
            );

            List<TimeInterval> valid = free.stream()
                    .filter(iv -> Duration.between(iv.start(), iv.end()).toMinutes() >= duration)
                    .toList();

            availability.put(day, valid);
        }

        return new ParticipantSchedule(new ArrayList<>(days), availability);
    }

    private Map<LocalDate, List<TimeInterval>> loadBusy(
            EventParticipantEntity participant,
            Set<LocalDate> days
    ) {
        Map<LocalDate, List<TimeInterval>> busy = new HashMap<>();

        meetingRepository.getBusyIntervals(participant.getUser().getId(), new ArrayList<>(days))
                .forEach(dto -> {
                    LocalDateTime start = dto.startTime();
                    LocalDate day = start.toLocalDate();

                    if (!days.contains(day)) return;

                    LocalDateTime end = start.plusMinutes(dto.duration());

                    busy.computeIfAbsent(day, d -> new ArrayList<>())
                            .add(new TimeInterval(start.toLocalTime(), end.toLocalTime()));
                });

        return busy;
    }
}