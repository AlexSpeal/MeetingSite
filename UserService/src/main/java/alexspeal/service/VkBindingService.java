package alexspeal.service;

import alexspeal.config.VkBotProperties;
import alexspeal.dto.requests.ConfirmVkBindingRequest;
import alexspeal.dto.requests.StartVkBindingRequest;
import alexspeal.entities.UserEntity;
import alexspeal.entities.VkBindingAttemptEntity;
import alexspeal.repositories.UserRepository;
import alexspeal.repositories.VkBindingAttemptRepository;
import alexspeal.utils.VkBindingCodeHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VkBindingService {

    private final VkNotificationService vkNotificationService;
    private final VkBindingAttemptRepository vkBindingAttemptRepository;
    private final UserRepository userRepository;
    private final VkBindingCodeHasher codeHasher;
    private final VkBotProperties vkBotProperties;
    private final VkBindingAttemptTxService vkBindingAttemptTxService;

    @Transactional
    public void startBinding(UserEntity user, StartVkBindingRequest request) {
        if (request.screenName() == null || request.screenName().isBlank()) {
            throw new IllegalArgumentException("Не указан логин VK");
        }

        Long userId = user.getId();
        Long vkUserId = vkNotificationService.resolveVkUserIdByScreenName(request.screenName().trim());
        if (user.getVkUserId() != null) {
            if (user.getVkUserId().equals(vkUserId)) {
                throw new IllegalArgumentException("Этот VK-аккаунт уже привязан к вашему профилю");
            }
            throw new IllegalArgumentException("У вас уже подключен другой VK-аккаунт. Сначала отключите текущую привязку");
        }
        vkBindingAttemptRepository
                .findFirstByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(userId, LocalDateTime.now())
                .ifPresent(attempt -> {
                    throw new IllegalArgumentException(
                            "Код подтверждения уже был отправлен и действует до " + attempt.getExpiresAt()
                    );
                });
        userRepository.findByVkUserId(vkUserId)
                .ifPresent(owner -> {
                    if (!owner.getId().equals(userId)) {
                        throw new IllegalArgumentException("Этот VK-аккаунт уже привязан к другому пользователю");
                    }
                });

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(vkBotProperties.codeTtlMinutes());
        String codeHash = codeHasher.hash(userId, vkUserId, code);

        VkBindingAttemptEntity attempt = new VkBindingAttemptEntity(userId, expiresAt, vkUserId, codeHash, 0);

        vkBindingAttemptRepository.save(attempt);

        vkNotificationService.sendMessage(
                vkUserId,
                "Код подтверждения для подключения уведомлений: " + code
        );
    }

    @Transactional
    public void confirmBinding(UserEntity user, ConfirmVkBindingRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Не указан код подтверждения");
        }

        Long userId = user.getId();

        VkBindingAttemptEntity attempt = vkBindingAttemptRepository
                .findFirstByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(userId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Активная попытка привязки не найдена или срок действия кода истек"
                ));

        if (attempt.getAttempts() >= vkBotProperties.maxAttempts()) {
            throw new IllegalArgumentException("Превышено количество попыток подтверждения");
        }

        String expectedHash = codeHasher.hash(userId, attempt.getVkUserId(), request.code().trim());

        if (!attempt.getCodeHash().equals(expectedHash)) {
            vkBindingAttemptTxService.incrementAttempts(userId, attempt.getExpiresAt());
            throw new IllegalArgumentException("Неверный код подтверждения");
        }

        userRepository.findByVkUserId(attempt.getVkUserId())
                .ifPresent(owner -> {
                    if (!owner.getId().equals(userId)) {
                        throw new IllegalArgumentException("Этот VK-аккаунт уже привязан к другому пользователю");
                    }
                });

        if (user.getVkUserId() != null) {
            if (user.getVkUserId().equals(attempt.getVkUserId())) {
                return;
            }
            throw new IllegalArgumentException("У вас уже привязан другой VK-аккаунт");
        }

        user.setVkUserId(attempt.getVkUserId());

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Этот VK-аккаунт уже был привязан другим пользователем");
        }

        vkBindingAttemptRepository.delete(attempt);
    }

    @Transactional
    public void disableBinding(UserEntity user) {
        if (user.getVkUserId() == null) {
            throw new IllegalArgumentException("VK-уведомления уже отключены");
        }

        user.setVkUserId(null);
        userRepository.save(user);
    }

    private String generateCode() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(value);
    }
}