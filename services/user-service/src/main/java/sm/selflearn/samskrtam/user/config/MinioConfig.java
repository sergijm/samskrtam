package sm.selflearn.samskrtam.user.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.url}")
    private String minioInternalUrl;

    @Value("${minio.external-url}")
    private String minioExternalUrl;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Bean
    @Primary
    @Qualifier("internalMinioClient")
    public MinioClient internalMinioClient() {
        log.info("Initializing internal MinioClient with URL: {}", minioInternalUrl);
        return MinioClient.builder()
                .endpoint(minioInternalUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    @Bean
    @Qualifier("presigningMinioClient")
    public MinioClient presigningMinioClient() {
        log.info("Initializing presigning MinioClient with external URL: {}", minioExternalUrl);
        return MinioClient.builder()
                .endpoint(minioExternalUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .region("us-east-1") // This is crucial to prevent region lookup calls to the Ingress
                .build();
    }
}
