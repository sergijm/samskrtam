package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.sangraha.model.AnalysisConfidence;
import sm.selflearn.samskrtam.sangraha.model.Gender;

/**
 * Кандидат на импорт существительного в curriculum-лексикон (для бутстрапа
 * квизов/парадигм склонений). Читается из {@code nominal_lemmas} объединением
 * с {@code verse_words} по тексту {@code lemma_iast} для подсчёта частотности.
 * Род берётся из {@code verse_word_morphology} как самый частый по лемме.
 *
 * @param lemmaIast        словарная форма IAST (ключ, UNIQUE в nominal_lemmas)
 * @param lemmaDevanagari  lemma в силлабарии деванагари
 * @param lemmaSlp1        lemma в SLP1-транслитерации
 * @param stemIast         основа склонения IAST (источник для WordFormBuilder)
 * @param stemClass        регулярный класс основы (A_STEM … R_STEM)
 * @param confidence       уверенность LLM-классификации
 * @param gender           самый частый род вхождения леммы (может быть null)
 * @param frequency        число вхождений леммы в verse_words (порядок сортировки)
 */
public record NominalLemmaCandidateDto(
        String lemmaIast,
        String lemmaDevanagari,
        String lemmaSlp1,
        String stemIast,
        String stemClass,
        AnalysisConfidence confidence,
        Gender gender,
        long frequency
) {
}