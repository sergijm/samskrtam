package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Создание стиха (Verse) внутри главы: сохраняется сырой текст (rawText) и
 * статус DRAFT. Дальнейший анализ выполняется существующей кнопкой «Анализ»
 * на странице стиха (POST /verses/{verseId}/analyze).
 */
public record CreateVerseRequest(String text) {}
