package com.faforever.client.domain.api;


import java.time.OffsetDateTime;

public record NameRecord(Integer id, String name, OffsetDateTime changeTime) {}
