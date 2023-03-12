package com.faforever.client.replay;

import com.faforever.client.domain.ReplayBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.DeleteLocalReplayEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
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
  private final VaultPrefs vaultPrefs;

  private ReplayDetailController replayDetailController;


  public LocalReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n, EventBus eventBus, ReportingService reportingService, VaultPrefs vaultPrefs) {
    super(uiService, notificationService, i18n, reportingService, vaultPrefs);
    this.replayService = replayService;
    this.eventBus = eventBus;
    this.vaultPrefs = vaultPrefs;
  }

  @Override
  public void initialize() {
    super.initialize();
    uploadButton.setVisible(false);

    backButton.visibleProperty().unbind();
    backButton.setVisible(false);
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
  public void onLocalReplayDeleted(DeleteLocalReplayEvent event) throws ExecutionException, InterruptedException {
    if (replayService.deleteReplayFile(event.replayFile())) {
      onPageChange(searchController.getLastSearchConfig(), false);
    }
  }

  @Override
  protected void loadShowRooms() {
    onPageChange(null, true);
  }

  @Override
  protected void enterResultState() {
    state.set(State.RESULT);
  }

  protected void setSupplier(SearchConfig searchConfig) {
    try {
      currentSupplier = replayService.loadLocalReplayPage(pageSize, pagination.getCurrentPageIndex() + 1);
    } catch (IOException e) {
      log.error("Could not load local replays", e);
    }
  }

  @Override
  protected ReplayCardController createEntityCard() {
    ReplayCardController controller = uiService.loadFxml("theme/vault/replay/replay_card.fxml");
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller;
  }
  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    return List.of();
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
    searchController.setSortConfig(vaultPrefs.onlineReplaySortConfigProperty());
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
