package com.faforever.client.replay;

import com.faforever.client.domain.ReplayBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
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
public class LocalReplayVaultController extends VaultEntityController<ReplayBean> {

  private final ReplayService replayService;
  private final VaultPrefs vaultPrefs;

  private ReplayDetailController replayDetailController;


  public LocalReplayVaultController(ReplayService replayService, UiService uiService,
                                    NotificationService notificationService, I18n i18n,
                                    ReportingService reportingService, VaultPrefs vaultPrefs,
                                    FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, notificationService, i18n, reportingService, vaultPrefs, fxApplicationThreadExecutor);
    this.replayService = replayService;
    this.vaultPrefs = vaultPrefs;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    uploadButton.setVisible(false);

    backButton.visibleProperty().unbind();
    backButton.setVisible(false);
  }

  @Override
  protected void onDisplayDetails(ReplayBean replay) {
    JavaFxUtil.assertApplicationThread();
    replayDetailController.setReplay(replay);
    replayDetailController.getRoot().setVisible(true);
    replayDetailController.getRoot().requestFocus();
  }

  @Override
  protected void loadShowRooms() {
    onPageChange(null, true);
  }

  @Override
  protected void enterResultState() {
    state.set(State.RESULT);
  }

  @Override
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
    controller.setOnDeleteListener(() -> onPageChange(searchController.getLastSearchConfig(), false));
    return controller;
  }
  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    return List.of();
  }

  @Override
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
    replayDetailController.setOnDeleteListener(() -> onPageChange(searchController.getLastSearchConfig(), false));
    return replayDetailController.getRoot();
  }

  @Override
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
