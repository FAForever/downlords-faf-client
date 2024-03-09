package com.faforever.client.instancio;

import com.faforever.client.map.MapSize;
import org.instancio.Random;
import org.instancio.generator.Generator;

public class MapSizeGenerator implements Generator<MapSize> {
  @Override
  public MapSize generate(Random random) {
    return new MapSize(random.intRange(0, 8192), random.intRange(0, 8192));
  }
}
