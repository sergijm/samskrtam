package sm.selflearn.samskrtam.mw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import sm.selflearn.samskrtam.mw.MwHtmlRenderer;
import sm.selflearn.samskrtam.mw.dto.MwEntryDto;
import sm.selflearn.samskrtam.mw.repository.MwRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MwService {

    private final MwRepository mwRepository;
    private final TransliterationService transliterationService;

    /**
     * Поиск словарной статьи MW по лемме. Вход — IAST (или devanagari);
     * транслитерируется в SLP1 и матчится с колонкой key1.
     */
    public List<MwEntryDto> getEntriesByLemma(String lemma) {
        String slp1 = transliterationService.slp1RemoveStress(
                transliterationService.normalizeToSlp1(lemma, null));
        return groupHtml(mwRepository.findByK1Slp1(slp1));
    }

    public List<MwEntryDto> getEntriesByIds(List<Long> ids) {
        return groupHtml(mwRepository.findByIds(ids));
    }

    /**
     * Группирует записи по головному слову (key1) и прикрепляет готовый HTML
     * одной словарной статьи (один <h1> на группу) к первой записи группы,
     * а остальным записям группы HTML не проставляет (он уже встроен в общий блок).
     */
    private List<MwEntryDto> groupHtml(List<MwEntryDto> entries) {
        if (entries == null || entries.isEmpty()) {
            return entries;
        }
        Map<String, List<MwEntryDto>> groups = new LinkedHashMap<>();
        for (MwEntryDto e : entries) {
            String head = e.getKey1() != null ? e.getKey1() : "";
            groups.computeIfAbsent(head, k -> new ArrayList<>()).add(e);
        }
            for (List<MwEntryDto> group : groups.values()) {
                MwHtmlRenderer.RenderedArticle article = MwHtmlRenderer.renderEntries(group);
                for (int i = 0; i < group.size(); i++) {
                    MwEntryDto dto = group.get(i);
                    if (i == 0) {
                        dto.setHeadwordIast(article.headwordIast);
                        dto.setPageRefsHtml(article.pageRefsHtml);
                        dto.setHtml(article.bodyHtml);
                    } else {
                        dto.setHeadwordIast(null);
                        dto.setPageRefsHtml(null);
                        dto.setHtml(null);
                    }
                }
            }
        return entries;
    }
}
