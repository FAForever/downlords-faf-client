package com.faforever.client.instancio;

import com.faforever.client.map.MapSize;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class InstancioServiceProvider implements org.instancio.spi.InstancioServiceProvider {
  @Override
  public GeneratorProvider getGeneratorProvider() {
    return (node, generators) -> {
      Class<?> targetClass = node.getTargetClass();
      return switch (targetClass) {
        case Class<?> clazz when ComparableVersion.class.isAssignableFrom(clazz) -> new ComparableVersionGenerator();
        case Class<?> clazz when MapSize.class.isAssignableFrom(clazz) -> new MapSizeGenerator();
        default -> null;
      };
    };
  }
}
