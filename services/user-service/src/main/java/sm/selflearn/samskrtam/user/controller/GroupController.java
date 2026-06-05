package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.user.dto.Group;
import sm.selflearn.samskrtam.user.service.GroupService;

import java.util.List;

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
}
