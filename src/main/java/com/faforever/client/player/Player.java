package com.faforever.client.player;

import com.faforever.client.avatar.AvatarBean;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.game.Game;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.leaderboard.LeaderboardRating;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.PlayerInfo;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.faforever.client.player.SocialStatus.OTHER;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on.
 */
@NoArgsConstructor
public class Player {

  private final IntegerProperty id = new SimpleIntegerProperty();
  private final StringProperty username = new SimpleStringProperty();
  private final StringProperty clan = new SimpleStringProperty();
  private final StringProperty country = new SimpleStringProperty();
  private final StringProperty avatarUrl = new SimpleStringProperty();
  private final StringProperty avatarTooltip = new SimpleStringProperty();
  private final ObjectProperty<SocialStatus> socialStatus = new SimpleObjectProperty<>(OTHER);
  private final MapProperty<String, LeaderboardRating> leaderboardRatings = new SimpleMapProperty<>(FXCollections.emptyObservableMap());
  private final ObjectProperty<Game> game = new SimpleObjectProperty<>();
  private final ObjectProperty<PlayerStatus> status = new SimpleObjectProperty<>(PlayerStatus.IDLE);
  private final ObservableSet<ChatChannelUser> chatChannelUsers = FXCollections.observableSet();
  private final ObjectProperty<Instant> idleSince = new SimpleObjectProperty<>(Instant.now());
  private final ObservableList<NameRecord> names = FXCollections.observableArrayList();
  private final InvalidationListener gameStatusListener = observable -> updateGameStatus();

  public Player(String username) {
    this();
    setUsername(username);
  }

  private void updateGameStatus() {
    Game game = getGame();
    if (game != null) {
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
        setGame(null);
        status.set(PlayerStatus.IDLE);
      }
    } else {
      status.set(PlayerStatus.IDLE);
    }
  }

  public static Player fromDto(com.faforever.commons.api.dto.Player dto) {
    Player player = new Player(dto.getLogin());
    player.setId(Integer.parseInt(dto.getId()));
    player.setUsername(dto.getLogin());
    if (dto.getNames() != null) {
      player.getNames().addAll(dto.getNames().stream().map(NameRecord::fromDto).collect(Collectors.toList()));
    }
    return player;
  }

  public static Player fromPlayerInfo(PlayerInfo dto) {
    Player player = new Player(dto.getLogin());
    player.updateFromPlayerInfo(dto);
    return player;
  }

  public ObservableList<NameRecord> getNames() {
    return names;
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

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public int getNumberOfGames() {
    return leaderboardRatings.values().stream().mapToInt(LeaderboardRating::getNumberOfGames).sum();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id.get(), username.get());
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && (obj.getClass() == Player.class)
        && (getId() == ((Player) obj).getId() && getId() != 0 ||
        getUsername().equalsIgnoreCase(((Player) obj).getUsername()));
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

  @SneakyThrows
  public void setAvatar(AvatarBean avatar) {
    if (avatar != null) {
      if (avatar.getUrl() != null) {
        setAvatarUrl(avatar.getUrl().toExternalForm());
      } else {
        setAvatarUrl(null);
      }
      setAvatarTooltip(avatar.getDescription());
    } else {
      setAvatarUrl(null);
      setAvatarTooltip(null);
    }
  }

  public String getAvatarUrl() {
    return avatarUrl.get();
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl.set(avatarUrl);
  }

  public StringProperty avatarUrlProperty() {
    return avatarUrl;
  }

  public String getAvatarTooltip() {
    return avatarTooltip.get();
  }

  public void setAvatarTooltip(String avatarTooltip) {
    this.avatarTooltip.set(avatarTooltip);
  }

  public StringProperty avatarTooltipProperty() {
    return avatarTooltip;
  }

  @NotNull
  public Map<String, LeaderboardRating> getLeaderboardRatings() {
    return leaderboardRatings.get();
  }

  public void setLeaderboardRatings(Map<String, LeaderboardRating> leaderboardRatings) {
    this.leaderboardRatings.set(FXCollections.observableMap(leaderboardRatings));
  }

  public MapProperty<String, LeaderboardRating> leaderboardRatingMapProperty() {
    return leaderboardRatings;
  }

  public PlayerStatus getStatus() {
    return status.get();
  }

  public ReadOnlyObjectProperty<PlayerStatus> statusProperty() {
    return status;
  }

  public Game getGame() {
    return game.get();
  }

  public void setGame(Game game) {
    Game currentGame = this.game.get();
    if ((game == null || game.getStatus() == GameStatus.CLOSED) && currentGame != null) {
      currentGame.removeListeners();
      this.game.set(null);
      status.set(PlayerStatus.IDLE);
    } else if (game != null) {
      this.game.set(game);
      game.setGameStatusListener(gameStatusListener);
      game.setHostListener(gameStatusListener);
    }
  }

  public ObjectProperty<Game> gameProperty() {
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

  public void updateFromPlayerInfo(PlayerInfo playerInfo) {
    setUsername(playerInfo.getLogin());
    setId(playerInfo.getId());
    setClan(playerInfo.getClan());
    setCountry(playerInfo.getCountry());

    if (playerInfo.getRatings() != null) {
      Map<String, LeaderboardRating> ratingMap = new HashMap<>();
      playerInfo.getRatings().forEach((key, value) -> ratingMap.put(key, LeaderboardRating.fromDto(value)));
      setLeaderboardRatings(ratingMap);
    }

    if (playerInfo.getAvatar() != null) {
      setAvatarUrl(playerInfo.getAvatar().getUrl());
      setAvatarTooltip(playerInfo.getAvatar().getTooltip());
    }
  }
}
