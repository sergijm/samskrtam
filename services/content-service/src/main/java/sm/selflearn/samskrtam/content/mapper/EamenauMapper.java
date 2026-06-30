package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.content.dto.SandhiRuleDto;
import sm.selflearn.samskrtam.content.dto.SandhiRuleGroupDto;
import sm.selflearn.samskrtam.emenau.model.SandhiRule;
import sm.selflearn.samskrtam.emenau.model.SandhiRuleGroup;

@Mapper(componentModel = "spring")
public interface EamenauMapper {

    SandhiRuleDto toSandhiRuleDto(SandhiRule rule);

    SandhiRuleGroupDto toSandhiRuleGroupDto(SandhiRuleGroup group);
}