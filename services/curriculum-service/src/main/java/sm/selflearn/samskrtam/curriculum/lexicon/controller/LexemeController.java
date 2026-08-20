package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeAdminPage;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeDetailDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexemeAdminService;

import java.util.UUID;

/**
 * Admin CRUD лексем (task-curriculum-16 §1–§4). Запись — только ADMIN.
 */
@RestController
@RequestMapping("/api/v2/lexicon/lexemes")
@RequiredArgsConstructor
public class LexemeController {

    private final LexemeAdminService adminService;

    @GetMapping
    public LexemeAdminPage list(
            @RequestParam(required = false) String posCode,
            @RequestParam(required = false) UUID semanticClassId,
            @RequestParam(defaultValue = "false") boolean noSemanticClass,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return adminService.list(posCode, semanticClassId, noSemanticClass, page, size);
    }

    @GetMapping("/{id}")
    public LexemeDetailDto get(@PathVariable UUID id) {
        return adminService.get(id);
    }

    @PostMapping
    public LexemeDetailDto create(@RequestBody LexemeUpsertRequest request) {
        return adminService.create(request);
    }

    @PutMapping("/{id}")
    public LexemeDetailDto update(@PathVariable UUID id, @RequestBody LexemeUpsertRequest request) {
        return adminService.update(id, request);
    }
}