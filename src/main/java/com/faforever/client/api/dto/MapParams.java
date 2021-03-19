package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes(
    @JsonSubTypes.Type(value = NeroxisGeneratorParams.class, name = "neroxis")
)
@Getter
@Setter
@NoArgsConstructor
public abstract class MapParams {
}
