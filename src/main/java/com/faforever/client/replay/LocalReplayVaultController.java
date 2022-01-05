package com.faforever.client.replay;

import com.faforever.client.domain.ReplayBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.DeleteLocalReplayEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.commons.api.dto.Game;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocalReplayVaultController extends VaultEntityController<ReplayBean> {

  private final ReplayService replayService;
  private final EventBus eventBus;
  private ReplayDetailController replayDetailController;


  public LocalReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n, EventBus eventBus, PreferencesService preferencesService, ReportingService reportingService) {
    super(uiService, notificationService, i18n, preferencesService, reportingService);
    this.replayService = replayService;
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    super.initialize();
    uploadButton.setVisible(false);
    backButton.setVisible(false);
    searchBox.setVisible(false);
    searchSeparator.setVisible(false);
    this.eventBus.register(this);
  }

  @Override
  protected void onDisplayDetails(ReplayBean replay) {
    JavaFxUtil.assertApplicationThread();
    replayDetailController.setReplay(replay);
    replayDetailController.getRoot().setVisible(true);
    replayDetailController.getRoot().requestFocus();
  }

  @Subscribe
  private void onLocalReplayDeleted(DeleteLocalReplayEvent event) throws ExecutionException, InterruptedException {
    if (replayService.deleteReplayFile(event.getReplayFile())) {
      onPageChange(searchController.getLastSearchConfig(), false);
    }
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

  protected Node getEntityCard(ReplayBean replay) {
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
    // do nothing
  }

  @Override
  protected void onManageVaultButtonClicked() {
    // do nothing
  }

  @Override
  protected Node getDetailView() {
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    return replayDetailController.getRoot();
  }

  protected void initSearchController() {
    searchController.setRootType(Game.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.GAME_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVault().onlineReplaySortConfigProperty());
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
