package com.faforever.client.domain;

import com.faforever.client.game.PlayerGameStatus;
import com.faforever.client.player.ServerStatus;
import com.faforever.client.player.SocialStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static com.faforever.client.player.SocialStatus.OTHER;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Slf4j
public class PlayerBean extends AbstractEntityBean {

  private static final SimpleObjectProperty<PlayerGameStatus> PLAYING_STATUS_PROPERTY = new SimpleObjectProperty<>(
      PlayerGameStatus.PLAYING);
  private static final SimpleObjectProperty<PlayerGameStatus> IDLE_STATUS_PROPERTY = new SimpleObjectProperty<>(
      PlayerGameStatus.IDLE);

  @ToString.Include
  private final StringProperty username = new SimpleStringProperty();
  private final StringProperty clan = new SimpleStringProperty();
  private final StringProperty country = new SimpleStringProperty();
  private final ObjectProperty<AvatarBean> avatar = new SimpleObjectProperty<>();
  private final ObjectProperty<ServerStatus> serverStatus = new SimpleObjectProperty<>();
  private final ObjectProperty<SocialStatus> socialStatus = new SimpleObjectProperty<>(OTHER);
  private final ObjectProperty<Map<String, LeaderboardRatingBean>> leaderboardRatings = new SimpleObjectProperty<>(
      Map.of());
  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final ObservableValue<PlayerGameStatus> gameStatus = game.flatMap(this::statusPropertyFromGame)
                                                                   .orElse(PlayerGameStatus.IDLE);
  private final StringProperty note = new SimpleStringProperty();
  private final ObservableValue<Integer> numberOfGames = leaderboardRatings.map(
      ratings -> ratings.values().stream().mapToInt(LeaderboardRatingBean::getNumberOfGames).sum()).orElse(0);

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
    return numberOfGames.getValue();
  }

  public ObservableValue<Integer> numberOfGamesProperty() {
    return numberOfGames;
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

  public ServerStatus getServerStatus() {
    return serverStatus.get();
  }

  public ObjectProperty<ServerStatus> serverStatusProperty() {
    return serverStatus;
  }

  public void setServerStatus(ServerStatus serverStatus) {
    this.serverStatus.set(serverStatus);
  }

  public Map<String, LeaderboardRatingBean> getLeaderboardRatings() {
    return leaderboardRatings.get();
  }

  public void setLeaderboardRatings(Map<String, LeaderboardRatingBean> leaderboardRatings) {
    this.leaderboardRatings.setValue(leaderboardRatings == null ? Map.of() : Map.copyOf(leaderboardRatings));
  }

  public ObjectProperty<Map<String, LeaderboardRatingBean>> leaderboardRatingsProperty() {
    return leaderboardRatings;
  }

  public PlayerGameStatus getGameStatus() {
    return gameStatus.getValue();
  }

  public ObservableValue<PlayerGameStatus> gameStatusProperty() {
    return gameStatus;
  }

  public GameBean getGame() {
    return game.get();
  }

  public void setGame(GameBean game) {
    this.game.set(game);
  }

  public ObjectProperty<GameBean> gameProperty() {
    return game;
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

  public int getNumberOfGamesForLeaderboard(final String leaderboardName) {
    return Optional.ofNullable(leaderboardRatings.get()).map(ratings -> ratings.get(leaderboardName))
                   .map(LeaderboardRatingBean::getNumberOfGames)
                   .orElse(0);
  }

  private ObservableValue<PlayerGameStatus> statusPropertyFromGame(GameBean game) {
    return game.statusProperty().flatMap(status -> switch (status) {
      case OPEN -> game.hostProperty()
                       .map(host -> host.equalsIgnoreCase(
                           getUsername()) ? PlayerGameStatus.HOSTING : PlayerGameStatus.LOBBYING);
      case PLAYING -> PLAYING_STATUS_PROPERTY;
      case CLOSED, UNKNOWN -> IDLE_STATUS_PROPERTY;
    });
  }
}
