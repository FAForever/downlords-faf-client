package com.faforever.client.mod;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WindowController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.event.ModUploadedEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.collect.Iterators;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 7;
  private static final int MAX_SUGGESTIONS = 10;
  private static final int LOAD_MORE_COUNT = 200;
  /**
   * How many mod cards should be badged into one UI thread runnable.
   */
  private static final int BATCH_SIZE = 10;
  private final ModService modService;
  private final I18n i18n;
  private final PreferencesService preferencesService;
  private final EventBus eventBus;
  private final UiService uiService;

  public Pane searchResultGroup;
  public Pane searchResultPane;
  public Pane showroomGroup;
  public Pane loadingPane;
  public TextField searchTextField;
  public Pane recommendedUiModsPane;
  public Pane newestModsPane;
  public Pane popularModsPane;
  public Pane mostLikedModsPane;
  public Pane modVaultRoot;
  private boolean initialized;

  @Inject
  public ModVaultController(ModService modService, I18n i18n, PreferencesService preferencesService, EventBus eventBus, UiService uiService) {
    this.modService = modService;
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    this.eventBus = eventBus;
    this.uiService = uiService;
  }

  @Override
  public void initialize() {
    super.initialize();
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());

    eventBus.register(this);
  }

  @Override
  public void onDisplay() {
    if (initialized) {
      return;
    }
    initialized = true;

    displayShowroomMods();

    JavaFxUtil.makeSuggestionField(searchTextField, this::createModSuggestions, aVoid -> onSearchModButtonClicked());
  }

  private void displayShowroomMods() {
    enterLoadingState();
    modService.getMostDownloadedMods(TOP_ELEMENT_COUNT)
        .thenAccept(modInfoBeans -> populateMods(modInfoBeans, popularModsPane))
        .thenCompose(aVoid -> modService.getMostLikedMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, mostLikedModsPane)))
        .thenCompose(aVoid -> modService.getNewestMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, newestModsPane)))
        .thenCompose(aVoid -> modService.getMostLikedUiMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, recommendedUiModsPane)))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
  }

  public void onSearchModButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }
    enterSearchResultState();
    modService.lookupMod(searchTextField.getText(), 100)
        .thenAccept(this::displaySearchResult);
  }

  private void populateMods(List<Mod> mods, Pane pane) {
    JavaFxUtil.assertBackgroundThread();

    ObservableList<Node> children = pane.getChildren();
    Platform.runLater(children::clear);

    List<ModCardController> controllers = mods.parallelStream()
        .map(mod -> {
          ModCardController controller = uiService.loadFxml("theme/vault/mod/mod_card.fxml");
          controller.setMod(mod);
          controller.setOnOpenDetailListener(this::onShowModDetail);
          return controller;
        }).collect(Collectors.toList());

    Iterators.partition(controllers.iterator(), BATCH_SIZE).forEachRemaining(mapCardControllers -> Platform.runLater(() -> {
      for (ModCardController modCardController : mapCardControllers) {
        children.add(modCardController.getRoot());
      }
    }));
  }

  private void enterLoadingState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
  }

  public void onResetButtonClicked() {
    searchTextField.clear();
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
  }

  private void enterSearchResultState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
  }

  private CompletableFuture<Set<Label>> createModSuggestions(String string) {
    return modService.lookupMod(string, MAX_SUGGESTIONS)
        .thenApply(new Function<List<Mod>, Set<Label>>() {
          @Override
          public Set<Label> apply(List<Mod> mods) {
            return mods.stream()
                .map(result -> {
                  String name = result.getName();
                  Label item = new Label(name) {
                    @Override
                    public int hashCode() {
                      return getText().hashCode();
                    }

                    @Override
                    public boolean equals(Object obj) {
                      return obj != null
                          && obj.getClass() == getClass()
                          && getText().equals(((Label) obj).getText());
                    }
                  };
                  item.setUserData(name);
                  return item;
                })
                .collect(Collectors.toSet());
          }
        });
  }

  private void enterShowroomState() {
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(false);
  }

  void onShowModDetail(Mod mod) {
    ModDetailController modDetailController = uiService.loadFxml("theme/vault/mod/mod_detail.fxml");
    modDetailController.setMod(mod);

    Node modDetailRoot = modDetailController.getRoot();
    modVaultRoot.getChildren().add(modDetailRoot);
    AnchorPane.setTopAnchor(modDetailRoot, 0d);
    AnchorPane.setRightAnchor(modDetailRoot, 0d);
    AnchorPane.setBottomAnchor(modDetailRoot, 0d);
    AnchorPane.setLeftAnchor(modDetailRoot, 0d);
    modDetailRoot.requestFocus();
  }

  private void displaySearchResult(List<Mod> mods) {
    populateMods(mods, searchResultPane);
    enterSearchResultState();
  }

  public void onUploadModButtonClicked(ActionEvent actionEvent) {
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

  public void onRefreshClicked() {
    modService.evictModsCache();
    displayShowroomMods();
  }

  @Subscribe
  public void onModUploaded(ModUploadedEvent event) {
    onRefreshClicked();
  }

  public void showMoreRecommendedUiMods() {
    enterLoadingState();
    modService.getMostLikedUiMods(LOAD_MORE_COUNT).thenAccept(this::displaySearchResult);
  }

  public void showMoreNewestMods() {
    enterLoadingState();
    modService.getNewestMods(LOAD_MORE_COUNT).thenAccept(this::displaySearchResult);
  }

  public void showMorePopularMods() {
    enterLoadingState();
    modService.getMostPlayedMods(LOAD_MORE_COUNT).thenAccept(this::displaySearchResult);
  }

  public void showMoreMostLikedMods() {
    enterLoadingState();
    modService.getMostLikedMods(LOAD_MORE_COUNT).thenAccept(this::displaySearchResult);
  }
}
