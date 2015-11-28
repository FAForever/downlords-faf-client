package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameTypeMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jsoup.Jsoup;

public class GameTypeBean {

  private final StringProperty name;
  private final StringProperty fullName;
  private final StringProperty description;

  public GameTypeBean(GameTypeMessage gameTypeMessage) {
    name = new SimpleStringProperty(gameTypeMessage.getName());
    fullName = new SimpleStringProperty(gameTypeMessage.getFullname());
    description = new SimpleStringProperty(Jsoup.parse(gameTypeMessage.getDesc()).text());
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
