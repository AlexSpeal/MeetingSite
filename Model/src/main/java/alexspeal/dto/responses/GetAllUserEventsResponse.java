package alexspeal.dto.responses;

import alexspeal.dto.EventDto;

import java.util.List;

public record GetAllUserEventsResponse(List<EventDto> eventDtoList) {
}
