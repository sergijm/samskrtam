package sm.selflearn.samskrtam.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.content.dto.SandhiRuleDto;
import sm.selflearn.samskrtam.content.service.EamenauService;

import java.util.List;

@RestController
@RequestMapping("/eamenau")
@Tag(name = "Eamenau", description = "APIs for Eamenau content (e.g., Sandhi Rules)")
@RequiredArgsConstructor
public class EamenauController {

    private final EamenauService eamenauService;

    @GetMapping("/sandhi-rules")
    @Operation(summary = "Get all Sandhi Rules")
    @ApiResponse(responseCode = "200", description = "List of Sandhi Rules retrieved successfully")
    public List<SandhiRuleDto> getAllSandhiRules() {
        return eamenauService.getAllSandhiRules();
    }
}
