package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.TimeService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.format.FormatStyle;
import java.util.List;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardController extends NodeController<StackPane> {

  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final TimeService timeService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public StackPane leaderboardRoot;
  public Pane connectionProgressPane;
  public Pane contentPane;
  public Label seasonLabel;
  public Label seasonDateLabel;
  public LeaderboardRankingsController leaderboardRankingsController;
  public LeaderboardPlayerDetailsController leaderboardPlayerDetailsController;
  public LeaderboardDistributionController leaderboardDistributionController;

  private final ObjectProperty<LeagueSeasonBean> leagueSeason = new SimpleObjectProperty<>();

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(contentPane, connectionProgressPane);
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());

    seasonLabel.textProperty()
               .bind(leagueSeason.map(seasonBean -> i18n.getOrDefault(seasonBean.nameKey(),
                                                                      "leagueLeaderboard.season.%s".formatted(
                                                                          seasonBean.nameKey()),
                                                                      seasonBean.seasonNumber()))
                                 .map(String::toUpperCase)
                                 .when(showing));

    seasonDateLabel.textProperty().bind(leagueSeason.map(seasonBean -> {
      String startDate = timeService.asDate(seasonBean.startDate(), FormatStyle.MEDIUM);
      String endDate = timeService.asDate(seasonBean.endDate(), FormatStyle.MEDIUM);
      return i18n.get("leaderboard.seasonDate", startDate, endDate);
    }).when(showing));

    leaderboardPlayerDetailsController.leagueSeasonProperty().bind(leagueSeason);

    leagueSeason.when(showing).subscribe(newSeason -> {
      if (newSeason == null) {
        leaderboardRankingsController.setLeagueEntries(List.of());
        leaderboardDistributionController.setLeagueEntries(List.of());
        leaderboardRankingsController.setSubdivisions(List.of());
        leaderboardDistributionController.setSubdivisions(List.of());
        leaderboardPlayerDetailsController.setLeagueEntry(null);
        return;
      }

      Mono<LeagueEntryBean> playerLeagueEntry = leaderboardService.getLeagueEntryForPlayer(
                                                                      playerService.getCurrentPlayer(), newSeason)
                                                                  .publishOn(fxApplicationThreadExecutor.asScheduler())
                                                                  .doOnNext(
                                                                      leaderboardPlayerDetailsController::setLeagueEntry)
                                                                  .switchIfEmpty(Mono.fromRunnable(
                                                                      () -> leaderboardPlayerDetailsController.setLeagueEntry(
                                                                          null)))
                                                                  .doOnError(throwable -> {
                                                                    log.error("Error while loading player league entry",
                                                                              throwable);
                                                                    notificationService.addImmediateErrorNotification(
                                                                        throwable, "leaderboard.failedToLoadEntry");
                                                                  });

      Mono<List<LeagueEntryBean>> activeEntries = leaderboardService.getActiveEntries(newSeason)
                                                                    .collectList()
                                                                    .publishOn(
                                                                        fxApplicationThreadExecutor.asScheduler())
                                                                    .doOnNext(leagueEntries -> {
                                                                      leaderboardRankingsController.setLeagueEntries(
                                                                          leagueEntries);
                                                                      leaderboardDistributionController.setLeagueEntries(
                                                                          leagueEntries);
                                                                    })
                                                                    .doOnError(throwable -> {
                                                                      log.error("Error while loading league entries",
                                                                                throwable);
                                                                      notificationService.addImmediateErrorNotification(
                                                                          throwable, "leaderboard.failedToLoadEntries");
                                                                    });

      Mono<List<SubdivisionBean>> subdivisions = leaderboardService.getAllSubdivisions(newSeason)
                                                                   .collectList()
                                                                   .publishOn(fxApplicationThreadExecutor.asScheduler())
                                                                   .doOnNext(subdivs -> {
                                                                     leaderboardRankingsController.setSubdivisions(
                                                                         subdivs);
                                                                     leaderboardDistributionController.setSubdivisions(
                                                                         subdivs);
                                                                   })
                                                                   .doOnError(throwable -> {
                                                                     log.error(
                                                                         "Error while loading league sub divisions",
                                                                         throwable);
                                                                     notificationService.addImmediateErrorNotification(
                                                                         throwable,
                                                                         "leaderboard.failedToLoadDivisions");
                                                                   });

      Mono.when(playerLeagueEntry, activeEntries, subdivisions).subscribe();
    });
  }

  public void setLeagueSeason(LeagueSeasonBean leagueSeason) {
    this.leagueSeason.set(leagueSeason);
  }

  @Override
  public StackPane getRoot() {
    return leaderboardRoot;
  }
}
