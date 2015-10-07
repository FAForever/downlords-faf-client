package com.faforever.client.stats.domain;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class UnitTypeAdapter extends XmlAdapter<String, UnitType> {

  @Override
  public UnitType unmarshal(String v) throws Exception {
    return UnitType.fromString(v);
  }

  @Override
  public String marshal(UnitType v) throws Exception {
    return v.getString();
  }
}
