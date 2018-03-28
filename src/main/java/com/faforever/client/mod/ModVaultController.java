package com.faforever.client.mod;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WindowController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.mod.event.ModUploadedEvent;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchableProperties;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ModVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 7;
  private static final int LOAD_MORE_COUNT = 100;
  private static final int MAX_SEARCH_RESULTS = 100;
  /**
   * How many mod cards should be badged into one UI thread runnable.
   */
  private static final int BATCH_SIZE = 10;

  private final ModService modService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final NotificationService notificationService;

  public Pane searchResultGroup;
  public Pane searchResultPane;
  public Pane showroomGroup;
  public Node loadingLabel;
  public Pane highestRatedUiPane;
  public Pane newestPane;
  public Pane highestRatedPane;
  public Pane modVaultRoot;
  public ScrollPane scrollPane;
  public Button backButton;
  public SearchController searchController;
  public Button moreButton;

  private boolean initialized;
  private ModDetailController modDetailController;
  private ModVaultController.State state;
  private int currentPage;
  private Supplier<CompletableFuture<List<ModVersion>>> currentSupplier;

  @Inject
  public ModVaultController(ModService modService, I18n i18n, EventBus eventBus, PreferencesService preferencesService,
                            UiService uiService, NotificationService notificationService) {
    this.modService = modService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.notificationService = notificationService;
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);

    loadingLabel.managedProperty().bind(loadingLabel.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    backButton.managedProperty().bind(backButton.visibleProperty());
    moreButton.managedProperty().bind(moreButton.visibleProperty());

    modDetailController = uiService.loadFxml("theme/vault/mod/mod_detail.fxml");
    Node modDetailRoot = modDetailController.getRoot();
    modVaultRoot.getChildren().add(modDetailRoot);
    AnchorPane.setTopAnchor(modDetailRoot, 0d);
    AnchorPane.setRightAnchor(modDetailRoot, 0d);
    AnchorPane.setBottomAnchor(modDetailRoot, 0d);
    AnchorPane.setLeftAnchor(modDetailRoot, 0d);
    modDetailRoot.setVisible(false);

    eventBus.register(this);

    searchController.setRootType(com.faforever.client.api.dto.Mod.class);
    searchController.setSearchListener(this::searchByQuery);
    searchController.setSearchableProperties(SearchableProperties.MOD_PROPERTIES);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().modVaultConfigProperty());
  }

  private void searchByQuery(SearchConfig searchConfig) {
    SearchConfig newSearchConfig = new SearchConfig(searchConfig.getSortConfig(), searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"");
    currentPage = 0;
    enterLoadingState();
    displayModsFromSupplier(() -> modService.findByQuery(newSearchConfig, ++currentPage, MAX_SEARCH_RESULTS));
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    if (initialized) {
      return;
    }
    initialized = true;

    displayShowroomMods();
  }

  private void displayShowroomMods() {
    enterLoadingState();
    modService.getNewestMods(TOP_ELEMENT_COUNT, 1).thenAccept(mods -> replaceSearchResult(mods, newestPane))
        .thenCompose(aVoid -> modService.getHighestRatedMods(TOP_ELEMENT_COUNT, 1)).thenAccept(mods -> replaceSearchResult(mods, highestRatedPane))
        .thenCompose(aVoid -> modService.getHighestRatedUiMods(TOP_ELEMENT_COUNT, 1)).thenAccept(mods -> replaceSearchResult(mods, highestRatedUiPane))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
  }

  private void replaceSearchResult(List<ModVersion> modVersions, Pane pane) {
    Platform.runLater(() -> pane.getChildren().clear());
    appendSearchResult(modVersions, pane);
  }

  private void enterLoadingState() {
    state = ModVaultController.State.LOADING;
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingLabel.setVisible(true);
    backButton.setVisible(true);
    moreButton.setVisible(false);
  }

  private void enterSearchResultState() {
    state = ModVaultController.State.SEARCH_RESULT;
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingLabel.setVisible(false);
    backButton.setVisible(true);
    moreButton.setVisible(searchResultPane.getChildren().size() % MAX_SEARCH_RESULTS == 0);
  }

  private void enterShowroomState() {
    state = ModVaultController.State.SHOWROOM;
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingLabel.setVisible(false);
    backButton.setVisible(false);
    moreButton.setVisible(false);
  }

  @VisibleForTesting
  void onShowModDetail(ModVersion modVersion) {
    modDetailController.setModVersion(modVersion);
    modDetailController.getRoot().setVisible(true);
    modDetailController.getRoot().requestFocus();
  }

  public void onUploadModButtonClicked() {
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(preferencesService.getPreferences().getForgedAlliance().getModsDirectory().toFile());
      directoryChooser.setTitle(i18n.get("modVault.upload.chooseDirectory"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      openModUploadWindow(result.toPath());
    });
  }

  public Node getRoot() {
    return modVaultRoot;
  }

  private void openModUploadWindow(Path path) {
    ModUploadController modUploadController = uiService.loadFxml("theme/vault/mod/mod_upload.fxml");
    modUploadController.setModPath(path);

    Stage modUploadWindow = new Stage(StageStyle.TRANSPARENT);
    modUploadWindow.initModality(Modality.NONE);
    modUploadWindow.initOwner(getRoot().getScene().getWindow());

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(modUploadWindow, modUploadController.getRoot(), true, CLOSE);

    modUploadWindow.show();
  }

  public void onRefreshButtonClicked() {
    modService.evictCache();
    switch (state) {
      case SHOWROOM:
        displayShowroomMods();
        break;
      case SEARCH_RESULT:
        currentPage--;
        currentSupplier.get()
            .thenAccept(this::displayMods)
            .exceptionally(e -> {
              notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
                  i18n.get("vault.mods.searchError"), Severity.ERROR, e,
                  Collections.singletonList(new DismissAction(i18n))));
              enterShowroomState();
              return null;
            });
        break;
      default:
        // Do nothing
    }
  }

  public void onBackButtonClicked() {
    enterShowroomState();
  }

  @Subscribe
  public void onModUploaded(ModUploadedEvent event) {
    onRefreshButtonClicked();
  }

  public void showMoreHighestRatedUiMods() {
    enterLoadingState();
    displayModsFromSupplier(() -> modService.getHighestRatedUiMods(LOAD_MORE_COUNT, ++currentPage));
  }

  public void showMoreHighestRatedMods() {
    enterLoadingState();
    displayModsFromSupplier(() -> modService.getHighestRatedMods(LOAD_MORE_COUNT, ++currentPage));
  }

  public void showMoreNewestMods() {
    enterLoadingState();
    displayModsFromSupplier(() -> modService.getNewestMods(LOAD_MORE_COUNT, ++currentPage));
  }

  private void appendSearchResult(List<ModVersion> modVersions, Pane pane) {
    JavaFxUtil.assertBackgroundThread();

    ObservableList<Node> children = pane.getChildren();
    List<ModCardController> controllers = modVersions.parallelStream()
        .map(mod -> {
          ModCardController controller = uiService.loadFxml("theme/vault/mod/mod_card.fxml");
          controller.setModVersion(mod);
          controller.setOnOpenDetailListener(this::onShowModDetail);
          return controller;
        }).collect(Collectors.toList());

    Iterators.partition(controllers.iterator(), BATCH_SIZE).forEachRemaining(modCardControllers -> Platform.runLater(() -> {
      for (ModCardController modCardController : modCardControllers) {
        children.add(modCardController.getRoot());
      }
    }));
  }

  private void displayModsFromSupplier(Supplier<CompletableFuture<List<ModVersion>>> modsSupplier) {
    this.currentSupplier = modsSupplier;
    modsSupplier.get()
        .thenAccept(this::displayMods)
        .exceptionally(e -> {
          notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("vault.replays.searchError"), Severity.ERROR, e,
              Collections.singletonList(new DismissAction(i18n))));
          enterShowroomState();
          return null;
        });
  }

  private void displayMods(List<ModVersion> modVersions) {
    Platform.runLater(() -> searchResultPane.getChildren().clear());
    appendSearchResult(modVersions, searchResultPane);
    Platform.runLater(this::enterSearchResultState);
  }

  public void onLoadMoreButtonClicked() {
    moreButton.setVisible(false);
    loadingLabel.setVisible(true);

    currentSupplier.get()
        .thenAccept(mods -> {
          appendSearchResult(mods, searchResultPane);
          enterSearchResultState();
        })
        .exceptionally(e -> {
          notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("vault.mod.searchError"), Severity.ERROR, e,
              Collections.singletonList(new DismissAction(i18n))));
          enterShowroomState();
          return null;
        });
  }

  private enum State {
    LOADING, SHOWROOM, SEARCH_RESULT
  }
}
