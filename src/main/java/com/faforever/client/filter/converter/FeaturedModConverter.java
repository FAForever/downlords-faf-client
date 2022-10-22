package com.faforever.client.filter.converter;

import com.faforever.client.domain.FeaturedModBean;
import javafx.util.StringConverter;

public class FeaturedModConverter extends StringConverter<FeaturedModBean> {

  @Override
  public String toString(FeaturedModBean object) {
    return object.getDisplayName();
  }

  @Override
  public FeaturedModBean fromString(String string) {
    throw new UnsupportedOperationException("Not supported");
  }
}
