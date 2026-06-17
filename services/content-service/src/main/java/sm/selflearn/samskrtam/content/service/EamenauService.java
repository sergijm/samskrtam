package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.SandhiRuleDto;
import sm.selflearn.samskrtam.content.dto.SandhiRuleGroupDto;
import sm.selflearn.samskrtam.eamenau.model.SandhiRule;
import sm.selflearn.samskrtam.eamenau.model.SandhiRuleGroup;
import sm.selflearn.samskrtam.eamenau.repository.SandhiRuleRepository;

import java.util.List;
import java.util.Set;
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
        Set<SandhiRuleGroupDto> sandhiRuleGroups = rule.getSandhiRuleGroups().stream()
                .map(this::mapToSandhiRuleGroupDto)
                .collect(Collectors.toSet());

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
                .sandhiRuleGroups(sandhiRuleGroups)
                .build();
    }

    private SandhiRuleGroupDto mapToSandhiRuleGroupDto(SandhiRuleGroup group) {
        return SandhiRuleGroupDto.builder()
                .id(group.getId())
                .description(group.getDescription())
                .code(group.getCode())
                .build();
    }
}
