package alexspeal.service;

import alexspeal.config.ApplicationConfig;
import alexspeal.dto.BusyIntervalDto;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.entities.UserEntity;
import alexspeal.enums.ErrorMessage;
import alexspeal.repositories.MeetingParticipantRepository;
import alexspeal.repositories.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalEventOptimizer {

    private static final int SLOT_MINUTES = 5;
    private static final String FALLBACK_TIMEZONE = "UTC";
    private static final String SOLVER_TIME_LIMIT = "10s";

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ApplicationConfig applicationConfig;

    public Map<EventEntity, OffsetDateTime> planRelocations(
            UserEntity user,
            Long newEventIdOrNull,
            OffsetDateTime newStartUtc,
            int newDurationMinutes
    ) {
        OffsetDateTime newEndUtc = newStartUtc.plusMinutes(newDurationMinutes);
        ZoneId userZone = resolveZone(user);

        OffsetDateTime fetchFrom = newStartUtc.minusDays(2);
        OffsetDateTime fetchTo = newEndUtc.plusDays(2);
        List<EventEntity> nearMovables = meetingRepository
                .findUserMovablePersonalEvents(user.getId(), fetchFrom, fetchTo);

        List<EventEntity> conflicts = nearMovables.stream()
                .filter(e -> newEventIdOrNull == null || !e.getId().equals(newEventIdOrNull))
                .filter(e -> overlaps(e, newStartUtc, newEndUtc))
                .toList();

        if (conflicts.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<LocalDate>> possibleDaysByEvent = new java.util.HashMap<>();
        Set<LocalDate> allCandidateDays = new TreeSet<>();
        for (EventEntity c : conflicts) {
            List<LocalDate> days = loadPossibleDays(c, user.getId());
            possibleDaysByEvent.put(c.getId(), days);
            allCandidateDays.addAll(days);
        }
        if (allCandidateDays.isEmpty()) {
            throw new IllegalStateException(
                    ErrorMessage.RELOCATION_INFEASIBLE.getMessage(conflicts.get(0).getTitle()));
        }

        LocalDate minDay = allCandidateDays.iterator().next();
        LocalDate maxDay = ((TreeSet<LocalDate>) allCandidateDays).last();

        OffsetDateTime windowStart = minDay.minusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime windowEnd = maxDay.plusDays(2).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        List<BusyIntervalDto> allBusy = meetingRepository.getBusyIntervals(user.getId(), windowStart, windowEnd);

        Set<Long> conflictIds = conflicts.stream().map(EventEntity::getId).collect(Collectors.toSet());
        List<long[]> blockedIntervals = new ArrayList<>();
        for (BusyIntervalDto b : allBusy) {
            if (conflictIds.contains(b.eventId())) continue;
            if (newEventIdOrNull != null && b.eventId() != null && b.eventId().equals(newEventIdOrNull)) continue;
            long sMin = epochMinute(b.startTime());
            long eMin = sMin + b.duration();
            blockedIntervals.add(new long[]{sMin, eMin});
        }
        blockedIntervals.add(new long[]{epochMinute(newStartUtc), epochMinute(newStartUtc) + newDurationMinutes});

        long epochRefMin = minDay.atStartOfDay(ZoneOffset.UTC).toEpochSecond() / 60L;

        int n = conflicts.size();
        int[][] domains = new int[n][];
        int[] origStarts = new int[n];
        int[] durations = new int[n];

        for (int i = 0; i < n; i++) {
            EventEntity c = conflicts.get(i);
            durations[i] = c.getDuration();
            origStarts[i] = (int) (epochMinute(c.getStartTime()) - epochRefMin);

            List<Integer> domain = computeDomain(
                    c, possibleDaysByEvent.get(c.getId()),
                    userZone, blockedIntervals, epochRefMin, durations[i]);

            if (domain.isEmpty()) {
                throw new IllegalStateException(
                        ErrorMessage.RELOCATION_INFEASIBLE.getMessage(c.getTitle()));
            }
            domains[i] = domain.stream().mapToInt(Integer::intValue).distinct().sorted().toArray();
        }

        return solveCsp(conflicts, domains, origStarts, durations, epochRefMin);
    }

    private Map<EventEntity, OffsetDateTime> solveCsp(
            List<EventEntity> conflicts,
            int[][] domains,
            int[] origStarts,
            int[] durations,
            long epochRefMin
    ) {
        int n = conflicts.size();

        Model model = new Model("personal-event-relocation");
        IntVar[] starts = new IntVar[n];
        IntVar[] heights = new IntVar[n];
        IntVar[] absDiffs = new IntVar[n];
        Task[] tasks = new Task[n];

        int maxStart = 0;
        for (int[] d : domains) maxStart = Math.max(maxStart, d[d.length - 1]);
        int maxDur = Arrays.stream(durations).max().orElse(0);
        int maxAbsDiff = Math.max(maxStart, maxDur) + maxStart + maxDur;

        for (int i = 0; i < n; i++) {
            starts[i] = model.intVar("s_" + i, domains[i]);
            int eMin = domains[i][0] + durations[i];
            int eMax = domains[i][domains[i].length - 1] + durations[i];
            IntVar dur = model.intVar(durations[i]);
            IntVar end = model.intVar("e_" + i, eMin, eMax);
            tasks[i] = new Task(starts[i], dur, end);
            heights[i] = model.intVar(1);

            absDiffs[i] = model.intVar("d_" + i, 0, maxAbsDiff);
            model.distance(starts[i], model.intVar(origStarts[i]), "=", absDiffs[i]).post();
        }

        if (n > 1) {
            model.cumulative(tasks, heights, model.intVar(1)).post();
        }

        IntVar total = model.intVar("total", 0, n * maxAbsDiff);
        model.sum(absDiffs, "=", total).post();

        Solver solver = model.getSolver();
        solver.limitTime(SOLVER_TIME_LIMIT);

        Solution sol = solver.findOptimalSolution(total, false);
        if (sol == null) {
            String firstTitle = conflicts.get(0).getTitle();
            throw new IllegalStateException(ErrorMessage.RELOCATION_INFEASIBLE.getMessage(firstTitle));
        }

        Map<EventEntity, OffsetDateTime> plan = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int newOffset = sol.getIntVal(starts[i]);
            if (newOffset == origStarts[i]) {
                continue;
            }
            long absMin = epochRefMin + newOffset;
            OffsetDateTime newStart = OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(absMin * 60L), ZoneOffset.UTC);
            plan.put(conflicts.get(i), newStart);
        }

        log.info("Planned {} relocations (totalDisplacement={})", plan.size(), sol.getIntVal(total));
        return plan;
    }

    private List<Integer> computeDomain(
            EventEntity event,
            List<LocalDate> possibleDays,
            ZoneId userZone,
            List<long[]> blockedIntervals,
            long epochRefMin,
            int duration
    ) {
        LocalTime workStart = applicationConfig.workStart();
        LocalTime workEnd = applicationConfig.workEnd();
        LocalTime preferredStart = event.getPreferredWindowStart();
        LocalTime preferredEnd = event.getPreferredWindowEnd();
        LocalTime effStart = (preferredStart != null && preferredStart.isAfter(workStart)) ? preferredStart : workStart;
        LocalTime effEnd = (preferredEnd != null && preferredEnd.isBefore(workEnd)) ? preferredEnd : workEnd;

        if (!effStart.isBefore(effEnd)) {
            return List.of();
        }

        Set<Integer> domain = new HashSet<>();
        for (LocalDate d : possibleDays) {
            ZonedDateTime startZ = d.atTime(effStart).atZone(userZone);
            ZonedDateTime endZ = d.atTime(effEnd).atZone(userZone);
            long startMin = startZ.toEpochSecond() / 60L;
            long endMin = endZ.toEpochSecond() / 60L;

            for (long[] free : subtractBlocked(startMin, endMin, blockedIntervals)) {
                long fs = free[0];
                long fe = free[1];
                long aligned = ((fs + SLOT_MINUTES - 1) / SLOT_MINUTES) * SLOT_MINUTES;
                while (aligned + duration <= fe) {
                    domain.add((int) (aligned - epochRefMin));
                    aligned += SLOT_MINUTES;
                }
            }
        }
        return new ArrayList<>(domain);
    }

    private List<long[]> subtractBlocked(long start, long end, List<long[]> blocks) {
        List<long[]> clipped = blocks.stream()
                .filter(b -> b[1] > start && b[0] < end)
                .map(b -> new long[]{Math.max(b[0], start), Math.min(b[1], end)})
                .sorted(Comparator.comparingLong(b -> b[0]))
                .toList();

        List<long[]> result = new ArrayList<>();
        long cursor = start;
        for (long[] b : clipped) {
            if (b[0] > cursor) {
                result.add(new long[]{cursor, b[0]});
            }
            cursor = Math.max(cursor, b[1]);
        }
        if (cursor < end) {
            result.add(new long[]{cursor, end});
        }
        return result;
    }

    private List<LocalDate> loadPossibleDays(EventEntity event, Long userId) {
        EventParticipantEntity authorPart = meetingParticipantRepository
                .findByEventIdAndUserId(event.getId(), userId)
                .orElseThrow(() -> new NoSuchElementException(
                        ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));
        return authorPart.getDays().stream()
                .map(DayEntity::getDate)
                .sorted()
                .toList();
    }

    private boolean overlaps(EventEntity e, OffsetDateTime newStart, OffsetDateTime newEnd) {
        OffsetDateTime eStart = e.getStartTime();
        if (eStart == null) return false;
        OffsetDateTime eEnd = eStart.plusMinutes(e.getDuration());
        return eEnd.isAfter(newStart) && eStart.isBefore(newEnd);
    }

    private long epochMinute(OffsetDateTime odt) {
        return odt.toEpochSecond() / 60L;
    }

    private ZoneId resolveZone(UserEntity user) {
        String tz = user.getTimezone();
        if (tz == null || tz.isBlank()) return ZoneId.of(FALLBACK_TIMEZONE);
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.of(FALLBACK_TIMEZONE);
        }
    }
}
