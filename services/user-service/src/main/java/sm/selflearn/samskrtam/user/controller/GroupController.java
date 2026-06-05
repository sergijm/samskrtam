package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.user.dto.CreateGroupRequest;
import sm.selflearn.samskrtam.user.dto.Group;
import sm.selflearn.samskrtam.user.dto.GroupDetail;
import sm.selflearn.samskrtam.user.service.GroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Group Management", description = "APIs for managing user groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    @Operation(summary = "Get a list of all groups")
    @ApiResponse(responseCode = "200", description = "List of groups retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<Group>> getGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group details by ID")
    @ApiResponse(responseCode = "200", description = "Group details retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<GroupDetail> getGroupById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroupDetail(groupId));
    }

    @PostMapping
    @Operation(summary = "Create a new group")
    @ApiResponse(responseCode = "201", description = "Group created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid group data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    public ResponseEntity<Group> createGroup(
            @RequestHeader("X-User-Id") UUID userId, // Curator ID
            @Valid @RequestBody CreateGroupRequest request) {
        Group newGroup = groupService.createGroup(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
    }
}
