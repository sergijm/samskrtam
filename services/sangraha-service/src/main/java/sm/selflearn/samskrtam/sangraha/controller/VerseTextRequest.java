package sm.selflearn.samskrtam.sangraha.controller;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для PUT /verses/{id}/text.
 * Единое поле text — backend определяет письменность по Unicode-диапазону
 * (наличие символов деванагари → textDevanagari, иначе → textIast).
 */
public record VerseTextRequest(
    @NotBlank
    String text
) {}