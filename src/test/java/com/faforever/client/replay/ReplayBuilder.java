package com.faforever.client.replay;

import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewsSummary;
import com.faforever.commons.api.dto.Validity;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class ReplayBuilder {

  private final Replay replay;

  private ReplayBuilder() {
    replay = new Replay();
  }

  public static ReplayBuilder create() {
    return new ReplayBuilder();
  }

  public ReplayBuilder defaultValues() {
    id(1);
    title("test");
    replayAvailable(true);
    startTime(OffsetDateTime.now().minusHours(1));
    endTime(OffsetDateTime.now());
    validity(Validity.VALID);
    featuredMod(new FeaturedMod());
    teams(FXCollections.observableMap(Map.of("2", List.of("junit1"), "3", List.of("junit2"))));
    views(100);
    chatMessages(FXCollections.observableArrayList(ReplayChatMessageListBuilder.create().defaultValues().get()));
    teamPlayerStats(FXCollections.observableMap(PlayerStatsMapBuilder.create().defaultValues().get()));
    gameOptions(FXCollections.observableArrayList(GameOptionListBuilder.create().defaultValues().get()));
    return this;
  }

  public ReplayBuilder id(int id) {
    replay.setId(id);
    return this;
  }

  public ReplayBuilder title(String title) {
    replay.setTitle(title);
    return this;
  }

  public ReplayBuilder replayAvailable(boolean available) {
    replay.setReplayAvailable(available);
    return this;
  }

  public ReplayBuilder teams(ObservableMap<String, List<String>> teams) {
    replay.setTeams(teams);
    return this;
  }

  public ReplayBuilder teamPlayerStats(ObservableMap<String, List<Replay.PlayerStats>> teamPlayerStats) {
    replay.setTeamPlayerStats(teamPlayerStats);
    return this;
  }

  public ReplayBuilder startTime(OffsetDateTime startTime) {
    replay.setStartTime(startTime);
    return this;
  }

  public ReplayBuilder endTime(OffsetDateTime endTime) {
    replay.setEndTime(endTime);
    return this;
  }

  public ReplayBuilder featuredMod(FeaturedMod featuredMod) {
    replay.setFeaturedMod(featuredMod);
    return this;
  }

  public ReplayBuilder map(MapBean map) {
    replay.setMap(map);
    return this;
  }

  public ReplayBuilder replayFile(Path replayFile) {
    replay.setReplayFile(replayFile);
    return this;
  }

  public ReplayBuilder replayTicks(int replayTicks) {
    replay.setReplayTicks(replayTicks);
    return this;
  }

  public ReplayBuilder views(int views) {
    replay.setViews(views);
    return this;
  }

  public ReplayBuilder chatMessages(ObservableList<Replay.ChatMessage> chatMessages) {
    replay.setChatMessages(chatMessages);
    return this;
  }

  public ReplayBuilder gameOptions(ObservableList<Replay.GameOption> gameOptions) {
    replay.setGameOptions(gameOptions);
    return this;
  }

  public ReplayBuilder reviews(List<Review> reviews) {
    replay.getReviews().setAll(reviews);
    return this;
  }

  public ReplayBuilder validity(Validity validity) {
    replay.setValidity(validity);
    return this;
  }

  public ReplayBuilder reviewsSummary(ReviewsSummary reviewsSummary) {
    replay.setReviewsSummary(reviewsSummary);
    return this;
  }

  public Replay get() {
    return replay;
  }
}
