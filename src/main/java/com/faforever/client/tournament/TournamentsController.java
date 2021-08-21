package com.faforever.client.tournament;


import com.faforever.client.domain.TournamentBean;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.io.CharStreams;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Comparator;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TournamentsController extends AbstractViewController<Node> {
  private static final ClassPathResource TOURNAMENT_DETAIL_HTML_RESOURCE = new ClassPathResource("/theme/tournaments/tournament_detail.html");

  private final TimeService timeService;
  private final I18n i18n;
  private final TournamentService tournamentService;
  private final UiService uiService;
  private final WebViewConfigurer webViewConfigurer;
  private final PreferencesService preferencesService;

  public Pane tournamentRoot;
  public WebView tournamentDetailWebView;
  public Pane loadingIndicator;
  public Node contentPane;
  public ListView<TournamentBean> tournamentListView;

  public TournamentsController(TimeService timeService, I18n i18n, TournamentService tournamentService, UiService uiService, WebViewConfigurer webViewConfigurer, PreferencesService preferencesService) {
    this.timeService = timeService;
    this.i18n = i18n;
    this.tournamentService = tournamentService;
    this.uiService = uiService;
    this.webViewConfigurer = webViewConfigurer;
    this.preferencesService = preferencesService;
  }

  @Override
  public Node getRoot() {
    return tournamentRoot;
  }

  @Override
  public void initialize() {
    contentPane.managedProperty().bind(contentPane.visibleProperty());
    contentPane.setVisible(false);

    tournamentListView.setCellFactory(param -> new TournamentItemListCell(uiService));
    tournamentListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> displayTournamentItem(newValue));
  }

  private void onLoadingStart() {
    JavaFxUtil.runLater(() -> loadingIndicator.setVisible(true));
  }

  private void onLoadingStop() {
    JavaFxUtil.runLater(() -> {
      tournamentRoot.getChildren().remove(loadingIndicator);
      loadingIndicator = null;
      contentPane.setVisible(true);
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (contentPane.isVisible()) {
      return;
    }
    onLoadingStart();

    tournamentDetailWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(tournamentDetailWebView);

    tournamentService.getAllTournaments()
        .thenAccept(tournaments -> JavaFxUtil.runLater(() -> {
          tournaments.sort(
              Comparator.<TournamentBean, Integer>comparing(o -> o.getStatus().getSortOrderPriority())
                  .thenComparing(TournamentBean::getCreatedAt)
                  .reversed()
          );
          tournamentListView.getItems().setAll(tournaments);
          tournamentListView.getSelectionModel().selectFirst();
          onLoadingStop();
        })).exceptionally(throwable -> {
      log.warn("Tournaments could not be loaded", throwable);
      return null;
    });
  }

  @SneakyThrows
  private void displayTournamentItem(TournamentBean tournamentBean) {
    String startingDate = i18n.get("tournament.noStartingDate");
    if (tournamentBean.getStartingAt() != null) {
      startingDate = MessageFormat.format(i18n.get("dateWithTime"), timeService.asDate(tournamentBean.getStartingAt()), timeService.asShortTime(tournamentBean.getStartingAt()));
    }

    String completedDate = i18n.get("tournament.noCompletionDate");
    if (tournamentBean.getCompletedAt() != null) {
      completedDate = MessageFormat.format(i18n.get("dateWithTime"), timeService.asDate(tournamentBean.getCompletedAt()), timeService.asShortTime(tournamentBean.getCompletedAt()));
    }

    Reader reader = new InputStreamReader(TOURNAMENT_DETAIL_HTML_RESOURCE.getInputStream());
    String html = CharStreams.toString(reader).replace("{name}", tournamentBean.getName())
        .replace("{challonge-url}", tournamentBean.getChallongeUrl())
        .replace("{tournament-type}", tournamentBean.getTournamentType())
        .replace("{starting-date}", startingDate)
        .replace("{completed-date}", completedDate)
        .replace("{description}", tournamentBean.getDescription())
        .replace("{tournament-image}", tournamentBean.getLiveImageUrl())
        .replace("{open-on-challonge-label}", i18n.get("tournament.openOnChallonge"))
        .replace("{game-type-label}", i18n.get("tournament.gameType"))
        .replace("{starting-at-label}", i18n.get("tournament.startingAt"))
        .replace("{completed-at-label}", i18n.get("tournament.completedAt"))
        .replace("{loading-label}", i18n.get("loading"));

    tournamentDetailWebView.getEngine().loadContent(html);
  }
}
