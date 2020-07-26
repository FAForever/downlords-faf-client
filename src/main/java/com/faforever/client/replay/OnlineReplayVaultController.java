package com.faforever.client.replay;

import com.faforever.client.api.dto.Game;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import javafx.application.Platform;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OnlineReplayVaultController extends VaultEntityController<Replay> {

  private static final int TOP_ELEMENT_COUNT = 6;

  private final ReplayService replayService;

  private int playerId;
  private ReplayDetailController replayDetailController;

  public OnlineReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n, PreferencesService preferencesService, ReportingService reportingService) {
    super(uiService, notificationService, i18n, preferencesService, reportingService);
    this.replayService = replayService;
  }

  @Override
  public void initialize() {
    super.initialize();
    uploadButton.setVisible(false);

    searchController.setRootType(Game.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.GAME_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().onlineReplaySortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(true, true);
  }

  @Override
  protected void onDisplayDetails(Replay replay) {
    JavaFxUtil.assertApplicationThread();
    replayDetailController.setReplay(replay);
    replayDetailController.getRoot().setVisible(true);
    replayDetailController.getRoot().requestFocus();
  }

  protected void setSupplier(SearchConfig searchConfig, int page) {
    switch (searchType) {
      case SEARCH:
        currentSupplier = replayService.findByQueryWithPageCount(searchConfig.getSearchQuery(), pageSize, page, searchConfig.getSortConfig());
        break;
      case OWN:
        currentSupplier = replayService.getOwnReplaysWithPageCount(pageSize, page);
        break;
      case NEWEST:
        currentSupplier = replayService.getNewestReplaysWithPageCount(pageSize, page);
        break;
      case HIGHEST_RATED:
        currentSupplier = replayService.getHighestRatedReplaysWithPageCount(pageSize, page);
        break;
      case PLAYER:
        currentSupplier = replayService.getReplaysForPlayerWithPageCount(playerId, pageSize, page, new SortConfig("startTime", SortOrder.DESC));
        break;
    }
  }

  protected Node getEntityCard(Replay replay) {
    ReplayCardController controller = uiService.loadFxml("theme/vault/replay/replay_card.fxml");
    controller.setReplay(replay);
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller.getRoot();
  }

  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    return Arrays.asList(
        new ShowRoomCategory(() -> replayService.getNewestReplaysWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.NEWEST, "vault.replays.newest"),
        new ShowRoomCategory(() -> replayService.getOwnReplaysWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.OWN, "vault.replays.ownReplays"),
        new ShowRoomCategory(() -> replayService.getHighestRatedReplaysWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.HIGHEST_RATED, "vault.replays.highestRated")
    );
  }

  public void onUploadButtonClicked() {
    //do nothing
  }

  @Override
  protected Node getDetailView() {
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    return replayDetailController.getRoot();
  }

  @Override
  protected Class<? extends NavigateEvent> getDefaultNavigateEvent() {
    return OpenOnlineReplayVaultEvent.class;
  }

  @Override
  protected void handleSpecialNavigateEvent(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof ShowReplayEvent) {
      onShowReplayEvent((ShowReplayEvent) navigateEvent);
    } else if (navigateEvent instanceof ShowUserReplaysEvent) {
      onShowUserReplaysEvent((ShowUserReplaysEvent) navigateEvent);
    } else {
      log.warn("No such NavigateEvent for this Controller: {}", navigateEvent.getClass());
    }
  }

  private void onShowReplayEvent(ShowReplayEvent event) {
    int replayId = event.getReplayId();
    replayService.findById(replayId).thenAccept(replay -> {
      if (replay.isPresent()) {
        Platform.runLater(() -> onDisplayDetails(replay.get()));
      } else {
        notificationService.addNotification(new ImmediateNotification(i18n.get("replay.notFoundTitle"), i18n.get("replay.replayNotFoundText", replayId), Severity.WARN));
      }
    });
  }

  private void onShowUserReplaysEvent(ShowUserReplaysEvent event) {
    enterSearchingState();
    searchType = SearchType.PLAYER;
    playerId = event.getPlayerId();
    SortConfig sortConfig = new SortConfig("startTime", SortOrder.DESC);
    displayFromSupplier(() -> replayService.getReplaysForPlayerWithPageCount(playerId, pageSize, 1, sortConfig), true);
  }
}
