package alexspeal.models;

import alexspeal.enums.EventType;

import java.time.LocalTime;

public record TimeEvent(LocalTime time, EventType type) {
}