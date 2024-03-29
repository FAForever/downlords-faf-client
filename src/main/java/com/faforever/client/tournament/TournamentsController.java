package com.faforever.client.tournament;


import com.faforever.client.domain.api.Tournament;
import com.faforever.client.domain.api.Tournament.Status;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.io.CharStreams;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Comparator;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class TournamentsController extends NodeController<Node> {
  private static final ClassPathResource TOURNAMENT_DETAIL_HTML_RESOURCE = new ClassPathResource("/theme/tournaments/tournament_detail.html");

  private final TimeService timeService;
  private final I18n i18n;
  private final TournamentService tournamentService;
  private final UiService uiService;
  private final WebViewConfigurer webViewConfigurer;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Pane tournamentRoot;
  public WebView tournamentDetailWebView;
  public Pane loadingIndicator;
  public Node contentPane;
  public ListView<Tournament> tournamentListView;

  @Override
  public Node getRoot() {
    return tournamentRoot;
  }

  @Override
  protected void onInitialize() {
    contentPane.managedProperty().bind(contentPane.visibleProperty());
    contentPane.setVisible(false);

    tournamentListView.setCellFactory(param -> new TournamentItemListCell(uiService));
    tournamentListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> displayTournamentItem(newValue));
  }

  private void onLoadingStart() {
    fxApplicationThreadExecutor.execute(() -> loadingIndicator.setVisible(true));
  }

  private void onLoadingStop() {
    fxApplicationThreadExecutor.execute(() -> {
      tournamentRoot.getChildren().remove(loadingIndicator);
      loadingIndicator = null;
      contentPane.setVisible(true);
    });
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    if (contentPane.isVisible()) {
      return;
    }
    onLoadingStart();

    tournamentDetailWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(tournamentDetailWebView);

    tournamentService.getAllTournaments()
                     .sort(Comparator.<Tournament, Status>comparing(Tournament::status)
                                     .thenComparing(Tournament::createdAt)
                                     .reversed())
                     .collectList()
                     .map(FXCollections::observableList)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .subscribe(tournaments -> {
                       tournamentListView.getItems().setAll(tournaments);
                       tournamentListView.getSelectionModel().selectFirst();
                       onLoadingStop();
                     }, throwable -> log.error("Tournaments could not be loaded", throwable));
  }

  private void displayTournamentItem(Tournament tournament) {
    String startingDate = i18n.get("tournament.noStartingDate");
    if (tournament.startingAt() != null) {
      startingDate = MessageFormat.format(i18n.get("dateWithTime"), timeService.asDate(tournament.startingAt()),
                                          timeService.asShortTime(tournament.startingAt()));
    }

    String completedDate = i18n.get("tournament.noCompletionDate");
    if (tournament.completedAt() != null) {
      completedDate = MessageFormat.format(i18n.get("dateWithTime"), timeService.asDate(tournament.completedAt()),
                                           timeService.asShortTime(tournament.completedAt()));
    }

    try (Reader reader = new InputStreamReader(TOURNAMENT_DETAIL_HTML_RESOURCE.getInputStream())) {
      String html = CharStreams.toString(reader)
                               .replace("{name}", tournament.name())
                               .replace("{challonge-url}", tournament.challongeUrl())
                               .replace("{tournament-type}", tournament.tournamentType())
          .replace("{starting-date}", startingDate)
          .replace("{completed-date}", completedDate)
                               .replace("{description}", tournament.description())
                               .replace("{tournament-image}", tournament.liveImageUrl())
          .replace("{open-on-challonge-label}", i18n.get("tournament.openOnChallonge"))
          .replace("{game-type-label}", i18n.get("tournament.gameType"))
          .replace("{starting-at-label}", i18n.get("tournament.startingAt"))
          .replace("{completed-at-label}", i18n.get("tournament.completedAt"))
          .replace("{loading-label}", i18n.get("loading"));

      tournamentDetailWebView.getEngine().loadContent(html);
    } catch (IOException e) {
      throw new AssetLoadException("Tournament view could not be loaded", e, "tournament.viewNotLoaded");
    }
  }
}
