package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes(
    @JsonSubTypes.Type(value = NeroxisGeneratorParams.class, name = "neroxis")
)
public interface MapParams {
}
