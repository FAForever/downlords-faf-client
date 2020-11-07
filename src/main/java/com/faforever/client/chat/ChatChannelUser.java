package com.faforever.client.chat;

import com.faforever.client.clan.Clan;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
  private final ObjectProperty<PlayerStatus> status;
  private final ObjectProperty<Image> avatar;
  private final ObjectProperty<Clan> clan;
  private final StringProperty clanTag;
  private final ObjectProperty<Image> countryFlag;
  private final StringProperty countryName;
  private final ObjectProperty<Image> mapImage;
  private final ObjectProperty<Image> statusImage;
  private final StringProperty statusTooltipText;
  private final BooleanProperty displayed;
  private final BooleanProperty populated;

  ChatChannelUser(String username, Color color, boolean moderator) {
    this(username, color, moderator, null);
  }

  ChatChannelUser(String username, Color color, boolean moderator, Player player) {
    this.username = new SimpleStringProperty(username);
    this.moderator = new SimpleBooleanProperty(moderator);
    this.color = new SimpleObjectProperty<>(color);
    this.player = new SimpleObjectProperty<>(player);
    this.lastActive = new SimpleObjectProperty<>();
    this.status = new SimpleObjectProperty<>();
    this.avatar = new SimpleObjectProperty<>();
    this.clan = new SimpleObjectProperty<>();
    this.clanTag = new SimpleStringProperty();
    this.countryFlag = new SimpleObjectProperty<>();
    this.countryName = new SimpleStringProperty();
    this.mapImage = new SimpleObjectProperty<>();
    this.statusImage = new SimpleObjectProperty<>();
    this.statusTooltipText = new SimpleStringProperty();
    this.displayed = new SimpleBooleanProperty(false);
    this.populated = new SimpleBooleanProperty(false);
  }

  public Optional<Player> getPlayer() {
    return Optional.ofNullable(player.get());
  }

  public void setPlayer(Player player) {
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

  public Optional<PlayerStatus> getStatus() {
    return Optional.ofNullable(status.get());
  }

  public void setStatus(PlayerStatus status) {
    this.status.set(status);
  }

  public ObjectProperty<PlayerStatus> statusProperty() {
    return status;
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

  public Optional<Image> getStatusImage() {
    return Optional.ofNullable(statusImage.get());
  }

  public void setStatusImage(Image statusImage) {
    this.statusImage.set(statusImage);
  }

  public ObjectProperty<Image> statusImageProperty() {
    return statusImage;
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

  public BooleanProperty displayedProperty() {
    return displayed;
  }

  public boolean isPopulated() {
    return populated.get();
  }

  public void setPopulated(boolean populated) {
    this.populated.set(populated);
  }

  public BooleanProperty populatedProperty() {
    return populated;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj.getClass() == this.getClass()
        && username.get().equalsIgnoreCase(((ChatChannelUser) obj).username.get());
  }

  Set<ChatUserCategory> getChatUserCategories() {
    Set<ChatUserCategory> userCategories = new HashSet<>();

    Optional<Player> playerOptional = Optional.ofNullable(player.get());
    if (!playerOptional.isPresent()) {
      userCategories.add(ChatUserCategory.CHAT_ONLY);
    } else {
      switch (playerOptional.get().getSocialStatus()) {
        case FRIEND:
          userCategories.add(ChatUserCategory.FRIEND);
          break;
        case FOE:
          userCategories.add(ChatUserCategory.FOE);
          break;
        case OTHER:
        case SELF:
          userCategories.add(ChatUserCategory.OTHER);
          break;
        default:
          // Nothing to do
      }
    }

    if (moderator.get()) {
      userCategories.add(ChatUserCategory.MODERATOR);
    }

    return userCategories;
  }
}
