package com.faforever.client.chat;

import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.util.BeanUpdatePolicy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.pircbotx.User;

import java.util.Comparator;

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

  private PlayerInfoBean() {
    username = new SimpleStringProperty();
    clan = new SimpleStringProperty();
    country = new SimpleStringProperty();
    avatarUrl = new SimpleStringProperty();
    avatarTooltip = new SimpleStringProperty();
    friend = new SimpleBooleanProperty();
    foe = new SimpleBooleanProperty();
    moderator = new SimpleBooleanProperty();
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

  public PlayerInfoBean(User user) {
    this();

    username.set(user.getLogin());
  }

  public void update(PlayerInfoBean playerInfoBean, BeanUpdatePolicy policy) {
    String username = playerInfoBean.getUsername();
    String clan = playerInfoBean.getClan();
    String country = playerInfoBean.getCountry();
    String avatarUrl = playerInfoBean.getAvatarUrl();
    String avatarTooltip = playerInfoBean.getAvatarTooltip();

    switch (policy) {
      case OVERRIDE:
        this.username.set(username);
        this.clan.set(clan);
        this.country.set(country);
        this.avatarUrl.set(avatarUrl);
        this.avatarTooltip.set(avatarTooltip);
        break;

      case MERGE:
        if (username != null) {
          this.username.set(username);
        }
        if (clan != null) {
          this.clan.set(clan);
        }
        if (country != null) {
          this.country.set(country);
        }
        if (avatarUrl != null) {
          this.avatarUrl.set(avatarUrl);
        }
        if (avatarTooltip != null) {
          this.avatarTooltip.set(avatarTooltip);
        }
        break;
    }
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

  /**
   * Updates this instance with the username in the specified instance.
   */
  public void updateFromIrc(PlayerInfoBean playerInfoBean) {
   username.set(playerInfoBean.getUsername());
  }
}
