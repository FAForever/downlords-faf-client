package com.faforever.client.player;

import com.faforever.client.api.dto.GlobalRating;
import com.faforever.client.api.dto.Ladder1v1Rating;
import com.faforever.client.chat.SocialStatus;
import com.faforever.client.game.Game;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.remote.domain.GameStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.time.Instant;
import java.util.Optional;

import static com.faforever.client.chat.SocialStatus.OTHER;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on. Can also be a chat-only user. This
 * represents the combination of a PlayersInfo (from the FAF server) and a ChatUser (from IRC).
 */
public class Player {

  private final IntegerProperty id;
  private final StringProperty username;
  private final StringProperty clan;
  private final StringProperty country;
  private final StringProperty avatarUrl;
  private final StringProperty avatarTooltip;
  private final ObjectProperty<SocialStatus> socialStatus;
  private final SetProperty<String> moderatorForChannels;
  private final BooleanProperty chatOnly;
  private final FloatProperty globalRatingDeviation;
  private final FloatProperty globalRatingMean;
  private final FloatProperty leaderboardRatingDeviation;
  private final FloatProperty leaderboardRatingMean;
  private final ObjectProperty<Game> game;
  private final SimpleObjectProperty<PlayerStatus> status;
  private final IntegerProperty numberOfGames;
  private final ObjectProperty<Instant> idleSince;

  public Player(com.faforever.client.remote.domain.Player player) {
    this();

    username.set(player.getLogin());
    clan.set(player.getClan());
    country.set(player.getCountry());

    if (player.getAvatar() != null) {
      avatarTooltip.set(player.getAvatar().getTooltip());
      avatarUrl.set(player.getAvatar().getUrl());
    }
  }

  private Player() {
    id = new SimpleIntegerProperty();
    username = new SimpleStringProperty();
    clan = new SimpleStringProperty();
    country = new SimpleStringProperty();
    avatarUrl = new SimpleStringProperty();
    avatarTooltip = new SimpleStringProperty();
    moderatorForChannels = new SimpleSetProperty<>(FXCollections.observableSet());
    chatOnly = new SimpleBooleanProperty(true);
    globalRatingDeviation = new SimpleFloatProperty();
    globalRatingMean = new SimpleFloatProperty();
    leaderboardRatingDeviation = new SimpleFloatProperty();
    leaderboardRatingMean = new SimpleFloatProperty();
    status = new SimpleObjectProperty<>(PlayerStatus.IDLE);
    game = new SimpleObjectProperty<>();
    numberOfGames = new SimpleIntegerProperty();
    socialStatus = new SimpleObjectProperty<>(OTHER);
    idleSince = new SimpleObjectProperty<>(Instant.now());
  }

  public Player(String username) {
    this();
    this.username.set(username);
  }

  public static Player fromDto(com.faforever.client.api.dto.Player dto) {
    Player player = new Player(dto.getLogin());
    player.setId(Integer.parseInt(dto.getId()));
    player.setUsername(dto.getLogin());
    player.setGlobalRatingMean(Optional.ofNullable(dto.getGlobalRating()).map(GlobalRating::getMean).orElse(0d).floatValue());
    player.setGlobalRatingDeviation(Optional.ofNullable(dto.getGlobalRating()).map(GlobalRating::getDeviation).orElse(0d).floatValue());
    player.setLeaderboardRatingMean(Optional.ofNullable(dto.getLadder1v1Rating()).map(Ladder1v1Rating::getMean).orElse(0d).floatValue());
    player.setLeaderboardRatingDeviation(Optional.ofNullable(dto.getLadder1v1Rating()).map(Ladder1v1Rating::getDeviation).orElse(0d).floatValue());
    return player;
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
    return numberOfGames.get();
  }

  public void setNumberOfGames(int numberOfGames) {
    this.numberOfGames.set(numberOfGames);
  }

  public IntegerProperty numberOfGamesProperty() {
    return numberOfGames;
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && (obj.getClass() == Player.class)
        && getUsername().equalsIgnoreCase(((Player) obj).getUsername());
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

  public boolean isChatOnly() {
    return chatOnly.get();
  }

  public BooleanProperty chatOnlyProperty() {
    return chatOnly;
  }

  public ObservableSet<String> getModeratorForChannels() {
    return moderatorForChannels.get();
  }

  public SetProperty<String> moderatorForChannelsProperty() {
    return moderatorForChannels;
  }

  public boolean getChatOnly() {
    return chatOnly.get();
  }

  public void setChatOnly(boolean chatOnly) {
    this.chatOnly.set(chatOnly);
  }

  public float getGlobalRatingDeviation() {
    return globalRatingDeviation.get();
  }

  public void setGlobalRatingDeviation(float globalRatingDeviation) {
    this.globalRatingDeviation.set(globalRatingDeviation);
  }

  public FloatProperty globalRatingDeviationProperty() {
    return globalRatingDeviation;
  }

  public float getGlobalRatingMean() {
    return globalRatingMean.get();
  }

  public void setGlobalRatingMean(float globalRatingMean) {
    this.globalRatingMean.set(globalRatingMean);
  }

  public FloatProperty globalRatingMeanProperty() {
    return globalRatingMean;
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
    this.game.set(game);
    if (game == null) {
      status.unbind();
      status.set(PlayerStatus.IDLE);
    } else {
      this.status.bind(Bindings.createObjectBinding(() -> {
        if (getGame().getStatus() == GameStatus.OPEN) {
          if (getGame().getHost().equalsIgnoreCase(username.get())) {
            return PlayerStatus.HOSTING;
          }
          return PlayerStatus.LOBBYING;
        } else if (getGame().getStatus() == GameStatus.CLOSED) {
          return PlayerStatus.IDLE;
        }
        return PlayerStatus.PLAYING;
      }, game.statusProperty()));
    }
  }

  public ObjectProperty<Game> gameProperty() {
    return game;
  }

  public float getLeaderboardRatingMean() {
    return leaderboardRatingMean.get();
  }

  public void setLeaderboardRatingMean(float leaderboardRatingMean) {
    this.leaderboardRatingMean.set(leaderboardRatingMean);
  }

  public FloatProperty leaderboardRatingMeanProperty() {
    return leaderboardRatingMean;
  }

  public float getLeaderboardRatingDeviation() {
    return leaderboardRatingDeviation.get();
  }

  public void setLeaderboardRatingDeviation(float leaderboardRatingDeviation) {
    this.leaderboardRatingDeviation.set(leaderboardRatingDeviation);
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

  public FloatProperty leaderboardRatingDeviationProperty() {
    return leaderboardRatingDeviation;
  }

  public void updateFromPlayerInfo(com.faforever.client.remote.domain.Player player) {
    setId(player.getId());
    setChatOnly(false);
    setClan(player.getClan());
    setCountry(player.getCountry());

    if (player.getGlobalRating() != null) {
      setGlobalRatingMean(player.getGlobalRating()[0]);
      setGlobalRatingDeviation(player.getGlobalRating()[1]);
    }
    if (player.getLadderRating() != null) {
      setLeaderboardRatingMean(player.getLadderRating()[0]);
      setLeaderboardRatingDeviation(player.getLadderRating()[1]);
    }
    setNumberOfGames(player.getNumberOfGames());
    if (player.getAvatar() != null) {
      setAvatarUrl(player.getAvatar().getUrl());
      setAvatarTooltip(player.getAvatar().getTooltip());
    }
  }
}
