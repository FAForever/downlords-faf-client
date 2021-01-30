package com.faforever.client.chat;

import com.faforever.client.clan.Clan;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.Player;
import com.faforever.client.player.SocialStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a chat user within a channel. If a user is in multiple channels, one instance per channel needs to be
 * created since e.g. the {@code isModerator} flag is specific to the channel.
 */
@ToString
public class ChatChannelUser {

  private final StringProperty username;
  private final BooleanProperty moderator;
  private final ObjectProperty<Color> color;
  private final ObjectProperty<Player> player;
  private final ObjectProperty<Instant> lastActive;
  private final ObjectProperty<PlayerStatus> gameStatus;
  private final ObjectProperty<SocialStatus> socialStatus;
  private final ObjectProperty<Image> avatar;
  private final ObjectProperty<Clan> clan;
  private final StringProperty clanTag;
  private final ObjectProperty<Image> countryFlag;
  private final StringProperty countryName;
  private final ObjectProperty<Image> mapImage;
  private final ObjectProperty<Image> gameStatusImage;
  private final StringProperty statusTooltipText;
  private final BooleanProperty displayed;
  private ChangeListener<SocialStatus> socialStatusChangeListener;
  private ChangeListener<PlayerStatus> gameStatusChangeListener;
  private ChangeListener<String> clanTagChangeListener;
  private ChangeListener<String> avatarChangeListener;
  private ChangeListener<String> countryInvalidationListener;
  private ChangeListener<Boolean> displayedChangeListener;

  ChatChannelUser(String username, boolean moderator) {
    this(username, moderator, null);
  }

  ChatChannelUser(String username, boolean moderator, Player player) {
    this.username = new SimpleStringProperty(username);
    this.moderator = new SimpleBooleanProperty(moderator);
    this.color = new SimpleObjectProperty<>();
    this.player = new SimpleObjectProperty<>(player);
    this.lastActive = new SimpleObjectProperty<>();
    this.gameStatus = new SimpleObjectProperty<>();
    this.socialStatus = new SimpleObjectProperty<>();
    this.avatar = new SimpleObjectProperty<>();
    this.clan = new SimpleObjectProperty<>();
    this.clanTag = new SimpleStringProperty();
    this.countryFlag = new SimpleObjectProperty<>();
    this.countryName = new SimpleStringProperty();
    this.mapImage = new SimpleObjectProperty<>();
    this.gameStatusImage = new SimpleObjectProperty<>();
    this.statusTooltipText = new SimpleStringProperty();
    this.displayed = new SimpleBooleanProperty(false);
    if (player != null) {
      player.getChatChannelUsers().add(this);
      socialStatus.setValue(player.getSocialStatus());
    }
  }

  public Optional<Player> getPlayer() {
    return Optional.ofNullable(player.get());
  }

  public void setPlayer(Player player) {
    if (this.player.get() != null) {
      this.player.get().getChatChannelUsers().remove(this);
      socialStatus.unbind();
      gameStatus.unbind();
      clanTag.unbind();
    }
    if (player != null) {
      player.getChatChannelUsers().add(this);
      socialStatus.bind(player.socialStatusProperty());
      gameStatus.bind(player.statusProperty());
      clanTag.bind(Bindings.createStringBinding(() -> {
        if (player.getClan() != null && !player.getClan().isBlank()) {
          return String.format("[%s]", player.getClan());
        }
        return null;
      }, player.clanProperty()));
    }
    this.player.set(player);
  }

  public ObjectProperty<Player> playerProperty() {
    return player;
  }

  public Optional<Color> getColor() {
    return Optional.ofNullable(color.get());
  }

  public void setColor(Color color) {
    this.color.set(color);
  }

  public ObjectProperty<Color> colorProperty() {
    return color;
  }

  public boolean isModerator() {
    return moderator.get();
  }

  public void setModerator(boolean moderator) {
    this.moderator.set(moderator);
  }

  public String getUsername() {
    return username.get();
  }

  public StringProperty usernameProperty() {
    return username;
  }

  @Override
  public int hashCode() {
    return username.get().hashCode();
  }

  public BooleanProperty moderatorProperty() {
    return moderator;
  }

