package com.faforever.client.chat;

import com.faforever.client.legacy.GameStatus;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.PlayerInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableSet;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on. Can also be a chat-only user. This
 * represents the combination of a PlayerInfo (from the FAF server) and a ChatUser (from IRC).
 */
public class PlayerInfoBean {

  private final StringProperty username;
  private final StringProperty clan;
  private final StringProperty country;
  private final StringProperty avatarUrl;
  private final StringProperty avatarTooltip;
  private final BooleanProperty friend;
  private final BooleanProperty foe;
  private final SetProperty<String> moderatorInChannels;
  private final BooleanProperty chatOnly;
  private final FloatProperty deviation;
  private final FloatProperty mean;

  private final SimpleObjectProperty<GameStatus> gameStatus;

  public PlayerInfoBean(PlayerInfo playerInfo) {
    this();

    username.set(playerInfo.getLogin());
    clan.set(playerInfo.getClan());
    country.set(playerInfo.getCountry());

    if (playerInfo.getAvatar() != null) {
      avatarTooltip.set(playerInfo.getAvatar().getTooltip());
      avatarUrl.set(playerInfo.getAvatar().getUrl());
    }
  }

  private PlayerInfoBean() {
    username = new SimpleStringProperty();
    clan = new SimpleStringProperty();
    country = new SimpleStringProperty();
    avatarUrl = new SimpleStringProperty();
    avatarTooltip = new SimpleStringProperty();
    friend = new SimpleBooleanProperty();
    foe = new SimpleBooleanProperty();
    moderatorInChannels = new SimpleSetProperty<>();
    chatOnly = new SimpleBooleanProperty(true);
    deviation = new SimpleFloatProperty();
    mean = new SimpleFloatProperty();
    gameStatus = new SimpleObjectProperty<GameStatus>();
  }

  public PlayerInfoBean(String username) {
    this();
    this.gameStatus.set(GameStatus.NONE);
    this.username.set(username);
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj.getClass() == PlayerInfoBean.class)
        && username.equals(((PlayerInfoBean) obj).username);
  }

  public String getUsername() {
    return username.get();
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public String getClan() {
    return clan.get();
  }

  public void setClan(String clan) {
    this.clan.set(clan);
  }

  public StringProperty clanProperty() {
    return clan;
  }

  public String getCountry() {
    return country.get();
  }

  public void setCountry(String country) {
    this.country.set(country);
  }

  public StringProperty countryProperty() {
    return country;
  }

  public String getAvatarUrl() {
    return avatarUrl.get();
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl.set(avatarUrl);
  }

  public StringProperty avatarUrlProperty() {
    return avatarUrl;
  }

  public String getAvatarTooltip() {
    return avatarTooltip.get();
  }

  public void setAvatarTooltip(String avatarTooltip) {
    this.avatarTooltip.set(avatarTooltip);
  }

  public StringProperty avatarTooltipProperty() {
    return avatarTooltip;
  }

  public boolean isFriend() {
    return friend.get();
  }

  public BooleanProperty friendProperty() {
    return friend;
  }

  public boolean isFoe() {
    return foe.get();
  }

  public BooleanProperty foeProperty() {
    return foe;
  }

  public boolean isChatOnly() {
    return chatOnly.get();
  }

  public void setChatOnly(boolean chatOnly) {
    this.chatOnly.set(chatOnly);
  }

  public BooleanProperty chatOnlyProperty() {
    return chatOnly;
  }

  public boolean getFriend() {
    return friend.get();
  }

  public void setFriend(boolean friend) {
    this.friend.set(friend);
  }

  public boolean getFoe() {
    return foe.get();
  }

  public void setFoe(boolean foe) {
    this.foe.set(foe);
  }

  public ObservableSet<String> getModeratorInChannels() {
    return moderatorInChannels.get();
  }

  public SetProperty<String> moderatorInChannelsProperty() {
    return moderatorInChannels;
  }

  public boolean getIrcOnly() {
    return chatOnly.get();
  }

  public float getDeviation() {
    return deviation.get();
  }

  public void setDeviation(float deviation) {
    this.deviation.set(deviation);
  }

  public FloatProperty deviationProperty() {
    return deviation;
  }

  public float getMean() {
    return mean.get();
  }

  public void setMean(float mean) {
    this.mean.set(mean);
  }

  public FloatProperty meanProperty() {
    return mean;
  }

  public GameStatus getGameStatus() {
    return gameStatus.get();
  }

  public SimpleObjectProperty<GameStatus> gameStatusProperty() {
    return gameStatus;
  }

  public void setGameStatus(GameStatus gameStatus) {
    this.gameStatus.set(gameStatus);
  }

  public void setGameStatusFromGameState(GameState gameState){
    gameStatus.set(GameStatus.getFromGameState(gameState));
  }

  public void updateFromPlayerInfo(PlayerInfo playerInfo) {
    setChatOnly(false);
    setDeviation(playerInfo.getRatingDeviation());
    setClan(playerInfo.getClan());
    setCountry(playerInfo.getCountry());
    setMean(playerInfo.getRatingMean());
    setDeviation(playerInfo.getRatingDeviation());
    if (playerInfo.getAvatar() != null) {
      setAvatarUrl(playerInfo.getAvatar().getUrl());
      setAvatarTooltip(playerInfo.getAvatar().getTooltip());
    }
  }
}
