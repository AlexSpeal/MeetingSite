package alexspeal.service;

import alexspeal.config.ApplicationConfig;
import alexspeal.dto.BusyIntervalDto;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.models.ParticipantSchedule;
import alexspeal.models.TimeInterval;
import alexspeal.repositories.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final String FALLBACK_TIMEZONE = "UTC";

    public ParticipantSchedule build(EventParticipantEntity participant, int duration) {
        return build(participant, duration, null, null, false);
    }

    public ParticipantSchedule build(EventParticipantEntity participant,
                                     int duration,
                                     LocalTime preferredRangeStart,
                                     LocalTime preferredRangeEnd,
                                     boolean ignoreMovablePersonalEvents) {
        ZoneId zone = resolveZone(participant);

        Set<LocalDate> localDays = participant.getDays().stream()
                .map(DayEntity::getDate)
                .collect(Collectors.toSet());

        if (localDays.isEmpty()) {
            return new ParticipantSchedule(List.of(), Map.of());
        }

        LocalDate minDay = localDays.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate maxDay = localDays.stream().max(Comparator.naturalOrder()).orElseThrow();

        OffsetDateTime fetchFrom = minDay.minusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime fetchTo = maxDay.plusDays(2).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<BusyIntervalDto> busyMeetings = meetingRepository.getBusyIntervals(
                participant.getUser().getId(), fetchFrom, fetchTo);

        List<BusyIntervalDto> blockingBusy = ignoreMovablePersonalEvents
                ? busyMeetings.stream().filter(b -> !isMovablePersonal(b)).toList()
                : busyMeetings;

        Integer dailyLoadMinutes = participant.getUser().getDailyLoadMinutes();

        LocalTime workStart = applicationConfig.workStart();
        LocalTime workEnd = applicationConfig.workEnd();
        LocalTime effectiveStart = preferredRangeStart != null && preferredRangeStart.isAfter(workStart)
                ? preferredRangeStart : workStart;
        LocalTime effectiveEnd = preferredRangeEnd != null && preferredRangeEnd.isBefore(workEnd)
                ? preferredRangeEnd : workEnd;

        Map<LocalDate, List<TimeInterval>> availability = new HashMap<>();

        if (!effectiveStart.isBefore(effectiveEnd)) {
            return new ParticipantSchedule(new ArrayList<>(localDays), availability);
        }

        for (LocalDate localDay : localDays) {
            if (exceedsDailyLoad(busyMeetings, localDay, zone, duration, dailyLoadMinutes)) {
                continue;
            }

            ZonedDateTime workStartZdt = localDay.atTime(effectiveStart).atZone(zone);
            ZonedDateTime workEndZdt = localDay.atTime(effectiveEnd).atZone(zone);

            OffsetDateTime workStartUtc = workStartZdt.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime workEndUtc = workEndZdt.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);

            List<TimeInterval> busyInWindow = collectBusyInWindow(blockingBusy, workStartUtc, workEndUtc);

            List<Map.Entry<LocalDate, TimeInterval>> windowEntries =
                    splitWorkWindow(workStartUtc, workEndUtc);

            for (Map.Entry<LocalDate, TimeInterval> entry : windowEntries) {
                LocalDate utcDate = entry.getKey();
                TimeInterval window = entry.getValue();

                List<TimeInterval> busyForWindow = busyInWindow.stream()
                        .filter(b -> b.start().isBefore(window.end()) && b.end().isAfter(window.start()))
                        .map(b -> new TimeInterval(
                                max(b.start(), window.start()),
                                min(b.end(), window.end())
                        ))
                        .toList();

                List<TimeInterval> free = intervalService.subtract(window, busyForWindow);
                List<TimeInterval> valid = free.stream()
                        .filter(iv -> Duration.between(iv.start(), iv.end()).toMinutes() >= duration)
                        .toList();

                if (!valid.isEmpty()) {
                    availability.merge(utcDate, new ArrayList<>(valid), (existing, added) -> {
                        existing.addAll(added);
                        return existing;
                    });
                }
            }
        }

        return new ParticipantSchedule(new ArrayList<>(localDays), availability);
    }

    private boolean isMovablePersonal(BusyIntervalDto dto) {
        return Boolean.TRUE.equals(dto.isPersonal()) && Boolean.FALSE.equals(dto.isFixed());
    }

    private List<TimeInterval> collectBusyInWindow(
            List<BusyIntervalDto> busyMeetings,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd
    ) {
        List<TimeInterval> result = new ArrayList<>();
        for (BusyIntervalDto dto : busyMeetings) {
            OffsetDateTime bStart = dto.startTime().withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime bEnd = bStart.plusMinutes(dto.duration());

            if (bEnd.isAfter(windowStart) && bStart.isBefore(windowEnd)) {
                OffsetDateTime clStart = bStart.isBefore(windowStart) ? windowStart : bStart;
                OffsetDateTime clEnd = bEnd.isAfter(windowEnd) ? windowEnd : bEnd;
                result.add(new TimeInterval(clStart.toLocalTime(), clEnd.toLocalTime()));
            }
        }
        return result;
    }

    private List<Map.Entry<LocalDate, TimeInterval>> splitWorkWindow(
            OffsetDateTime startUtc,
            OffsetDateTime endUtc
    ) {
        LocalDate startDate = startUtc.toLocalDate();
        LocalDate endDate = endUtc.toLocalDate();

        if (!endDate.isAfter(startDate)) {
            return List.of(Map.entry(startDate, new TimeInterval(startUtc.toLocalTime(), endUtc.toLocalTime())));
        }

        List<Map.Entry<LocalDate, TimeInterval>> entries = new ArrayList<>();
        entries.add(Map.entry(startDate, new TimeInterval(startUtc.toLocalTime(), LocalTime.MAX)));
        entries.add(Map.entry(endDate, new TimeInterval(LocalTime.MIN, endUtc.toLocalTime())));
        return entries;
    }

    private boolean exceedsDailyLoad(
            List<BusyIntervalDto> busyMeetings,
            LocalDate localDay,
            ZoneId zone,
            int newMeetingDuration,
            Integer dailyLoadMinutes
    ) {
        if (dailyLoadMinutes == null) {
            return false;
        }

        OffsetDateTime dayStartUtc = localDay.atStartOfDay(zone)
                .toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime dayEndUtc = localDay.plusDays(1).atStartOfDay(zone)
                .toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);

        long existingLoad = 0L;
        for (BusyIntervalDto dto : busyMeetings) {
            OffsetDateTime bStart = dto.startTime().withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime bEnd = bStart.plusMinutes(dto.duration());

            if (!bEnd.isAfter(dayStartUtc) || !bStart.isBefore(dayEndUtc)) {
                continue;
            }

            OffsetDateTime clipStart = bStart.isBefore(dayStartUtc) ? dayStartUtc : bStart;
            OffsetDateTime clipEnd = bEnd.isAfter(dayEndUtc) ? dayEndUtc : bEnd;
            existingLoad += Duration.between(clipStart, clipEnd).toMinutes();
        }

        return existingLoad + newMeetingDuration > dailyLoadMinutes;
    }

    private ZoneId resolveZone(EventParticipantEntity participant) {
        String tz = participant.getUser().getTimezone();
        if (tz == null || tz.isBlank()) {
            return ZoneId.of(FALLBACK_TIMEZONE);
        }
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.of(FALLBACK_TIMEZONE);
        }
    }

    private LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