  public Instant getLastActive() {
    return lastActive.get();
  }

  public void setLastActive(Instant lastActive) {
    this.lastActive.set(lastActive);
  }

  public ObjectProperty<Instant> lastActiveProperty() {
    return lastActive;
  }

  public Optional<PlayerStatus> getGameStatus() {
    return Optional.ofNullable(gameStatus.get());
  }

  public void setGameStatus(PlayerStatus gameStatus) {
    this.gameStatus.set(gameStatus);
  }

  public ObjectProperty<PlayerStatus> gameStatusProperty() {
    return gameStatus;
  }

  public Optional<SocialStatus> getSocialStatus() {
    return Optional.ofNullable(socialStatus.get());
  }

  public void setSocialStatus(SocialStatus socialStatus) {
    this.socialStatus.set(socialStatus);
  }

  public ObjectProperty<SocialStatus> socialStatusProperty() {
    return socialStatus;
  }

  public Optional<Image> getAvatar() {
    return Optional.ofNullable(avatar.get());
  }

  public void setAvatar(Image avatar) {
    this.avatar.set(avatar);
  }

  public ObjectProperty<Image> avatarProperty() {
    return avatar;
  }

  public Optional<Clan> getClan() {
    return Optional.ofNullable(clan.get());
  }

  public void setClan(Clan clan) {
    this.clan.set(clan);
  }

  public ObjectProperty<Clan> clanProperty() {
    return clan;
  }

  public Optional<String> getClanTag() {
    return Optional.ofNullable(clanTag.get());
  }

  public void setClanTag(String clanTag) {
    this.clanTag.set(clanTag);
  }

  public StringProperty clanTagProperty() {
    return clanTag;
  }

  public Optional<Image> getCountryFlag() {
    return Optional.ofNullable(countryFlag.get());
  }

  public void setCountryFlag(Image countryFlag) {
    this.countryFlag.set(countryFlag);
  }

  public ObjectProperty<Image> countryFlagProperty() {
    return countryFlag;
  }

  public Optional<String> getCountryName() {
    return Optional.ofNullable(countryName.get());
  }

  public void setCountryName(String countryName) {
    this.countryName.set(countryName);
  }

  public StringProperty countryNameProperty() {
    return countryName;
  }

  public Optional<Image> getMapImage() {
    return Optional.ofNullable(mapImage.get());
  }

  public void setMapImage(Image mapImage) {
    this.mapImage.set(mapImage);
  }

  public ObjectProperty<Image> mapImageProperty() {
    return mapImage;
  }

  public Optional<Image> getGameStatusImage() {
    return Optional.ofNullable(gameStatusImage.get());
  }

  public void setGameStatusImage(Image gameStatusImage) {
    this.gameStatusImage.set(gameStatusImage);
  }

  public ObjectProperty<Image> gameStatusImageProperty() {
    return gameStatusImage;
  }

  public Optional<String> getStatusTooltipText() {
    return Optional.ofNullable(statusTooltipText.get());
  }

  public void setStatusTooltipText(String value) {
    this.statusTooltipText.set(value);
  }

  public StringProperty statusTooltipTextProperty() {
    return statusTooltipText;
  }

  public boolean isDisplayed() {
    return displayed.get();
  }

  public void setDisplayed(boolean displayed) {
    this.displayed.set(displayed);
  }

  public ChangeListener<Boolean> getDisplayedChangeListener() {
    return displayedChangeListener;
  }

  public void setDisplayedChangeListener(ChangeListener<Boolean> listener) {
    if (player.get() != null) {
      if (displayedChangeListener != null) {
        JavaFxUtil.removeListener(displayed, displayedChangeListener);
      }
      displayedChangeListener = listener;
      if (displayedChangeListener != null) {
        JavaFxUtil.addListener(displayed, displayedChangeListener);
      }
    }
  }

  public ChangeListener<SocialStatus> getSocialStatusChangeListener() {
    return socialStatusChangeListener;
  }

