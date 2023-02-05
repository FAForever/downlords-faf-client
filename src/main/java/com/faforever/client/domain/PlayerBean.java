package com.faforever.client.domain;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.player.SocialStatus;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
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

  private static final SimpleObjectProperty<PlayerStatus> PLAYING_STATUS_PROPERTY = new SimpleObjectProperty<>(PlayerStatus.PLAYING);
  private static final SimpleObjectProperty<PlayerStatus> IDLE_STATUS_PROPERTY = new SimpleObjectProperty<>(PlayerStatus.IDLE);

  @ToString.Include
  StringProperty username = new SimpleStringProperty();
  StringProperty clan = new SimpleStringProperty();
  StringProperty country = new SimpleStringProperty();
  ObjectProperty<AvatarBean> avatar = new SimpleObjectProperty<>();
  ObjectProperty<SocialStatus> socialStatus = new SimpleObjectProperty<>(OTHER);
  MapProperty<String, LeaderboardRatingBean> leaderboardRatings = new SimpleMapProperty<>(FXCollections.observableHashMap());
  ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  ObservableValue<PlayerStatus> status = game.flatMap(this::statusPropertyFromGame).orElse(PlayerStatus.IDLE);
  ObservableSet<ChatChannelUser> chatChannelUsers = FXCollections.observableSet();
  ObjectProperty<Instant> idleSince = new SimpleObjectProperty<>();
  StringProperty note = new SimpleStringProperty();
  ObservableValue<Integer> numGames = leaderboardRatings.map(ratings -> ratings.values()
      .stream()
      .mapToInt(LeaderboardRatingBean::getNumberOfGames)
      .sum()).orElse(0);


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
    return numGames.getValue();
  }

  public ObservableValue<Integer> numberOfGamesProperty() {
    return numGames;
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
    return status.getValue();
  }

  public ObservableValue<PlayerStatus> statusProperty() {
    return status;
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

  public int getNumberOfGamesForLeaderboard(final String leaderboardName) {
    return Optional.ofNullable(leaderboardRatings.get(leaderboardName))
        .map(LeaderboardRatingBean::getNumberOfGames)
        .orElse(0);
  }

  private ObservableValue<PlayerStatus> statusPropertyFromGame(GameBean game) {
    return game.statusProperty().flatMap(status -> switch (status) {
      case OPEN -> game.hostProperty().map(host -> host.equalsIgnoreCase(getUsername()) ? PlayerStatus.HOSTING : PlayerStatus.LOBBYING);
      case PLAYING -> PLAYING_STATUS_PROPERTY;
      default -> IDLE_STATUS_PROPERTY;
    });
  }
}
