package com.faforever.client.preferences;

import com.faforever.client.game.KnownFeaturedMod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LastGamePrefs {
  private final StringProperty lastGameType;
  private final StringProperty lastGameTitle;
  private final StringProperty lastMap;
  private final StringProperty lastGamePassword;
  private final IntegerProperty lastGameMinRating;
  private final IntegerProperty lastGameMaxRating;
  private final BooleanProperty lastGameOnlyFriends;

  public LastGamePrefs() {
    lastGameType = new SimpleStringProperty(KnownFeaturedMod.DEFAULT.getTechnicalName());
    lastGameTitle = new SimpleStringProperty();
    lastMap = new SimpleStringProperty();
    lastGamePassword = new SimpleStringProperty();
    lastGameMinRating = new SimpleIntegerProperty(800);
    lastGameMaxRating = new SimpleIntegerProperty(1300);
    lastGameOnlyFriends = new SimpleBooleanProperty();

  }

  public String getLastGameType() {
    return lastGameType.get();
  }

  public void setLastGameType(String lastGameType) {
    this.lastGameType.set(lastGameType);
  }

  public StringProperty lastGameTypeProperty() {
    return lastGameType;
  }

  public String getLastGameTitle() {
    return lastGameTitle.get();
  }

  public void setLastGameTitle(String lastGameTitle) {
    this.lastGameTitle.set(lastGameTitle);
  }

  public StringProperty lastGameTitleProperty() {
    return lastGameTitle;
  }

  public String getLastMap() {
    return lastMap.get();
  }

  public void setLastMap(String lastMap) {
    this.lastMap.set(lastMap);
  }

  public StringProperty lastMapProperty() {
    return lastMap;
  }

  public String getLastGamePassword() {
    return lastGamePassword.get();
  }

  public void setLastGamePassword(String lastGamePassword) {
    this.lastGamePassword.set(lastGamePassword);
  }

  public StringProperty lastGamePasswordProperty() {
    return lastGamePassword;
  }

  public int getLastGameMinRating() {
    return lastGameMinRating.get();
  }

  public void setLastGameMinRating(int lastGameMinRating) {
    this.lastGameMinRating.set(lastGameMinRating);
  }

  public IntegerProperty lastGameMinRatingProperty() {
    return lastGameMinRating;
  }

  public int getLastGameMaxRating() {
    return lastGameMaxRating.get();
  }

  public void setLastGameMaxRating(int lastGameMaxRating) {
    this.lastGameMaxRating.set(lastGameMaxRating);
  }

  public IntegerProperty lastGameMaxRatingProperty() {
    return lastGameMaxRating;
  }

  public boolean isLastGameOnlyFriends() {
    return lastGameOnlyFriends.get();
  }

  public void setLastGameOnlyFriends(boolean lastGameOnlyFriends) {
    this.lastGameOnlyFriends.set(lastGameOnlyFriends);
  }

  public BooleanProperty lastGameOnlyFriendsProperty() {
    return lastGameOnlyFriends;
  }
}
