package com.faforever.client.map.generator;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents the result of a map generation operation. Used to track generated maps and their status.
 *
 * @param mapName Name of the generated map.
 * @param generatorOptions The options that were used for generation.
 * @param mapDirectory Path to the map directory.
 * @param errorMessage Optional error message if generation failed.
 * @param isSuccess Flag indicating if generation was successful.
 */
public record MapGenerationResult(
    String mapName,
    GeneratorOptions generatorOptions,
    Path mapDirectory,
    Optional<String> errorMessage,
    boolean isSuccess
) {
}
