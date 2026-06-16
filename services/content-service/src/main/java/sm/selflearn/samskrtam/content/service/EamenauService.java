package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.SandhiRuleDto;
import sm.selflearn.samskrtam.eamenau.model.SandhiRule;
import sm.selflearn.samskrtam.eamenau.repository.SandhiRuleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EamenauService {

    private final SandhiRuleRepository sandhiRuleRepository;

    public List<SandhiRuleDto> getAllSandhiRules() {
        return sandhiRuleRepository.findAllByOrderByRuleNumberAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private SandhiRuleDto mapToDto(SandhiRule rule) {
        return SandhiRuleDto.builder()
                .id(rule.getId())
                .ruleNumber(rule.getRuleNumber())
                .ruleType(rule.getRuleType())
                .shortDescription(rule.getShortDescription())
                .whitneyNumber(rule.getWhitneyNumber())
                .iastExample(rule.getIastExample())
                .hkExample(rule.getHkExample())
                .notes(rule.getNotes())
                .fullText(rule.getFullText())
                .build();
    }
}
