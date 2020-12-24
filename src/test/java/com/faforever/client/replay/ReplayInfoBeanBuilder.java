package com.faforever.client.replay;

import com.faforever.client.api.dto.GameReviewsSummary;
import com.faforever.client.api.dto.Validity;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.vault.review.Review;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.List;

public class ReplayInfoBeanBuilder {

  private final Replay replay;

  private ReplayInfoBeanBuilder() {
    replay = new Replay();
  }

  public static ReplayInfoBeanBuilder create() {
    return new ReplayInfoBeanBuilder();
  }

  public ReplayInfoBeanBuilder defaultValues() {
    id(1).title("test")
        .startTime(LocalDateTime.MIN)
        .endTime(LocalDateTime.MAX)
        .validity(Validity.VALID)
        .featuredMod(new FeaturedMod())
        .reviews(FXCollections.emptyObservableList());
    return this;
  }

  public ReplayInfoBeanBuilder id(int id) {
    replay.setId(id);
    return this;
  }

  public ReplayInfoBeanBuilder title(String title) {
    replay.setTitle(title);
    return this;
  }

  public ReplayInfoBeanBuilder teams(ObservableMap<String, List<String>> teams) {
    replay.setTeams(teams);
    return this;
  }

  public ReplayInfoBeanBuilder teamPlayerStats(ObservableMap<String, List<Replay.PlayerStats>> teamPlayerStats) {
    replay.setTeamPlayerStats(teamPlayerStats);
    return this;
  }

  public ReplayInfoBeanBuilder startTime(Temporal startTime) {
    replay.setStartTime(startTime);
    return this;
  }

  public ReplayInfoBeanBuilder endTime(Temporal endTime) {
    replay.setEndTime(endTime);
    return this;
  }

  public ReplayInfoBeanBuilder featuredMod(FeaturedMod featuredMod) {
    replay.setFeaturedMod(featuredMod);
    return this;
  }

  public ReplayInfoBeanBuilder map(MapBean map) {
    replay.setMap(map);
    return this;
  }

  public ReplayInfoBeanBuilder replayFile(Path replayFile) {
    replay.setReplayFile(replayFile);
    return this;
  }

  public ReplayInfoBeanBuilder replayTicks(int replayTicks) {
    replay.setReplayTicks(replayTicks);
    return this;
  }

  public ReplayInfoBeanBuilder views(int views) {
    replay.setViews(views);
    return this;
  }

  public ReplayInfoBeanBuilder chatMessages(ObservableList<Replay.ChatMessage> chatMessages) {
    replay.setChatMessages(chatMessages);
    return this;
  }

  public ReplayInfoBeanBuilder gameOptions(ObservableList<Replay.GameOption> gameOptions) {
    replay.setGameOptions(gameOptions);
    return this;
  }

  public ReplayInfoBeanBuilder reviews(List<Review> reviews) {
    replay.getReviews().setAll(reviews);
    return this;
  }

  public ReplayInfoBeanBuilder validity(Validity validity) {
    replay.setValidity(validity);
    return this;
  }

  public ReplayInfoBeanBuilder reviewsSummary(GameReviewsSummary reviewsSummary) {
    replay.setReviewsSummary(reviewsSummary);
    return this;
  }

  public Replay get() {
    return replay;
  }
}
