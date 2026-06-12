package sm.selflearn.samskrtam.user.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;

    @Value("${minio.bucket.avatars}")
    private String avatarsBucketName;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Check if the bucket already exists.
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(avatarsBucketName).build());
            if (!found) {
                // 2. If not, create it.
                log.info("Bucket '{}' not found. Creating it...", avatarsBucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(avatarsBucketName).build());
                log.info("Bucket '{}' created successfully.", avatarsBucketName);

                // 3. Set a public read-only policy on the new bucket.
                String policyJson = createPublicReadPolicy(avatarsBucketName);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(avatarsBucketName)
                        .config(policyJson)
                        .build());
                log.info("Public read policy set for bucket '{}'.", avatarsBucketName);

            } else {
                log.info("Bucket '{}' already exists. No action needed.", avatarsBucketName);
            }
        } catch (Exception e) {
            log.error("Error during MinIO bucket initialization: {}", e.getMessage(), e);
            // Throwing a runtime exception will prevent the application from starting
            // if it cannot configure its required infrastructure.
            throw new RuntimeException("Failed to initialize MinIO bucket: " + avatarsBucketName, e);
        }
    }

    /**
     * Creates a JSON policy string that makes all objects in a bucket publicly readable.
     */
    private String createPublicReadPolicy(String bucketName) {
        return "{\n" +
               "    \"Version\": \"2012-10-17\",\n" +
               "    \"Statement\": [\n" +
               "        {\n" +
               "            \"Effect\": \"Allow\",\n" +
               "            \"Principal\": {\"AWS\": [\"*\"]},\n" +
               "            \"Action\": [\"s3:GetObject\"],\n" +
               "            \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
               "        }\n" +
               "    ]\n" +
               "}";
    }
}
