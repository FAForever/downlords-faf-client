package com.faforever.client.preferences;

import com.faforever.client.game.KnownFeaturedMod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class LastGamePrefs {

  StringProperty lastGameType = new SimpleStringProperty(KnownFeaturedMod.DEFAULT.getTechnicalName());
  StringProperty lastGameTitle = new SimpleStringProperty();
  StringProperty lastMap = new SimpleStringProperty();
  StringProperty lastGamePassword = new SimpleStringProperty();
  ObjectProperty<Integer> lastGameMinRating = new SimpleObjectProperty<>(null);
  ObjectProperty<Integer> lastGameMaxRating = new SimpleObjectProperty<>(null);
  BooleanProperty lastGameOnlyFriends = new SimpleBooleanProperty();
  BooleanProperty lastGameEnforceRating = new SimpleBooleanProperty(false);
  BooleanProperty offlineMode = new SimpleBooleanProperty(false);

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

  public Integer getLastGameMinRating() {
    return lastGameMinRating.get();
  }

  public void setLastGameMinRating(Integer lastGameMinRating) {
    this.lastGameMinRating.set(lastGameMinRating);
  }

  public ObjectProperty<Integer> lastGameMinRatingProperty() {
    return lastGameMinRating;
  }

  public Integer getLastGameMaxRating() {
    return lastGameMaxRating.get();
  }

  public void setLastGameMaxRating(Integer lastGameMaxRating) {
    this.lastGameMaxRating.set(lastGameMaxRating);
  }

  public ObjectProperty<Integer> lastGameMaxRatingProperty() {
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

  public boolean isLastGameEnforceRating() {
    return lastGameEnforceRating.get();
  }

  public void setLastGameEnforceRating(boolean lastGameEnforceRating) {
    this.lastGameEnforceRating.set(lastGameEnforceRating);
  }

  public BooleanProperty lastGameEnforceRatingProperty() {
    return lastGameEnforceRating;
  }

  public boolean isOfflineMode() {
    return offlineMode.get();
  }

  public void setOfflineMode(boolean offlineMode) {
    this.offlineMode.set(offlineMode);
  }

  public BooleanProperty offlineModeProperty() {
    return offlineMode;
  }
}
