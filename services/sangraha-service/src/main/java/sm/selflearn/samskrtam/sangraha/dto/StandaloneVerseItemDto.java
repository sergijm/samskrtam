package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.sangraha.model.VerseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Элемент списка standalone-стихов пользователя (страница /analysis).
 * Стихи не привязаны к произведению/главе (chapter_id = null), поэтому
 * контекст work/chapter отсутствует — только превью текста и статус.
 */
public record StandaloneVerseItemDto(
    UUID id,
    String preview,
    VerseStatus status,
    Instant createdAt
) {}
