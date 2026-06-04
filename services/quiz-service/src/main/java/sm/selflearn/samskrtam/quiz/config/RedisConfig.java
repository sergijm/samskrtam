package sm.selflearn.samskrtam.quiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import sm.selflearn.samskrtam.quiz.model.SessionCache;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, SessionCache> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<SessionCache> valueSerializer = new Jackson2JsonRedisSerializer<>(SessionCache.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, SessionCache> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, SessionCache> context = builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
