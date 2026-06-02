package sm.selflearn.samskrtam.user.dto;

import java.util.List;

public record AdminUserListResponse(
    List<UserProfileResponse> users,
    int totalPages,
    long totalElements,
    int currentPage,
    int pageSize,
    boolean isFirst,
    boolean isLast
) {}
