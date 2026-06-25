package com.faforever.client.map.generator;

import lombok.Value;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents the result of a map generation operation.
 * Used to track generated maps and their status.
 */
@Value
public class MapGenerationResult {

  /**
   * Name of the generated map.
   */
  String mapName;

  /**
   * The options that were used for generation.
   */
  GeneratorOptions generatorOptions;

  /**
   * Path to the map directory.
   */
  Path mapDirectory;

  /**
   * Optional error message if generation failed.
   */
  Optional<String> errorMessage;

  /**
   * Flag indicating if generation was successful.
   */
  boolean isSuccess;

}
