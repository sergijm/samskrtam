package sm.selflearn.samskrtam.user.dto;

import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    String bio
) {}
