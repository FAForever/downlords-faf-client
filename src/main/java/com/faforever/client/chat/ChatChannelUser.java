package com.faforever.client.chat;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.SocialStatus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatChannelUser {

  @EqualsAndHashCode.Include
  private final ReadOnlyStringWrapper username = new ReadOnlyStringWrapper();
  @EqualsAndHashCode.Include
  private final ReadOnlyStringWrapper channel = new ReadOnlyStringWrapper();
  private final BooleanProperty moderator = new SimpleBooleanProperty();
  private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
  private final ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  private final ObjectProperty<Instant> lastActive = new SimpleObjectProperty<>();
  private final ObjectProperty<Image> avatar = new SimpleObjectProperty<>();
  private final ObjectProperty<Image> countryFlag = new SimpleObjectProperty<>();
  private final StringProperty countryName = new SimpleStringProperty();
  private final ObjectProperty<Image> mapImage = new SimpleObjectProperty<>();
  private final ObjectProperty<Image> gameStatusImage = new SimpleObjectProperty<>();
  private final StringProperty statusTooltipText = new SimpleStringProperty();
  private final BooleanProperty displayed = new SimpleBooleanProperty(false);

  private final ObservableValue<PlayerStatus> playerStatus = player.flatMap(PlayerBean::statusProperty);
  private final ObservableValue<SocialStatus> socialStatus = player.flatMap(PlayerBean::socialStatusProperty);
  private final ObservableValue<String> clanTag = player.flatMap(PlayerBean::clanProperty)
      .map(clanTag -> clanTag.isBlank() ? null : String.format("[%s]", clanTag));

  public ChatChannelUser(String username, String channel) {
    this(username, channel, false);
  }

  public ChatChannelUser(String username, String channel, boolean moderator) {
    this.username.set(username);
    this.channel.set(channel);
    setModerator(moderator);
  }

  public Optional<PlayerBean> getPlayer() {
    return Optional.ofNullable(player.get());
  }

  public void setPlayer(PlayerBean player) {
    if (this.player.get() != null) {
      this.player.get().getChatChannelUsers().remove(this);
    }
    if (player != null) {
      player.getChatChannelUsers().add(this);
    }
    this.player.set(player);
  }

  public ObjectProperty<PlayerBean> playerProperty() {
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

  public ReadOnlyStringProperty usernameProperty() {
    return username.getReadOnlyProperty();
  }

  public String getChannel() {
    return channel.get();
  }

  public ReadOnlyStringProperty channelProperty() {
    return channel.getReadOnlyProperty();
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

  public Optional<PlayerStatus> getPlayerStatus() {
    return Optional.ofNullable(playerStatus.getValue());
  }

  public ObservableValue<PlayerStatus> playerStatusProperty() {
    return playerStatus;
  }

  public Optional<SocialStatus> getSocialStatus() {
    return Optional.ofNullable(socialStatus.getValue());
  }

  public ObservableValue<SocialStatus> socialStatusProperty() {
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

  public Optional<String> getClanTag() {
    return Optional.ofNullable(clanTag.getValue());
  }

  public ObservableValue<String> clanTagProperty() {
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

  public Set<ChatUserCategory> getChatUserCategories() {
    Set<ChatUserCategory> userCategories = new HashSet<>();

    getSocialStatus().ifPresentOrElse(socialStatus -> userCategories.add(switch (socialStatus) {
      case FRIEND -> ChatUserCategory.FRIEND;
      case FOE -> ChatUserCategory.FOE;
      case OTHER -> ChatUserCategory.OTHER;
      case SELF -> ChatUserCategory.SELF;
    }), () -> userCategories.add(ChatUserCategory.CHAT_ONLY));

    if (moderator.get()) {
      userCategories.add(ChatUserCategory.MODERATOR);
    }

    return userCategories;
  }
}
