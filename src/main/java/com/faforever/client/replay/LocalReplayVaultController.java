package com.faforever.client.replay;

import com.faforever.client.api.dto.Game;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocalReplayVaultController extends VaultEntityController<Replay> {

  private final ReplayService replayService;

  private ReplayDetailController replayDetailController;

  public LocalReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n, PreferencesService preferencesService, ReportingService reportingService) {
    super(uiService, notificationService, i18n, preferencesService, reportingService);
    this.replayService = replayService;
  }

  @Override
  public void initialize() {
    super.initialize();
    uploadButton.setVisible(false);
    backButton.setVisible(false);
    searchBox.setVisible(false);
    searchSeparator.setVisible(false);

  }

  @Override
  protected void onDisplayDetails(Replay replay) {
    JavaFxUtil.assertApplicationThread();
    replayDetailController.setReplay(replay);
    replayDetailController.getRoot().setVisible(true);
    replayDetailController.getRoot().requestFocus();
  }

  @Override
  protected void loadShowRoom() {
    onPageChange(null, true);
  }

  @Override
  protected void enterResultState() {
    state.set(State.RESULT);

    showRoomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
    backButton.setVisible(false);
    paginationGroup.setVisible(true);
  }

  protected void setSupplier(SearchConfig searchConfig) {
    try {
      currentSupplier = replayService.loadLocalReplayPage(pageSize, pagination.getCurrentPageIndex() + 1);
    } catch (IOException e) {
      log.warn("Could not load the local replays", e);
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
    return null;
  }

  public void onUploadButtonClicked() {
    //do nothing
  }

  @Override
  protected Node getDetailView() {
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    return replayDetailController.getRoot();
  }

  protected void initSearchController() {
    searchController.setRootType(Game.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.GAME_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().onlineReplaySortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(true);
  }

  @Override
  protected Class<? extends NavigateEvent> getDefaultNavigateEvent() {
    return OpenLocalReplayVaultEvent.class;
  }

  @Override
  protected void handleSpecialNavigateEvent(NavigateEvent navigateEvent) {
    log.warn("No such NavigateEvent for this Controller: {}", navigateEvent.getClass());
  }
}
