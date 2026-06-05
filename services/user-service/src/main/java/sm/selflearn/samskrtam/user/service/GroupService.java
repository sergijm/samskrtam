package sm.selflearn.samskrtam.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.dto.CreateGroupRequest;
import sm.selflearn.samskrtam.user.dto.Group;
import sm.selflearn.samskrtam.user.exception.GroupNotFoundException;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.repository.GroupRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserProfileRepository userProfileRepository;

    public List<Group> getAllGroups() {
        log.debug("Fetching all groups");
        return groupRepository.findAll().stream()
                .map(this::mapToGroupDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Group createGroup(CreateGroupRequest request, UUID curatorId) {
        log.trace("Creating group: name={}, curatorId={}", request.name(), curatorId);

        UserProfile curator = userProfileRepository.findById(curatorId)
                .orElseThrow(() -> new UserNotFoundException(curatorId));

        sm.selflearn.samskrtam.user.model.Group newGroup = sm.selflearn.samskrtam.user.model.Group.builder()
                .name(request.name())
                .curator(curator)
                .members(new HashSet<>()) // Initialize with an empty set
                .createdAt(Instant.now())
                .build();

        // Add curator as a member
        newGroup.getMembers().add(curator);

        sm.selflearn.samskrtam.user.model.Group savedGroup = groupRepository.save(newGroup);
        log.debug("Group created: id={}, name={}", savedGroup.getId(), savedGroup.getName());
        return mapToGroupDto(savedGroup);
    }

    private Group mapToGroupDto(sm.selflearn.samskrtam.user.model.Group group) {
        return Group.builder()
                .id(group.getId())
                .name(group.getName())
                .curatorId(group.getCurator() != null ? group.getCurator().getId() : null)
                .curatorName(group.getCurator() != null ? group.getCurator().getUsername() : null)
                .memberCount(group.getMembers() != null ? group.getMembers().size() : 0)
                .createdAt(group.getCreatedAt())
                .build();
    }

    // TODO: Add other group management methods (get by id, rename, add/remove member, set curator)
}
