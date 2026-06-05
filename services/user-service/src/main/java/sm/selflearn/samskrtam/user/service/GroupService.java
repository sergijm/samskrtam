package sm.selflearn.samskrtam.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.user.dto.Group;
import sm.selflearn.samskrtam.user.repository.GroupRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;

    public List<Group> getAllGroups() {
        log.debug("Fetching all groups");
        return groupRepository.findAll().stream()
                .map(this::mapToGroupDto)
                .collect(Collectors.toList());
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

    // TODO: Add other group management methods (create, get by id, rename, add/remove member, set curator)
}
