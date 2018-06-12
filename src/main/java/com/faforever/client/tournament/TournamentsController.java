package com.faforever.client.tournament;


import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.io.CharStreams;
import javafx.scene.Node;
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

  public Pane tournamentRoot;
  public Pane tournamentListPane;
  public WebView tournamentDetailWebView;

  public TournamentsController(TimeService timeService, I18n i18n, TournamentService tournamentService, UiService uiService, WebViewConfigurer webViewConfigurer) {
    this.timeService = timeService;
    this.i18n = i18n;
    this.tournamentService = tournamentService;
    this.uiService = uiService;
    this.webViewConfigurer = webViewConfigurer;
  }

  @Override
  public Node getRoot() {
    return tournamentRoot;
  }

  @Override
  public void initialize() {
    super.initialize();
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    if (!tournamentListPane.getChildren().isEmpty()) {
      return;
    }
    tournamentDetailWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(tournamentDetailWebView);

    tournamentService.getAllTournaments()
        .thenAccept(tournaments -> {
          boolean firstItemSelected = false;

          tournaments.sort((o1, o2) -> -o1.getCreatedAt().compareTo(o2.getCreatedAt()));

          for (TournamentBean tournamentBean : tournaments) {
            TournamentListItemController tournamentListItemController = createAndAddTournamentItem(tournamentBean);

            if (!firstItemSelected) {
              tournamentListItemController.onMouseClicked();
              firstItemSelected = true;
            }
          }
        });
  }

  private TournamentListItemController createAndAddTournamentItem(TournamentBean tournamentBean) {
    TournamentListItemController tournamentListItemController = uiService.loadFxml("theme/tournaments/tournament_list_item.fxml");
    tournamentListItemController.setTournamentBean(tournamentBean);
    tournamentListItemController.setOnItemSelectedListener((item) -> {
      tournamentListPane.getChildren().forEach(node -> node.pseudoClassStateChanged(TournamentListItemController.SELECTED_PSEUDO_CLASS, false));
      displayTournamentItem(item);
      tournamentListItemController.getRoot().pseudoClassStateChanged(TournamentListItemController.SELECTED_PSEUDO_CLASS, true);
    });

    tournamentListPane.getChildren().add(tournamentListItemController.getRoot());
    return tournamentListItemController;
  }

  @SneakyThrows
  private void displayTournamentItem(TournamentBean tournamentBean) {
    String startingDate = i18n.get("tournament.noStartingDate");
    if (tournamentBean.getStartingAt() != null) {
      startingDate = MessageFormat.format("{0} {1}", timeService.asDate(tournamentBean.getStartingAt()), timeService.asShortTime(tournamentBean.getStartingAt()));
    }

    String completedDate = i18n.get("tournament.noCompletionDate");
    if (tournamentBean.getCompletedAt() != null) {
      completedDate = MessageFormat.format("{0} {1}", timeService.asDate(tournamentBean.getCompletedAt()), timeService.asShortTime(tournamentBean.getCompletedAt()));
    }

    Reader reader = new InputStreamReader(TOURNAMENT_DETAIL_HTML_RESOURCE.getInputStream());
    String html = CharStreams.toString(reader).replace("{name}", tournamentBean.getName())
        .replace("{challonge-url}", tournamentBean.getChallongeUrl())
        .replace("{tournament-type}", tournamentBean.getTournamentType())
        .replace("{starting-date}", startingDate)
        .replace("{completed-date}", completedDate)
        .replace("{description}", tournamentBean.getDescription())
        .replace("{tournament-image}", tournamentBean.getLiveImageUrl());

    tournamentDetailWebView.getEngine().loadContent(html);
  }
}
