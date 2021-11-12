package com.faforever.client.domain;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.SocialStatus;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.faforever.client.player.SocialStatus.OTHER;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Value
public class PlayerBean extends AbstractEntityBean<PlayerBean> {

  @ToString.Include
  StringProperty username = new SimpleStringProperty();
  StringProperty clan = new SimpleStringProperty();
  StringProperty country = new SimpleStringProperty();
  ObjectProperty<AvatarBean> avatar = new SimpleObjectProperty<>();
  StringProperty avatarUrl = new SimpleStringProperty();
  StringProperty avatarDescription = new SimpleStringProperty();
  ObjectProperty<SocialStatus> socialStatus = new SimpleObjectProperty<>(OTHER);
  ObservableMap<String, LeaderboardRatingBean> leaderboardRatings = FXCollections.observableHashMap();
  ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  ObjectProperty<PlayerStatus> status = new SimpleObjectProperty<>(PlayerStatus.IDLE);
  ObservableSet<ChatChannelUser> chatChannelUsers = FXCollections.observableSet();
  ObjectProperty<Instant> idleSince = new SimpleObjectProperty<>();
  ObservableList<NameRecordBean> names = FXCollections.observableArrayList();
  InvalidationListener gameStatusListener = observable -> updateGameStatus();

  private void updateGameStatus() {
    GameBean game = getGame();
    if (game == null) {
      status.set(PlayerStatus.IDLE);
      return;
    }

    GameStatus gameStatus = game.getStatus();
    if (gameStatus == GameStatus.OPEN) {
      if (game.getHost().equalsIgnoreCase(username.get())) {
        status.set(PlayerStatus.HOSTING);
      } else {
        status.set(PlayerStatus.LOBBYING);
      }
    } else if (gameStatus == GameStatus.PLAYING) {
      status.set(PlayerStatus.PLAYING);
    } else {
      status.set(PlayerStatus.IDLE);
    }
  }

  public ObservableList<NameRecordBean> getNames() {
    return names;
  }

  public void setNames(List<NameRecordBean> names) {
    if (names == null) {
      names = List.of();
    }
    this.names.setAll(names);
  }

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
    return leaderboardRatings.values().stream().mapToInt(LeaderboardRatingBean::getNumberOfGames).sum();
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

  @NotNull
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
    return status;
  }

  public GameBean getGame() {
    return game.get();
  }

  public void setGame(GameBean game) {
    GameBean currentGame = this.game.get();
    if (currentGame == game) {
      return;
    }

    if (currentGame != null) {
      currentGame.removeListeners();
    }

    this.game.set(game);

    if (game != null) {
      game.setGameStatusListener(gameStatusListener);
      game.setHostListener(gameStatusListener);
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
}
