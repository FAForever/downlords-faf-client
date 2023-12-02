package com.faforever.client.domain;

import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Validity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
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
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReplayBean {

  @EqualsAndHashCode.Include
  @ToString.Include
  IntegerProperty id = new SimpleIntegerProperty();
  @ToString.Include
  StringProperty title = new SimpleStringProperty();
  BooleanProperty replayAvailable = new SimpleBooleanProperty();
  ReadOnlyObjectWrapper<Map<String, List<String>>> teams = new ReadOnlyObjectWrapper<>(Map.of());
  ReadOnlyObjectWrapper<Map<String, List<GamePlayerStatsBean>>> teamPlayerStats = new ReadOnlyObjectWrapper<>(Map.of());
  ObjectProperty<PlayerBean> host = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> startTime = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> endTime = new SimpleObjectProperty<>();
  ObjectProperty<FeaturedModBean> featuredMod = new SimpleObjectProperty<>();
  ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();
  ObjectProperty<Path> replayFile = new SimpleObjectProperty<>();
  ObjectProperty<Integer> replayTicks = new SimpleObjectProperty<>();
  IntegerProperty views = new SimpleIntegerProperty();
  ReadOnlyObjectWrapper<List<ChatMessage>> chatMessages = new ReadOnlyObjectWrapper<>(List.of());
  ReadOnlyObjectWrapper<List<GameOption>> gameOptions = new ReadOnlyObjectWrapper<>(List.of());
  ObjectProperty<Validity> validity = new SimpleObjectProperty<>();
  ObjectProperty<ReplayReviewsSummaryBean> gameReviewsSummary = new SimpleObjectProperty<>();
  BooleanProperty local = new SimpleBooleanProperty();
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

  public Map<String, List<String>> getTeams() {
    return teams.get();
  }

  public void setTeams(Map<String, List<String>> teams) {
    this.teams.set(teams == null ? Map.of() : Map.copyOf(teams));
  }

  public ReadOnlyObjectProperty<Map<String, List<String>>> teamsProperty() {
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

  public List<ChatMessage> getChatMessages() {
    return chatMessages.get();
  }

  public void setChatMessages(List<ChatMessage> chatMessages) {
    this.chatMessages.set(chatMessages == null ? List.of() : List.copyOf(chatMessages));
  }

  public ReadOnlyObjectProperty<List<ChatMessage>> chatMessagesProperty() {
    return chatMessages.getReadOnlyProperty();
  }

  public List<GameOption> getGameOptions() {
    return gameOptions.get();
  }

  public void setGameOptions(List<GameOption> gameOptions) {
    this.gameOptions.set(gameOptions == null ? List.of() : List.copyOf(gameOptions));
  }

  public ReadOnlyObjectProperty<List<GameOption>> gameOptionsProperty() {
    return gameOptions.getReadOnlyProperty();
  }

  public Map<String, List<GamePlayerStatsBean>> getTeamPlayerStats() {
    return teamPlayerStats.get();
  }

  public void setTeamPlayerStats(Map<String, List<GamePlayerStatsBean>> teamPlayerStats) {
    this.teamPlayerStats.set(teamPlayerStats == null ? Map.of() : Map.copyOf(teamPlayerStats));
  }

  public ReadOnlyObjectProperty<Map<String, List<GamePlayerStatsBean>>> teamPlayerStatsProperty() {
    return teamPlayerStats.getReadOnlyProperty();
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

  public boolean isLocal() {
    return local.get();
  }

  public BooleanProperty localProperty() {
    return local;
  }

  public void setLocal(boolean local) {
    this.local.set(local);
  }

  public record ChatMessage(Duration time, String sender, String message) {}

  public record GameOption(String key, String value) {}
}
