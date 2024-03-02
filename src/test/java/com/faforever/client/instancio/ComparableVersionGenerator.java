package com.faforever.client.instancio;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.instancio.Random;
import org.instancio.generator.Generator;
import org.instancio.spi.InstancioServiceProvider;

public class ComparableVersionGenerator implements Generator<ComparableVersion>, InstancioServiceProvider {
  @Override
  public ComparableVersion generate(Random random) {
    return new ComparableVersion(random.digits(1));
  }

  @Override
  public InstancioServiceProvider.GeneratorProvider getGeneratorProvider() {
    return (node, generators) -> {
      if (ComparableVersion.class.isAssignableFrom(node.getTargetClass())) {
        return this;
      }
      return null;
    };
  }
}
