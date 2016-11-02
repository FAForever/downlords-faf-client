package com.faforever.client.game;

import com.faforever.client.remote.domain.FeaturedModMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jsoup.Jsoup;

public class FeaturedModBean {

  private final StringProperty name;
  private final StringProperty fullName;
  private final StringProperty description;

  public FeaturedModBean(FeaturedModMessage featuredModMessage) {
    name = new SimpleStringProperty(featuredModMessage.getName());
    fullName = new SimpleStringProperty(featuredModMessage.getFullname());
    description = new SimpleStringProperty(Jsoup.parse(featuredModMessage.getDesc()).text());
  }

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getFullName() {
    return fullName.get();
  }

  public StringProperty fullNameProperty() {
    return fullName;
  }

  public String getDescription() {
    return description.get();
  }

  public StringProperty descriptionProperty() {
    return description;
  }
}
