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

        # Sangraha (произведения, главы, стихи, LLM-анализ) — см. docs/services/sangraha-service.md
        # sangraha-service ожидает полный путь /api/v1/sangraha/**, поэтому без StripPrefix
        - id: sangraha
          uri: lb://sangraha-service:8089
          predicates:
            - Path=/api/v1/sangraha/**
          filters:
            - RewritePath=/api/.*, /$1

