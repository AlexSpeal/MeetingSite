package alexspeal.service;

import alexspeal.repositories.VkBindingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VkBindingAttemptTxService {

    private final VkBindingAttemptRepository vkBindingAttemptRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementAttempts(Long userId, LocalDateTime expiresAt) {
        int updated = vkBindingAttemptRepository.incrementAttempts(userId, expiresAt);
        if (updated == 0) {
            throw new IllegalStateException("Не удалось обновить количество попыток подтверждения");
        }
    }
}