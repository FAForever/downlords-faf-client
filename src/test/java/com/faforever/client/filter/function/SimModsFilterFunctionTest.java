package com.faforever.client.filter.function;

import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.faforever.client.builders.GameInfoBuilder.create;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimModsFilterFunctionTest extends ServiceTest {

  private SimModsFilterFunction instance;

  @BeforeEach
  public void setUp() {
    instance = new SimModsFilterFunction();
  }

  @Test
  public void testFilter() {
    assertTrue(instance.apply(false, create().defaultValues().simMods(Map.of()).get()));
    assertTrue(instance.apply(false, create().defaultValues().simMods(Map.of("1", "2")).get()));
    assertTrue(instance.apply(true, create().defaultValues().simMods(Map.of()).get()));
    assertFalse(instance.apply(true, create().defaultValues().simMods(Map.of("1", "2")).get()));
  }
}