  public void setSocialStatusChangeListener(ChangeListener<SocialStatus> listener) {
    if (socialStatusChangeListener != null) {
      JavaFxUtil.removeListener(socialStatus, socialStatusChangeListener);
    }
    socialStatusChangeListener = listener;
    if (socialStatusChangeListener != null) {
      JavaFxUtil.addListener(socialStatus, socialStatusChangeListener);
    }
  }

  public ChangeListener<PlayerStatus> getGameStatusChangeListener() {
    return gameStatusChangeListener;
  }

  public void setGameStatusChangeListener(ChangeListener<PlayerStatus> listener) {
    if (gameStatusChangeListener != null) {
      JavaFxUtil.removeListener(gameStatus, gameStatusChangeListener);
    }
    gameStatusChangeListener = listener;
    if (gameStatusChangeListener != null) {
      JavaFxUtil.addListener(gameStatus, gameStatusChangeListener);
    }
  }

  public ChangeListener<String> getClanTagChangeListener() {
    return clanTagChangeListener;
  }

  public void setClanTagChangeListener(ChangeListener<String> listener) {
    if (clanTagChangeListener != null) {
      JavaFxUtil.removeListener(clanTag, clanTagChangeListener);
    }
    clanTagChangeListener = listener;
    if (clanTagChangeListener != null) {
      JavaFxUtil.addListener(clanTag, clanTagChangeListener);
    }
  }

  public ChangeListener<String> getCountryInvalidationListener() {
    return countryInvalidationListener;
  }

  public void setCountryChangeListener(ChangeListener<String> listener) {
    if (player.get() != null) {
      if (countryInvalidationListener != null) {
        JavaFxUtil.removeListener(player.get().countryProperty(), countryInvalidationListener);
      }
      countryInvalidationListener = listener;
      if (countryInvalidationListener != null) {
        JavaFxUtil.addListener(player.get().countryProperty(), countryInvalidationListener);
      }
    }
  }

  public ChangeListener<String> getAvatarChangeListener() {
    return avatarChangeListener;
  }

  public void setAvatarChangeListener(ChangeListener<String> listener) {
    if (player.get() != null) {
      if (avatarChangeListener != null) {
        JavaFxUtil.removeListener(player.get().avatarUrlProperty(), avatarChangeListener);
      }
      avatarChangeListener = listener;
      if (avatarChangeListener != null) {
        JavaFxUtil.addListener(player.get().avatarUrlProperty(), avatarChangeListener);
      }
    }
  }

  public void removeListeners() {
    if (avatarChangeListener != null) {
      JavaFxUtil.removeListener(player.get().avatarUrlProperty(), avatarChangeListener);
      avatarChangeListener = null;
    }
    if (countryInvalidationListener != null) {
      JavaFxUtil.removeListener(player.get().countryProperty(), countryInvalidationListener);
      countryInvalidationListener = null;
    }
    if (clanTagChangeListener != null) {
      JavaFxUtil.removeListener(clanTag, clanTagChangeListener);
      clanTagChangeListener = null;
    }
    if (gameStatusChangeListener != null) {
      JavaFxUtil.removeListener(gameStatus, gameStatusChangeListener);
      gameStatusChangeListener = null;
    }
    if (socialStatusChangeListener != null) {
      JavaFxUtil.removeListener(socialStatus, socialStatusChangeListener);
      socialStatusChangeListener = null;
    }
    if (displayedChangeListener != null) {
      JavaFxUtil.addListener(displayed, displayedChangeListener);
      displayedChangeListener = null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj.getClass() == this.getClass()
        && username.get().equalsIgnoreCase(((ChatChannelUser) obj).username.get());
  }

  Set<ChatUserCategory> getChatUserCategories() {
    Set<ChatUserCategory> userCategories = new HashSet<>();

    if (socialStatus.get() == null) {
      userCategories.add(ChatUserCategory.CHAT_ONLY);
    } else {
      ChatUserCategory category = switch (socialStatus.get()) {
        case FRIEND -> ChatUserCategory.FRIEND;
        case FOE -> ChatUserCategory.FOE;
        case OTHER, SELF -> ChatUserCategory.OTHER;
      };
      userCategories.add(category);
    }

    if (moderator.get()) {
      userCategories.add(ChatUserCategory.MODERATOR);
    }

    return userCategories;
  }
}
