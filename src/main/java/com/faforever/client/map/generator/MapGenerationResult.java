package com.faforever.client.map.generator;

import lombok.Value;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents the result of a map generation operation.
 * Used to track multiple generated maps and select the best one.
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
   * Flag indicating if this map was chosen.
   */
  boolean isChosen;

  /**
   * Optional error message if generation failed.
   */
  Optional<String> errorMessage;

  /**
   * Flag indicating if generation was successful.
   */
  boolean isSuccess;

}
