package com.faforever.client.domain;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.SocialStatus;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static com.faforever.client.player.SocialStatus.OTHER;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Value
@Slf4j
public class PlayerBean extends AbstractEntityBean<PlayerBean> {

  @ToString.Include
  StringProperty username = new SimpleStringProperty();
  StringProperty clan = new SimpleStringProperty();
  StringProperty country = new SimpleStringProperty();
  ObjectProperty<AvatarBean> avatar = new SimpleObjectProperty<>();
  ObjectProperty<SocialStatus> socialStatus = new SimpleObjectProperty<>(OTHER);
  ObservableMap<String, LeaderboardRatingBean> leaderboardRatings = FXCollections.observableHashMap();
  ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  ReadOnlyObjectWrapper<PlayerStatus> status = new ReadOnlyObjectWrapper<>(PlayerStatus.IDLE);
  ObservableSet<ChatChannelUser> chatChannelUsers = FXCollections.observableSet();
  ObjectProperty<Instant> idleSince = new SimpleObjectProperty<>();
  StringProperty note = new SimpleStringProperty();

  public SocialStatus getSocialStatus() {
    return socialStatus.get();
  }

  public void setSocialStatus(SocialStatus socialStatus) {
    this.socialStatus.set(socialStatus);
  }

  public ObjectProperty<SocialStatus> socialStatusProperty() {
    return socialStatus;
  }

  public int getNumberOfGames() {
    return new ArrayList<>(leaderboardRatings.values()).stream().mapToInt(LeaderboardRatingBean::getNumberOfGames).sum();
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

  public void setAvatar(AvatarBean avatar) {
    this.avatar.set(avatar);
  }

  public AvatarBean getAvatar() {
    return avatar.get();
  }

  public ObjectProperty<AvatarBean> avatarProperty() {
    return avatar;
  }

  public ObservableMap<String, LeaderboardRatingBean> getLeaderboardRatings() {
    return leaderboardRatings;
  }

  public void setLeaderboardRatings(Map<String, LeaderboardRatingBean> leaderboardRatings) {
    this.leaderboardRatings.clear();
    if (leaderboardRatings != null) {
      this.leaderboardRatings.putAll(leaderboardRatings);
    }
  }

  public PlayerStatus getStatus() {
    return status.get();
  }

  public ReadOnlyObjectProperty<PlayerStatus> statusProperty() {
    return status.getReadOnlyProperty();
  }

  public GameBean getGame() {
    return game.get();
  }

  public void setGame(GameBean game) {
    this.game.set(game);
    status.unbind();
    if (game != null) {
      status.bind(Bindings.createObjectBinding(() -> getPlayerStatusFromGameStatus(game), game.statusProperty()));
    } else {
      status.set(PlayerStatus.IDLE);
    }
  }

  private PlayerStatus getPlayerStatusFromGameStatus(GameBean game) {
    GameStatus gameStatus = game.getStatus();
    if (gameStatus == GameStatus.OPEN) {
      if (game.getHost().equalsIgnoreCase(username.get())) {
        return PlayerStatus.HOSTING;
      } else {
        return PlayerStatus.LOBBYING;
      }
    } else if (gameStatus == GameStatus.PLAYING) {
      return PlayerStatus.PLAYING;
    } else {
      return PlayerStatus.IDLE;
    }
  }

  public ObjectProperty<GameBean> gameProperty() {
    return game;
  }

  public Instant getIdleSince() {
    return idleSince.get();
  }

  public void setIdleSince(Instant idleSince) {
    this.idleSince.set(idleSince);
  }

  public ObjectProperty<Instant> idleSinceProperty() {
    return idleSince;
  }

  public ObservableSet<ChatChannelUser> getChatChannelUsers() {
    return chatChannelUsers;
  }

  public StringProperty noteProperty() {
    return note;
  }

  public void setNote(String text) {
    note.set(text);
  }

  public String getNote() {
    return note.get();
  }

  public int getNumberOfGames(final String leaderboardName) {
    return Optional.ofNullable(leaderboardRatings.get(leaderboardName))
        .map(LeaderboardRatingBean::getNumberOfGames)
        .orElse(0);
  }
}
