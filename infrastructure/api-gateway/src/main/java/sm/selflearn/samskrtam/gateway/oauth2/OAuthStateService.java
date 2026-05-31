package sm.selflearn.samskrtam.gateway.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Управляет OAuth2 state параметром для защиты от CSRF.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #generate(String)} — генерирует state, сохраняет в Redis на 10 мин
 *   <li>{@link #validateAndConsume(String)} — проверяет state и удаляет (одноразовый)
 * </ol>
 *
 * <p>Redis key: {@code oauth2:state:{state}} → provider alias
 * TTL: 10 минут (достаточно для прохождения OAuth2 flow)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private static final String KEY_PREFIX = "oauth2:state:";
    private static final Duration TTL      = Duration.ofMinutes(10);
    private static final int      BYTES    = 32;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Генерирует cryptographically secure state и сохраняет в Redis.
     *
     * @param provider провайдер (google / mailru) — сохраняется как значение
     * @return сгенерированный state (base64url без padding)
     */
    public Mono<String> generate(String provider) {
        byte[] bytes = new byte[BYTES];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        return redisTemplate.opsForValue()
                .set(KEY_PREFIX + state, provider, TTL)
                .thenReturn(state)
                .doOnSuccess(s -> log.debug("OAuth2 state generated: provider={}", provider));
    }

    /**
     * Проверяет state и возвращает provider. Удаляет state (одноразовый).
     *
     * @param state значение из callback query param
     * @return Mono с provider alias, или Mono.empty() если state невалиден/истёк
     */
    public Mono<String> validateAndConsume(String state) {
        if (state == null || state.isBlank()) {
            log.warn("OAuth2 callback: state is null or blank");
            return Mono.empty();
        }

        String key = KEY_PREFIX + state;
        return redisTemplate.opsForValue()
                .getAndDelete(key)
                .doOnNext(provider ->
                        log.debug("OAuth2 state consumed: provider={}", provider))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("OAuth2 callback: state not found or expired: {}", state);
                    return Mono.empty();
                }));
    }
}
