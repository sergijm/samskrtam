package sm.selflearn.samskrtam.user.service;

import sm.selflearn.samskrtam.user.dto.OAuthSyncRequest;
import sm.selflearn.samskrtam.user.dto.UserDto;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Optional<UserDto> findById(UUID userId);

    void syncOAuth2User(OAuthSyncRequest request); // Changed return type to void
}
