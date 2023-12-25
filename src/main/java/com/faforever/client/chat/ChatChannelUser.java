package com.faforever.client.chat;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.SocialStatus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Optional;

/**
 * Represents a chat user within a channel. If a user is in multiple channels, one instance per channel needs to be
 * created since e.g. the {@code isModerator} flag is specific to the channel.
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ChatChannelUser {

  public static final SimpleObjectProperty<ChatUserCategory> AWAY_PROPERTY = new SimpleObjectProperty<>(
      ChatUserCategory.AWAY);
  public static final SimpleObjectProperty<ChatUserCategory> MODERATOR_PROPERTY = new SimpleObjectProperty<>(
      ChatUserCategory.MODERATOR);
  @Getter
  @EqualsAndHashCode.Include
  @ToString.Include
  private final String username;
  @Getter
  @EqualsAndHashCode.Include
  @ToString.Include
  private final String channel;

  private final BooleanProperty away = new SimpleBooleanProperty();
  private final BooleanProperty moderator = new SimpleBooleanProperty();
  private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
  private final ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  private final ObservableValue<SocialStatus> playerSocialStatus = player.flatMap(PlayerBean::socialStatusProperty);
  private final ObservableValue<ChatUserCategory> category = away.flatMap(
      away -> away ? AWAY_PROPERTY : moderator.flatMap(
          moderator -> moderator ? MODERATOR_PROPERTY : player.flatMap(PlayerBean::socialStatusProperty)
                                                              .map(socialStatus -> switch (socialStatus) {
                                                                case FRIEND -> ChatUserCategory.FRIEND;
                                                                case FOE -> ChatUserCategory.FOE;
                                                                case SELF -> ChatUserCategory.SELF;
                                                                case OTHER -> ChatUserCategory.OTHER;
                                                              })
                                                              .orElse(ChatUserCategory.CHAT_ONLY)));

  public Optional<PlayerBean> getPlayer() {
    return Optional.ofNullable(player.get());
  }

  public void setPlayer(PlayerBean player) {
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

  public BooleanProperty moderatorProperty() {
    return moderator;
  }

  public ChatUserCategory getCategory() {
    return category.getValue();
  }

  public ObservableValue<ChatUserCategory> categoryProperty() {
    return category;
  }

  public boolean isAway() {
    return away.get();
  }

  public BooleanProperty awayProperty() {
    return away;
  }

  public void setAway(boolean away) {
    this.away.set(away);
  }
}
