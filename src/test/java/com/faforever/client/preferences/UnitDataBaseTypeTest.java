package com.faforever.client.preferences;

import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitDataBaseTypeTest {

  @Test
  public void testUnknownNameFallsBackToEtfreeman() throws Exception {
    ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    assertEquals(UnitDataBaseType.ETFREEMAN, mapper.readValue("\"RACKOVER\"", UnitDataBaseType.class));
    assertEquals(UnitDataBaseType.SPOOKY, mapper.readValue("\"SPOOKY\"", UnitDataBaseType.class));
  }

  @Test
  public void testNullFallsBackToEtfreeman() {
    Preferences preferences = new Preferences();
    preferences.setUnitDataBaseType(null);
    assertEquals(UnitDataBaseType.ETFREEMAN, preferences.getUnitDataBaseType());
  }
}
