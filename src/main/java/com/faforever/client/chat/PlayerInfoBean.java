package com.faforever.client.chat;

import com.faforever.client.legacy.domain.PlayerInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
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
  private final FloatProperty globalRatingDeviation;
  private final FloatProperty globalRatingMean;
  private final FloatProperty leaderboardRatingDeviation;
  private final FloatProperty leaderboardRatingMean;

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
    globalRatingDeviation = new SimpleFloatProperty();
    globalRatingMean = new SimpleFloatProperty();
    leaderboardRatingDeviation = new SimpleFloatProperty();
    leaderboardRatingMean = new SimpleFloatProperty();
  }

  public PlayerInfoBean(String username) {
    this();

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

  public float getGlobalRatingDeviation() {
    return globalRatingDeviation.get();
  }

  public void setGlobalRatingDeviation(float globalRatingDeviation) {
    this.globalRatingDeviation.set(globalRatingDeviation);
  }

  public FloatProperty globalRatingDeviationProperty() {
    return globalRatingDeviation;
  }

  public float getGlobalRatingMean() {
    return globalRatingMean.get();
  }

  public void setGlobalRatingMean(float globalRatingMean) {
    this.globalRatingMean.set(globalRatingMean);
  }

  public FloatProperty globalRatingMeanProperty() {
    return globalRatingMean;
  }

  public float getLeaderboardRatingDeviation() {
    return leaderboardRatingDeviation.get();
  }

  public void setLeaderboardRatingDeviation(float leaderboardRatingDeviation) {
    this.leaderboardRatingDeviation.set(leaderboardRatingDeviation);
  }

  public FloatProperty leaderboardRatingDeviationProperty() {
    return leaderboardRatingDeviation;
  }

  public float getLeaderboardRatingMean() {
    return leaderboardRatingMean.get();
  }

  public void setLeaderboardRatingMean(float leaderboardRatingMean) {
    this.leaderboardRatingMean.set(leaderboardRatingMean);
  }

  public FloatProperty leaderboardRatingMeanProperty() {
    return leaderboardRatingMean;
  }

  public void updateFromPlayerInfo(PlayerInfo playerInfo) {
    setChatOnly(false);
    setClan(playerInfo.getClan());
    setCountry(playerInfo.getCountry());
    setGlobalRatingMean(playerInfo.getRatingMean());
    setGlobalRatingDeviation(playerInfo.getRatingDeviation());
    setLeaderboardRatingDeviation(playerInfo.getLadderRatingDeviation());
    setLeaderboardRatingMean(playerInfo.getLadderRatingMean());
    if (playerInfo.getAvatar() != null) {
      setAvatarUrl(playerInfo.getAvatar().getUrl());
      setAvatarTooltip(playerInfo.getAvatar().getTooltip());
    }
  }
}
