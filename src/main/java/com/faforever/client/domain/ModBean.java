package com.faforever.client.domain;

public record ModBean(
    Integer id,
    String displayName,
    boolean recommended,
    String author,
    PlayerBean uploader,
    ModReviewsSummaryBean modReviewsSummary
) {}
