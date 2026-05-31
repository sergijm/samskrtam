package sm.selflearn.samskrtam.gateway.config;

import io.micrometer.context.ContextRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Пробрасывает traceId/spanId из Reactor Context в MDC для WebFlux стека.
 *
 * <p>Без этого бина traceId в JSON логах api-gateway будет null,
 * так как WebFlux не использует ThreadLocal (в отличие от Virtual Threads).
 *
 * <p>См. conventions.md раздел "WebFlux — ReactorContextAccessor".
 */
@Configuration
public class TracingConfig {

    /**
     * Включает автоматическое распространение Reactor Context в Micrometer Observation.
     * Вызывается один раз при старте приложения.
     */
    @Bean
    public ContextRegistry contextRegistry() {
        // Reactor 3.5+ — instrumentation через Hooks.enableAutomaticContextPropagation()
        Hooks.enableAutomaticContextPropagation();
        return ContextRegistry.getInstance();
    }
}
