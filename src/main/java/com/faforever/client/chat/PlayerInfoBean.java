package com.faforever.client.chat;

import com.faforever.client.legacy.domain.PlayerInfo;
import javafx.beans.property.*;

import java.util.Comparator;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on. Can also be a chat-only user. This
 * represents the combination of a PlayerInfo (from the FAF server) and a ChatUser (from IRC).
 */
public class PlayerInfoBean {

  public static final Comparator<PlayerInfoBean> SORT_BY_NAME_COMPARATOR = new Comparator<PlayerInfoBean>() {
    @Override
    public int compare(PlayerInfoBean o1, PlayerInfoBean o2) {
      return o1.getUsername().compareTo(o2.getUsername());
    }
  };

  private StringProperty username;
  private StringProperty clan;
  private StringProperty country;
  private StringProperty avatarUrl;
  private StringProperty avatarTooltip;
  private BooleanProperty friend;
  private BooleanProperty foe;
  private BooleanProperty moderator;
  private BooleanProperty chatOnly;
  private FloatProperty deviation;
  private FloatProperty mean;

  private PlayerInfoBean() {
    username = new SimpleStringProperty();
    clan = new SimpleStringProperty();
    country = new SimpleStringProperty();
    avatarUrl = new SimpleStringProperty();
    avatarTooltip = new SimpleStringProperty();
    friend = new SimpleBooleanProperty();
    foe = new SimpleBooleanProperty();
    moderator = new SimpleBooleanProperty();
    chatOnly = new SimpleBooleanProperty(true);
    deviation = new SimpleFloatProperty();
    mean = new SimpleFloatProperty();
  }

  public PlayerInfoBean(PlayerInfo playerInfo) {
    this();

    username.set(playerInfo.login);
    clan.set(playerInfo.clan);
    country.set(playerInfo.country);

    if (playerInfo.avatar != null) {
      avatarTooltip.set(playerInfo.avatar.tooltip);
      avatarUrl.set(playerInfo.avatar.url);
    }
  }

  public PlayerInfoBean(String username) {
    this();

    this.username.set(username);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj.getClass() == PlayerInfoBean.class)
        && username.equals(((PlayerInfoBean) obj).username);
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  public String getUsername() {
    return username.get();
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public String getClan() {
    return clan.get();
  }

  public StringProperty clanProperty() {
    return clan;
  }

  public String getCountry() {
    return country.get();
  }

  public StringProperty countryProperty() {
    return country;
  }

  public String getAvatarUrl() {
    return avatarUrl.get();
  }

  public StringProperty avatarUrlProperty() {
    return avatarUrl;
  }

  public String getAvatarTooltip() {
    return avatarTooltip.get();
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

  public void setFriend(boolean friend) {
    this.friend.set(friend);
  }

  public boolean isFoe() {
    return foe.get();
  }

  public BooleanProperty foeProperty() {
    return foe;
  }

  public void setFoe(boolean foe) {
    this.foe.set(foe);
  }

  public boolean isModerator() {
    return moderator.get();
  }

  public BooleanProperty moderatorProperty() {
    return moderator;
  }

  public void setModerator(boolean moderator) {
    this.moderator.set(moderator);
  }

  public boolean isChatOnly() {
    return chatOnly.get();
  }

  public BooleanProperty chatOnlyProperty() {
    return chatOnly;
  }

  public void setChatOnly(boolean chatOnly) {
    this.chatOnly.set(chatOnly);
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public void setClan(String clan) {
    this.clan.set(clan);
  }

  public void setCountry(String country) {
    this.country.set(country);
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl.set(avatarUrl);
  }

  public void setAvatarTooltip(String avatarTooltip) {
    this.avatarTooltip.set(avatarTooltip);
  }

  public boolean getFriend() {
    return friend.get();
  }

  public boolean getFoe() {
    return foe.get();
  }

  public boolean getModerator() {
    return moderator.get();
  }

  public boolean getIrcOnly() {
    return chatOnly.get();
  }

  public float getDeviation() {
    return deviation.get();
  }

  public FloatProperty deviationProperty() {
    return deviation;
  }

  public void setDeviation(float deviation) {
    this.deviation.set(deviation);
  }

  public float getMean() {
    return mean.get();
  }

  public FloatProperty meanProperty() {
    return mean;
  }

  public void setMean(float mean) {
    this.mean.set(mean);
  }

  public void updateFromPlayerInfo(PlayerInfo playerInfo) {
    setChatOnly(false);
    setDeviation(playerInfo.ratingDeviation);
    setClan(playerInfo.clan);
    setCountry(playerInfo.country);
    setMean(playerInfo.ratingMean);
    setDeviation(playerInfo.ratingDeviation);
    if (playerInfo.avatar != null) {
      setAvatarUrl(playerInfo.avatar.url);
      setAvatarTooltip(playerInfo.avatar.tooltip);
    }
  }
}
