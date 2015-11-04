package com.faforever.client.stats.domain;

import java.util.HashMap;
import java.util.Map;

public enum Unit {
  // ACUs
  AEON_ACU("ual0001"),
  CYBRAN_ACU("url0001"),
  UEF_ACU("uel0001"),
  SERAPHIM_ACU("xsl0001"),

  // ASFs
  CORONA("uaa0303"),
  GEMINI("ura0303"),
  WASP("uea0303"),
  IAZYNE("xsa0303"),

  // Experimentals
  PARAGON("xab1401"),
  MAVOR("ueb2401"),
  YOLONA_OSS("xsb2401"),
  CZAR("uaa0310"),
  SOUL_RIPPER("ura0401"),
  AHWASSA("xsa0402"),
  SCATHIS("url0401"),
  GALACTIC_COLOSSUS("ual0401"),
  MONKEYLORD("url0402"),
  MEGALITH("xrl0403"),
  FATBOY("uel0401"),
  YTHOTHA("xsl0401"),
  TEMPEST("uas0401"),
  ATLANTIS("ues0401"),
  NOVAX_CENTER("xeb2402"),

  // Transporters
  CHARIOT("uaa0107"),
  ALUMINAR("uaa0104"),
  SKYHOOK("ura0107"),
  DRAGON_FLY("ura0104"),
  C6_COURIER("uea0107"),
  C14_STAR_LIFTER("uea0104"),
  CONTINENTAL("xea0306"),
  VISH("xsa0107"),
  VISHALA("xsa0104"),

  SALVATION("xab2307"),

  // SACUs
  AEON_SACU("ual0301"),
  CYBRAN_SACU("url0301"),
  UEF_SACU("uel0301"),
  SERAPHIM_SACU("xsl0301"),

  // Engineers
  AEON_T1_ENGINEER("ual0105"),
  AEON_T2_ENGINEER("ual0208"),
  AEON_T3_ENGINEER("ual0309"),
  CYBRAN_T1_ENGINEER("url0105"),
  CYBRAN_T2_ENGINEER("url0208"),
  CYBRAN_T3_ENGINEER("url0309"),
  UEF_T1_ENGINEER("uel0105"),
  UEF_T2_ENGINEER("uel0208"),
  UEF_T2_FIELD_ENGINEER("xel0209"),
  UEF_T3_ENGINEER("uel0309"),
  SERAPHIM_T1_ENGINEER("xsl0105"),
  SERAPHIM_T2_ENGINEER("xsl0208"),
  SERAPHIM_T3_ENGINEER("xsl0309"),

  // Other units
  MERCY("daa0206"),
  FIRE_BEETLE("xrl030"),

  UNKNOWN(null);

  private static final Map<String, Unit> fromId;

  static {
    fromId = new HashMap<>();
    for (Unit unit : values()) {
      fromId.put(unit.id, unit);
    }
  }

  private final String id;

  Unit(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static Unit fromId(String id) {
    Unit unit = fromId.get(id);
    if (unit == null) {
      return UNKNOWN;
    }
    return unit;
  }
}
