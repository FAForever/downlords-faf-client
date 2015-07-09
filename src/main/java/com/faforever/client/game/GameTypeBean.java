package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameTypeInfo;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jsoup.Jsoup;

public class GameTypeBean {

  private StringProperty name;
  private StringProperty fullName;
  private StringProperty description;

  public GameTypeBean(GameTypeInfo gameTypeInfo) {
    name = new SimpleStringProperty(gameTypeInfo.name);
    fullName = new SimpleStringProperty(gameTypeInfo.fullname);
    description = new SimpleStringProperty(Jsoup.parse(gameTypeInfo.desc).text());
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
