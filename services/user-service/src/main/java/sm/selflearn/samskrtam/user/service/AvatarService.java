package sm.selflearn.samskrtam.user.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.exception.InvalidFileTypeException;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;
import sm.selflearn.samskrtam.user.dto.AvatarConfirmResponse;
import sm.selflearn.samskrtam.user.dto.UploadUrlResponse;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AvatarService {

    private final MinioClient internalMinioClient;
    private final MinioClient presigningMinioClient;
    private final UserProfileRepository profileRepository;

    @Value("${minio.bucket.avatars}")
    private String avatarsBucket;

    @Value("${minio.external-url}")
    private String minioExternalUrl;

    public AvatarService(
            @Qualifier("internalMinioClient") MinioClient internalMinioClient,
            @Qualifier("presigningMinioClient") MinioClient presigningMinioClient,
            UserProfileRepository profileRepository) {
        this.internalMinioClient = internalMinioClient;
        this.presigningMinioClient = presigningMinioClient;
        this.profileRepository = profileRepository;
    }

    public UploadUrlResponse generateUploadUrl(UUID userId, String contentType) {
        log.trace("generateUploadUrl: userId={}", userId);
        validateImageContentType(contentType);

        String objectKey = userId + "/" + UUID.randomUUID();

        try {
            // Use the presigning client to generate a URL with the correct public host
            String uploadUrl = presigningMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(avatarsBucket)
                            .object(objectKey)
                            .expiry(5, TimeUnit.MINUTES)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .build()
            );

            log.debug("Generated external upload URL: {}", uploadUrl);
            return new UploadUrlResponse(uploadUrl, objectKey);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Failed to generate presigned URL for userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    @Transactional
    public AvatarConfirmResponse confirmUpload(UUID userId, String objectKey) {
        log.trace("confirmUpload: userId={}, objectKey={}", userId, objectKey);

        try {
            // Use the internal client to verify the object's existence
            internalMinioClient.statObject(StatObjectArgs.builder()
                    .bucket(avatarsBucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to stat object {} for userId={}: {}", objectKey, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to confirm avatar upload, object not found or accessible", e);
        }

        // The final public URL is a simple, direct link
        String avatarUrl = String.format("%s/%s/%s", minioExternalUrl, avatarsBucket, objectKey);

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        log.debug("Avatar confirmed: userId={}, url={}", userId, avatarUrl);
        return new AvatarConfirmResponse(avatarUrl);
    }

    private void validateImageContentType(String contentType) {
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new InvalidFileTypeException(contentType);
        }
    }
}
