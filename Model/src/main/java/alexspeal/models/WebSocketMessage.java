package alexspeal.models;

import alexspeal.enums.WebSocketAction;

public record WebSocketMessage(WebSocketAction action, Long meetingId, Object data) {
}
