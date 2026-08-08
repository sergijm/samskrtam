package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.SourceUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Source;
import sm.selflearn.samskrtam.curriculum.lexicon.service.SourceAdminService;

import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD источников корпуса + refresh-cache (task-curriculum-16 §7).
 */
@RestController
@RequestMapping("/api/v2/lexicon/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceAdminService adminService;

    @GetMapping
    public List<Source> list() {
        return adminService.list();
    }

    @GetMapping("/{id}")
    public Source get(@PathVariable UUID id) {
        return adminService.get(id);
    }

    @PostMapping
    public Source create(@RequestBody SourceUpsertRequest request) {
        return adminService.create(request);
    }

    @PutMapping("/{id}")
    public Source update(@PathVariable UUID id, @RequestBody SourceUpsertRequest request) {
        return adminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        adminService.delete(id);
    }

    @PostMapping("/{id}/occurrences/refresh-cache")
    public Source refreshCache(@PathVariable UUID id) {
        return adminService.refreshCache(id);
    }
}