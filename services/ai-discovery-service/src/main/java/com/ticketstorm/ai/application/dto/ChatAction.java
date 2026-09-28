package com.ticketstorm.ai.application.dto;

public record ChatAction(
        String type,
        String eventId,
        String sectionId,
        Integer quantity
) {
    public static final String VIEW_EVENT = "VIEW_EVENT";
    public static final String VIEW_SECTIONS = "VIEW_SECTIONS";
    public static final String BUY = "BUY";
}
