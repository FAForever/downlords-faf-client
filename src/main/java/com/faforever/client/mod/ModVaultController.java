package com.faforever.client.mod;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WindowController;
import com.faforever.client.i18n.I18n;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

public class ModVaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 7;
  private static final int MAX_SUGGESTIONS = 10;

  @FXML
  Pane searchResultGroup;
  @FXML
  Pane searchResultPane;
  @FXML
  Pane showroomGroup;
  @FXML
  Pane loadingPane;
  @FXML
  TextField searchTextField;
  @FXML
  Pane recommendedUiModsPane;
  @FXML
  Pane newestModsPane;
  @FXML
  Pane popularModsPane;
  @FXML
  Pane mostLikedModsPane;
  @FXML
  Pane modVaultRoot;

  @Resource
  ModService modService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  ModDetailController modDetailController;
  @Resource
  I18n i18n;

  private boolean initialized;

  @FXML
  void initialize() {
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
  }

  @PostConstruct
  void postConstruct() {
    Node modDetailRoot = modDetailController.getRoot();
    modVaultRoot.getChildren().add(modDetailRoot);
    AnchorPane.setTopAnchor(modDetailRoot, 0d);
    AnchorPane.setRightAnchor(modDetailRoot, 0d);
    AnchorPane.setBottomAnchor(modDetailRoot, 0d);
    AnchorPane.setLeftAnchor(modDetailRoot, 0d);
    modDetailRoot.setVisible(false);
  }

  public void setUpIfNecessary() {
    if (initialized) {
      return;
    }
    initialized = true;

    displayShowroomMods();

    JavaFxUtil.makeSuggestionField(searchTextField, this::createModSuggestions, aVoid -> onSearchModButtonClicked());
  }

  private void displayShowroomMods() {
    enterLoadingState();
    modService.getMostDownloadedMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, popularModsPane))
        .thenCompose(aVoid -> modService.getMostLikedMods(TOP_ELEMENT_COUNT)).thenAccept(modInfoBeans -> populateMods(modInfoBeans, mostLikedModsPane))
        .thenCompose(aVoid -> modService.getNewestMods(TOP_ELEMENT_COUNT)).thenAccept(modInfoBeans -> populateMods(modInfoBeans, newestModsPane))
        .thenCompose(aVoid -> modService.getMostLikedUiMods(TOP_ELEMENT_COUNT)).thenAccept(modInfoBeans -> populateMods(modInfoBeans, recommendedUiModsPane))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
  }

  @FXML
  void onSearchModButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }
    enterSearchResultState();

    modService.lookupMod(searchTextField.getText(), 100)
        .thenAccept(this::displaySearchResult);
  }

  private void enterLoadingState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
  }

  private void populateMods(List<ModInfoBean> modInfoBeans, Pane pane) {
    ObservableList<Node> children = pane.getChildren();
    Platform.runLater(() -> {
      children.clear();
      for (ModInfoBean mod : modInfoBeans) {
        ModTileController controller = applicationContext.getBean(ModTileController.class);
        controller.setMod(mod);
        controller.setOnOpenDetailListener(this::onShowModDetail);
        children.add(controller.getRoot());
      }
    });
  }

  @FXML
  void onResetButtonClicked() {
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
    return modService.lookupMod(string, MAX_SUGGESTIONS).thenApply(new Function<List<ModInfoBean>, Set<Label>>() {
      @Override
      public Set<Label> apply(List<ModInfoBean> modInfoBeans) {
        return modInfoBeans.stream()
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

  @FXML
  void onShowModDetail(ModInfoBean mod) {
    modDetailController.setMod(mod);
    modDetailController.getRoot().setVisible(true);
    modDetailController.getRoot().requestFocus();
  }

  private void displaySearchResult(List<ModInfoBean> modInfoBeans) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);

    populateMods(modInfoBeans, searchResultPane);
  }

  public void onUploadModButtonClicked(ActionEvent actionEvent) {
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle(i18n.get("modVault.upload.choseDirectory"));
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
    UploadModController uploadModController = applicationContext.getBean(UploadModController.class);
    uploadModController.setModPath(path);

    Stage modUploadWindow = new Stage(StageStyle.TRANSPARENT);
    modUploadWindow.initModality(Modality.NONE);
    modUploadWindow.initOwner(getRoot().getScene().getWindow());

    WindowController windowController = applicationContext.getBean(WindowController.class);
    windowController.configure(modUploadWindow, uploadModController.getRoot(), true, CLOSE);

    modUploadWindow.show();
  }
}
