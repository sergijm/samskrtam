# api-gateway/src/main/resources/route-config.yaml
spring:
  cloud:
    gateway:
      routes:

        # Auth
        - id: login
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/login
          filters:
            - StripPrefix=2

        - id: logout
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/logout
          filters:
            - StripPrefix=2

        # Auth Callback
        - id: oauth2-google
          uri: http://localhost:8090/api/v1/auth/oauth2/callback
          predicates:
            - Path=/api/v1/auth/oauth2/google
          filters:
            - RewritePath=/api/.*, /$1

        - id: oauth2-mailru
          uri: http://localhost:8090/api/v1/auth/oauth2/callback
          predicates:
            - Path=/api/v1/auth/oauth2/mailru
          filters:
            - RewritePath=/api/.*, /$1

        # Auth Callback Proxy (Keycloak response)
        - id: oauth2-callback-proxy
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/oauth2/callback
          filters:
            - StripPrefix=2

        # Auth Refresh
        - id: refresh-token
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/refresh
          filters:
            - StripPrefix=2

        # Lessons (grammar)
        - id: lessons-grammar-slug-questions-history
          uri: lb://quiz-service:8082
          predicates:
            - Path=/api/v1/lessons/grammar/{slug}/questions/history
          filters:
            - RewritePath=/api/.*, /$1

        # Content (public, STUDENT) — включая НОВЫЙ /content/public/lessons/{slug}/declension-paradigms
        # ПРИМЕЧАНИЕ Агента 6: маршрут curriculum-service отсутствовал в этом файле вообще, хотя
        # уже фигурирует как эталонный в docs/agents/prompts/agent-1-gateway.md (таблица маршрутов,
        # /api/v1/content/public/** → STUDENT, /api/v1/content/** → ADMIN) — расхождение зафиксировано,
        # блок ниже добавлен, чтобы контракт соответствовал факту. Агенту 1: проверить, что в коде
        # (GatewayRoutesConfig.java) роль по этим двум предикатам действительно разведена так же.
        - id: content-public
          uri: lb://curriculum-service:8081
          predicates:
            - Path=/api/v1/content/public/**
          filters:
            - RewritePath=/api/.*, /$1
          # Auth: STUDENT (роль проверяется SecurityConfig по префиксу, см. agent-1-gateway.md)

        - id: content-admin
          uri: lb://curriculum-service:8081
          predicates:
            - Path=/api/v1/content/**
          filters:
            - RewritePath=/api/.*, /$1
          # Auth: ADMIN

        # Sangraha (произведения, главы, стихи, LLM-анализ) — см. docs/services/sangraha-service.md
        # sangraha-service ожидает полный путь /api/v1/sangraha/**, поэтому без StripPrefix
        - id: sangraha
          uri: lb://sangraha-service:8089
          predicates:
            - Path=/api/v1/sangraha/**
          filters:
            - RewritePath=/api/.*, /$1

