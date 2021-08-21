package com.faforever.client.builders;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayBean.ChatMessage;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.commons.api.dto.Validity;
import javafx.collections.FXCollections;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


public class ReplayBeanBuilder {
  public static ReplayBeanBuilder create() {
    return new ReplayBeanBuilder();
  }

  private final ReplayBean replayBean = new ReplayBean();

  public ReplayBeanBuilder defaultValues() {
    id(1);
    title("test");
    replayAvailable(true);
    startTime(OffsetDateTime.now().minusHours(1));
    endTime(OffsetDateTime.now());
    validity(Validity.VALID);
    featuredMod(new FeaturedModBean());
    teams(FXCollections.observableMap(Map.of("2", List.of("junit1"), "3", List.of("junit2"))));
    views(100);
    chatMessages(ReplayChatMessageListBuilder.create().defaultValues().get());
    teamPlayerStats(PlayerStatsMapBuilder.create().defaultValues().get());
    gameOptions(GameOptionListBuilder.create().defaultValues().get());
    return this;
  }

  public ReplayBeanBuilder id(int id) {
    replayBean.setId(id);
    return this;
  }

  public ReplayBeanBuilder title(String title) {
    replayBean.setTitle(title);
    return this;
  }

  public ReplayBeanBuilder replayAvailable(boolean replayAvailable) {
    replayBean.setReplayAvailable(replayAvailable);
    return this;
  }

  public ReplayBeanBuilder teams(Map<String, List<String>> teams) {
    replayBean.setTeams(teams);
    return this;
  }

  public ReplayBeanBuilder host(PlayerBean host) {
    replayBean.setHost(host);
    return this;
  }

  public ReplayBeanBuilder startTime(OffsetDateTime startTime) {
    replayBean.setStartTime(startTime);
    return this;
  }

  public ReplayBeanBuilder endTime(OffsetDateTime endTime) {
    replayBean.setEndTime(endTime);
    return this;
  }

  public ReplayBeanBuilder featuredMod(FeaturedModBean featuredMod) {
    replayBean.setFeaturedMod(featuredMod);
    return this;
  }

  public ReplayBeanBuilder mapVersion(MapVersionBean mapVersion) {
    replayBean.setMapVersion(mapVersion);
    return this;
  }

  public ReplayBeanBuilder replayFile(Path replayFile) {
    replayBean.setReplayFile(replayFile);
    return this;
  }

  public ReplayBeanBuilder replayTicks(Integer replayTicks) {
    replayBean.setReplayTicks(replayTicks);
    return this;
  }

  public ReplayBeanBuilder views(int views) {
    replayBean.setViews(views);
    return this;
  }

  public ReplayBeanBuilder reviews(List<ReplayReviewBean> reviews) {
    replayBean.setReviews(reviews);
    return this;
  }

  public ReplayBeanBuilder validity(Validity validity) {
    replayBean.setValidity(validity);
    return this;
  }

  public ReplayBeanBuilder gameReviewsSummary(ReplayReviewsSummaryBean gameReviewsSummary) {
    replayBean.setGameReviewsSummary(gameReviewsSummary);
    return this;
  }

  public ReplayBeanBuilder chatMessages(List<ChatMessage> chatMessages) {
    replayBean.setChatMessages(chatMessages);
    return this;
  }

  public ReplayBeanBuilder gameOptions(List<ReplayBean.GameOption> gameOptions) {
    replayBean.setGameOptions(gameOptions);
    return this;
  }

  public ReplayBeanBuilder teamPlayerStats(Map<String, List<GamePlayerStatsBean>> teamPlayerStats) {
    replayBean.setTeamPlayerStats(teamPlayerStats);
    return this;
  }

  public ReplayBean get() {
    return replayBean;
  }

    abstract class SubReplayBeanBuilder {
    public ReplayBeanBuilder then() {
      return ReplayBeanBuilder.this;
    }
  }

}

