package sm.selflearn.samskrtam.content.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.ArrayList;
import java.util.List;

/**
 * Парсит фильтры vowelType/gender из query-параметров генератора квиза.
 * Основной формат — JSON-массив ({"A_STEM","AA_STEM"}), fallback — CSV.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnumFilterParser {

    private final ObjectMapper objectMapper;

    public List<VowelType> parseVowelTypes(String filterVowelTypes) {
        return parseEnumList(filterVowelTypes, VowelType.class);
    }

    public List<Gender> parseGenders(String filterGenders) {
        return parseEnumList(filterGenders, Gender.class);
    }

    private <E extends Enum<E>> List<E> parseEnumList(String raw, Class<E> enumClass) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> tokens = parseStringList(raw);
        List<E> result = new ArrayList<>();
        for (String token : tokens) {
            try {
                result.add(Enum.valueOf(enumClass, token.trim()));
            } catch (IllegalArgumentException e) {
                // ignore unknown values
            }
        }
        return result;
    }

    /**
     * Основной формат — JSON-массив типа {@code ["A_STEM","AA_STEM"]}.
     * Fallback — значения через запятую типа {@code "A_STEM,AA_STEM"}.
     */
    private List<String> parseStringList(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.debug("Failed to parse as JSON array, falling back to CSV: {}", trimmed, e);
            }
        }
        return List.of(trimmed.split(","));
    }
}
