package sm.selflearn.samskrtam.curriculum.bootstrap;

/**
 * Кандидат существительного для импорта склонений, полученный от sangraha-service
 * (эндпоинт /sangraha/internal/content/nominal-lemmas). Поля соответствуют
 * DTO sangraha NominalLemmaCandidateDto; gender закодирован строкой (имя enum),
 * т.к. sangraha использует свой Gender.
 *
 * @param lemmaIast       словарная форма IAST
 * @param lemmaDevanagari lemma в деванагари
 * @param lemmaSlp1       lemma в SLP1
 * @param stemIast        основа склонения IAST
 * @param stemClass       класс основы (A_STEM … R_STEM)
 * @param gender          самый частый род вхождений (имя enum или null)
 * @param frequency       частотность вverse_words
 */
public record NominalLemmaCandidateDto(
        String lemmaIast,
        String lemmaDevanagari,
        String lemmaSlp1,
        String stemIast,
        String stemClass,
        String gender,
        long frequency
) {
}