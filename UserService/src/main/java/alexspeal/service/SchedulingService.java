package alexspeal.service;

import alexspeal.dto.responses.AvailabilityIntervalsResponse;
import alexspeal.entities.DayEntity;
import alexspeal.entities.EventEntity;
import alexspeal.entities.EventParticipantEntity;
import alexspeal.enums.AcceptStatusParticipant;
import alexspeal.enums.ErrorMessage;
import alexspeal.helpers.AvailabilityCalculator;
import alexspeal.models.AvailabilitySegment;
import alexspeal.models.Interval;
import alexspeal.models.ParticipantSchedule;
import alexspeal.models.ParticipantScheduleInfo;
import alexspeal.models.TimeInterval;
import alexspeal.repositories.MeetingParticipantRepository;
import alexspeal.repositories.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final ParticipantScheduleService scheduleService;
    private final RequiredWindowService requiredWindowService;
    private final AvailabilityCalculator availabilityCalculator;
    private final IntervalService intervalService;

    public AvailabilityIntervalsResponse getMeetingAvailability(Long meetingId) {
        EventEntity meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NoSuchElementException(ErrorMessage.MEETING_NOT_FOUND.getMessage()));

        int duration = meeting.getDuration();

        boolean hasPending = meeting.getEventParticipants().stream()
                .anyMatch(participant -> participant.getStatus() == AcceptStatusParticipant.PENDING);

        Long authorId = meeting.getAuthor().getId();

        EventParticipantEntity author = participantRepository
                .findByEventIdAndUserId(meetingId, authorId)
                .orElseThrow(() -> new IllegalStateException(ErrorMessage.NOT_FOUND_AUTHOR.getMessage()));

        List<LocalDate> dates = author.getDays().stream()
                .map(DayEntity::getDate)
                .toList();

        List<EventParticipantEntity> meetingParticipants = meeting.getEventParticipants();

        if (hasNotAcceptedRequiredParticipant(meetingParticipants)) {
            return new AvailabilityIntervalsResponse(
                    meetingId,
                    List.of(),
                    0,
                    hasPending
            );
        }

        List<EventParticipantEntity> acceptedParticipants = loadAcceptedParticipants(meetingParticipants, author);

        List<ParticipantScheduleInfo> schedulesInfo = acceptedParticipants.stream()
                .map(participant -> new ParticipantScheduleInfo(
                        scheduleService.build(participant, duration),
                        participant.isRequired()
                ))
                .toList();

        List<ParticipantSchedule> allSchedules = schedulesInfo.stream()
                .map(ParticipantScheduleInfo::schedule)
                .toList();

        List<ParticipantSchedule> requiredSchedules = schedulesInfo.stream()
                .filter(ParticipantScheduleInfo::required)
                .map(ParticipantScheduleInfo::schedule)
                .toList();

        ParticipantSchedule authorSchedule = scheduleService.build(author, duration);

        List<Interval> resultIntervals = new ArrayList<>();
        int globalMaxParticipants = 0;

        for (LocalDate date : dates) {
            List<TimeInterval> allowedWindows = resolveAllowedWindows(
                    date,
                    requiredSchedules,
                    authorSchedule,
                    duration
            );

            if (allowedWindows.isEmpty()) {
                continue;
            }

            List<AvailabilitySegment> segments = availabilityCalculator.calculateSegments(
                    date,
                    allSchedules,
                    allowedWindows
            );

            List<AvailabilitySegment> validSegments = segments.stream()
                    .filter(segment -> isLongEnough(segment.start(), segment.end(), duration))
                    .toList();

            int dayMaxParticipants = validSegments.stream()
                    .mapToInt(AvailabilitySegment::participantCount)
                    .max()
                    .orElse(0);

            globalMaxParticipants = Math.max(globalMaxParticipants, dayMaxParticipants);

            List<AvailabilitySegment> bestSegments = validSegments.stream()
                    .filter(segment -> segment.participantCount() == dayMaxParticipants)
                    .toList();

            resultIntervals.addAll(
                    intervalService.toMeetingIntervals(date, bestSegments, duration)
            );
        }

        return new AvailabilityIntervalsResponse(
                meetingId,
                resultIntervals,
                globalMaxParticipants,
                hasPending
        );
    }

    private boolean hasNotAcceptedRequiredParticipant(List<EventParticipantEntity> participants) {
        return participants.stream()
                .anyMatch(participant ->
                        participant.isRequired()
                                && participant.getStatus() != AcceptStatusParticipant.ACCEPTED
                );
    }

    private List<EventParticipantEntity> loadAcceptedParticipants(
            List<EventParticipantEntity> meetingParticipants,
            EventParticipantEntity author
    ) {
        List<EventParticipantEntity> participants = meetingParticipants.stream()
                .filter(participant -> participant.getStatus() == AcceptStatusParticipant.ACCEPTED)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        boolean authorIncluded = participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(author.getUser().getId()));

        if (!authorIncluded) {
            participants.add(author);
        }

        return participants;
    }

    private List<TimeInterval> resolveAllowedWindows(
            LocalDate day,
            List<ParticipantSchedule> requiredSchedules,
            ParticipantSchedule authorSchedule,
            int durationMinutes
    ) {
        if (!requiredSchedules.isEmpty()) {
            return requiredWindowService.findAllowedWindows(day, requiredSchedules, durationMinutes);
        }

        return authorSchedule.availability().getOrDefault(day, List.of()).stream()
                .filter(interval -> isLongEnough(interval.start(), interval.end(), durationMinutes))
                .toList();
    }

    private boolean isLongEnough(LocalTime start, LocalTime end, int durationMinutes) {
        return !end.minusMinutes(durationMinutes).isBefore(start);
    }
}