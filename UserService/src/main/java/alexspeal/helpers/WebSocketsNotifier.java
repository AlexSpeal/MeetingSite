package alexspeal.helpers;

import alexspeal.dto.EventDto;
import alexspeal.enums.WebSocketAction;
import alexspeal.models.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketsNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    public void notify(EventDto event, WebSocketAction action, Object payload) {
        List<Long> participantIds = event.participants().stream()
                .map(p -> p.user().id())
                .toList();
        participantIds.forEach(pid -> messagingTemplate.convertAndSend(
                "/user/" + pid + "/queue/updates",
                new WebSocketMessage(action, event.id(), payload)
        ));
    }
}
