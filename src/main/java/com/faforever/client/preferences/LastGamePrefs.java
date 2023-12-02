package com.faforever.client.preferences;

import com.faforever.client.game.KnownFeaturedMod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class LastGamePrefs {
  private final StringProperty lastGameType = new SimpleStringProperty(KnownFeaturedMod.DEFAULT.getTechnicalName());
  private final StringProperty lastGameTitle = new SimpleStringProperty();
  private final StringProperty lastMap = new SimpleStringProperty();
  private final StringProperty lastGamePassword = new SimpleStringProperty();
  private final ObjectProperty<Integer> lastGameMinRating = new SimpleObjectProperty<>(null);
  private final ObjectProperty<Integer> lastGameMaxRating = new SimpleObjectProperty<>(null);
  private final BooleanProperty lastGameOnlyFriends = new SimpleBooleanProperty();
  private final BooleanProperty lastGameEnforceRating = new SimpleBooleanProperty(false);

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
}
