package com.faforever.client.domain;

import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Validity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Value
@NoArgsConstructor
public class ReplayBean {

  @EqualsAndHashCode.Include
  @ToString.Include
  IntegerProperty id = new SimpleIntegerProperty();
  @ToString.Include
  StringProperty title = new SimpleStringProperty();
  BooleanProperty replayAvailable = new SimpleBooleanProperty();
  ReadOnlyMapWrapper<String, List<String>> teams = new ReadOnlyMapWrapper<>(FXCollections.emptyObservableMap());
  ReadOnlyMapWrapper<String, List<GamePlayerStatsBean>> teamPlayerStats = new ReadOnlyMapWrapper<>(FXCollections.emptyObservableMap());
  ObjectProperty<PlayerBean> host = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> startTime = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> endTime = new SimpleObjectProperty<>();
  ObjectProperty<FeaturedModBean> featuredMod = new SimpleObjectProperty<>();
  ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();
  ObjectProperty<Path> replayFile = new SimpleObjectProperty<>();
  ObjectProperty<Integer> replayTicks = new SimpleObjectProperty<>();
  IntegerProperty views = new SimpleIntegerProperty();
  ReadOnlyListWrapper<ChatMessage> chatMessages = new ReadOnlyListWrapper<>(FXCollections.emptyObservableList());
  ReadOnlyListWrapper<GameOption> gameOptions = new ReadOnlyListWrapper<>(FXCollections.emptyObservableList());
  ReadOnlyListWrapper<ReplayReviewBean> reviews = new ReadOnlyListWrapper<>(FXCollections.emptyObservableList());
  ObjectProperty<Validity> validity = new SimpleObjectProperty<>();
  ObjectProperty<ReplayReviewsSummaryBean> gameReviewsSummary = new SimpleObjectProperty<>();
  ObservableValue<Integer> numPlayers = teams.map(team -> team.values().stream().mapToInt(Collection::size).sum())
      .orElse(0);
  ObservableValue<Double> averageRating = teamPlayerStats.map(playerStats -> playerStats.values()
      .stream()
      .flatMap(Collection::stream)
      .map(stats -> stats.getLeaderboardRatingJournals().stream().findFirst())
      .flatMap(Optional::stream)
      .mapToInt(ratingJournal -> RatingUtil.getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore()))
      .average()
      .stream()
      .boxed()
      .findFirst()
      .orElse(null));

  public static String getReplayUrl(int replayId, String baseUrlFormat) {
    return String.format(baseUrlFormat, replayId);
  }

  public boolean getReplayAvailable() {
    return replayAvailable.get();
  }

  public void setReplayAvailable(boolean replayAvailable) {
    this.replayAvailable.set(replayAvailable);
  }

  public BooleanProperty replayAvailableProperty() {
    return replayAvailable;
  }

  public Validity getValidity() {
    return validity.get();
  }

  public void setValidity(Validity validity) {
    this.validity.set(validity);
  }

  public ObjectProperty<Validity> validityProperty() {
    return validity;
  }

  public Path getReplayFile() {
    return replayFile.get();
  }

  public void setReplayFile(Path replayFile) {
    this.replayFile.set(replayFile);
  }

  public ObjectProperty<Path> replayFileProperty() {
    return replayFile;
  }

  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams;
  }

  public void setTeams(Map<String, List<String>> teams) {
    this.teams.set(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(teams)));
  }

  public ReadOnlyMapProperty<String, List<String>> teamsProperty() {
    return teams.getReadOnlyProperty();
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

  public OffsetDateTime getStartTime() {
    return startTime.get();
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime.set(startTime);
  }

  public ObjectProperty<OffsetDateTime> startTimeProperty() {
    return startTime;
  }

  @Nullable
  public OffsetDateTime getEndTime() {
    return endTime.get();
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime.set(endTime);
  }

  public ObjectProperty<OffsetDateTime> endTimeProperty() {
    return endTime;
  }

  @Nullable
  public Integer getReplayTicks() {
    return replayTicks.get();
  }

  public void setReplayTicks(Integer replayTicks) {
    this.replayTicks.set(replayTicks);
  }

  public ObjectProperty<Integer> replayTicksProperty() {
    return replayTicks;
  }

  public FeaturedModBean getFeaturedMod() {
    return featuredMod.get();
  }

  public void setFeaturedMod(FeaturedModBean featuredMod) {
    this.featuredMod.set(featuredMod);
  }

  public ObjectProperty<FeaturedModBean> featuredModProperty() {
    return featuredMod;
  }

  @Nullable
  public MapVersionBean getMapVersion() {
    return mapVersion.get();
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  public ObjectProperty<MapVersionBean> mapVersionProperty() {
    return mapVersion;
  }

  public int getViews() {
    return views.get();
  }

  public void setViews(int views) {
    this.views.set(views);
  }

  public IntegerProperty viewsProperty() {
    return views;
  }

  public ObservableList<ChatMessage> getChatMessages() {
    return chatMessages;
  }

  public void setChatMessages(List<ChatMessage> chatMessages) {
    this.chatMessages.set(FXCollections.unmodifiableObservableList(FXCollections.observableList(chatMessages)));
  }

  public ReadOnlyListProperty<ChatMessage> chatMessagesProperty() {
    return chatMessages.getReadOnlyProperty();
  }

  public ObservableList<GameOption> getGameOptions() {
    return gameOptions;
  }

  public void setGameOptions(List<GameOption> gameOptions) {
    this.gameOptions.set(FXCollections.unmodifiableObservableList(FXCollections.observableList(gameOptions)));
  }

  public ReadOnlyListProperty<GameOption> gameOptionsProperty() {
    return gameOptions.getReadOnlyProperty();
  }

  public ObservableMap<String, List<GamePlayerStatsBean>> getTeamPlayerStats() {
    return teamPlayerStats;
  }

  public void setTeamPlayerStats(Map<String, List<GamePlayerStatsBean>> teamPlayerStats) {
    this.teamPlayerStats.set(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(teamPlayerStats)));
  }

  public ReadOnlyMapProperty<String, List<GamePlayerStatsBean>> teamPlayerStatsProperty() {
    return teamPlayerStats.getReadOnlyProperty();
  }

  public ObservableList<ReplayReviewBean> getReviews() {
    return reviews;
  }

  public void setReviews(List<ReplayReviewBean> reviews) {
    if (reviews == null) {
      this.reviews.set(FXCollections.emptyObservableList());
    } else {
      this.reviews.set(FXCollections.unmodifiableObservableList(FXCollections.observableList(reviews)));
    }
  }

  public ReadOnlyListProperty<ReplayReviewBean> reviewsProperty() {
    return reviews.getReadOnlyProperty();
  }

  public ObjectProperty<ReplayReviewsSummaryBean> gameReviewsSummaryProperty() {
    return gameReviewsSummary;
  }

  public ReplayReviewsSummaryBean getGameReviewsSummary() {
    return gameReviewsSummary.get();
  }

  public void setGameReviewsSummary(ReplayReviewsSummaryBean gameReviewsSummary) {
    this.gameReviewsSummary.set(gameReviewsSummary);
  }

  public boolean isReplayAvailable() {
    return replayAvailable.get();
  }

  public PlayerBean getHost() {
    return host.get();
  }

  public ObjectProperty<PlayerBean> hostProperty() {
    return host;
  }

  public void setHost(PlayerBean host) {
    this.host.set(host);
  }

  public Integer getNumPlayers() {
    return numPlayers.getValue();
  }

  public ObservableValue<Integer> numPlayersProperty() {
    return numPlayers;
  }

  public Double getAverageRating() {
    return averageRating.getValue();
  }

  public ObservableValue<Double> averageRatingProperty() {
    return averageRating;
  }

  @Value
  public static class ChatMessage {
    ObjectProperty<Duration> time = new SimpleObjectProperty<>();
    StringProperty sender = new SimpleStringProperty();
    StringProperty message = new SimpleStringProperty();

    public ChatMessage(Duration time, String sender, String message) {
      setTime(time);
      setSender(sender);
      setMessage(message);
    }

    public Duration getTime() {
      return time.get();
    }

    public void setTime(Duration time) {
      this.time.set(time);
    }

    public ObjectProperty<Duration> timeProperty() {
      return time;
    }

    public String getSender() {
      return sender.get();
    }

    public void setSender(String sender) {
      this.sender.set(sender);
    }

    public StringProperty senderProperty() {
      return sender;
    }

    public String getMessage() {
      return message.get();
    }

    public void setMessage(String message) {
      this.message.set(message);
    }

    public StringProperty messageProperty() {
      return message;
    }
  }

  @Value
  public static class GameOption {
    StringProperty key = new SimpleStringProperty();
    StringProperty value = new SimpleStringProperty();

    public GameOption(String key, Object value) {
      setKey(key);
      setValue(String.valueOf(value));
    }

    public String getKey() {
      return key.get();
    }

    public void setKey(String key) {
      this.key.set(key);
    }

    public StringProperty keyProperty() {
      return key;
    }

    public String getValue() {
      return value.get();
    }

    public void setValue(String value) {
      this.value.set(value);
    }

    public StringProperty valueProperty() {
      return value;
    }
  }
}
