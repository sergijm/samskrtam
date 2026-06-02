package sm.selflearn.samskrtam.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.content.service.QuizService;

@RestController
@RequestMapping("/api/v1/content/quizzes/manage") // Changed request mapping to avoid conflict
@RequiredArgsConstructor
public class QuizManagementController { // Renamed class

    private final QuizService quizService;

    // Removed the conflicting @GetMapping method.
    // If this controller is meant to manage quizzes, other methods (e.g., POST, PUT, DELETE for /quizzes/{id}) would go here.
    // For now, it's empty to resolve the conflict.
}
