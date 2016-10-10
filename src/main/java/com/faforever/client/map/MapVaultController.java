package com.faforever.client.map;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WindowController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.event.MapUploadedEvent;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;

public class MapVaultController {

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
  Pane newestMapsPane;
  @FXML
  Pane popularMapsPane;
  @FXML
  Pane recommendedMapsPane;
  @FXML
  Pane mapVaultRoot;

  @Resource
  MapService mapService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  MapDetailController mapDetailController;
  @Resource
  I18n i18n;
  @Resource
  EventBus eventBus;
  @Resource
  PreferencesService preferencesService;

  private boolean initialized;

  @FXML
  void initialize() {
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
  }

  @PostConstruct
  void postConstruct() {
    Node mapDetailRoot = mapDetailController.getRoot();
    mapVaultRoot.getChildren().add(mapDetailRoot);
    AnchorPane.setTopAnchor(mapDetailRoot, 0d);
    AnchorPane.setRightAnchor(mapDetailRoot, 0d);
    AnchorPane.setBottomAnchor(mapDetailRoot, 0d);
    AnchorPane.setLeftAnchor(mapDetailRoot, 0d);
    mapDetailRoot.setVisible(false);

    eventBus.register(this);
  }

  public void setUpIfNecessary() {
    if (initialized) {
      return;
    }
    initialized = true;

    displayShowroomMaps();

    JavaFxUtil.makeSuggestionField(searchTextField, this::createMapSuggestions, aVoid -> onSearchMapButtonClicked());
  }

  private void displayShowroomMaps() {
    enterLoadingState();
    mapService.getMostPlayedMaps(TOP_ELEMENT_COUNT).thenAccept(maps -> populateMaps(maps, popularMapsPane))
        .thenCompose(aVoid -> mapService.getMostLikedMaps(TOP_ELEMENT_COUNT)).thenAccept(maps -> populateMaps(maps, recommendedMapsPane))
        .thenCompose(aVoid -> mapService.getNewestMaps(TOP_ELEMENT_COUNT)).thenAccept(maps -> populateMaps(maps, newestMapsPane))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate maps", throwable);
          return null;
        });
  }

  @FXML
  void onSearchMapButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }
    enterSearchResultState();

    mapService.lookupMap(searchTextField.getText(), 100)
        .thenAccept(this::displaySearchResult);
  }

  private void enterLoadingState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
  }

  private void populateMaps(List<MapBean> maps, Pane pane) {
    ObservableList<Node> children = pane.getChildren();
    Platform.runLater(() -> {
      children.clear();
      for (MapBean map : maps) {
        MapTileController controller = applicationContext.getBean(MapTileController.class);
        controller.setMap(map);
        controller.setOnOpenDetailListener(this::onShowMapDetail);
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

  private CompletionStage<Set<Label>> createMapSuggestions(String string) {
    return mapService.lookupMap(string, MAX_SUGGESTIONS)
        .thenApply(new Function<List<MapBean>, Set<Label>>() {
          @Override
          public Set<Label> apply(List<MapBean> mapBeans) {
            return mapBeans.stream()
                .map(result -> {
                  String name = result.getDisplayName();
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
  void onShowMapDetail(MapBean map) {
    mapDetailController.setMap(map);
    mapDetailController.getRoot().setVisible(true);
    mapDetailController.getRoot().requestFocus();
  }

  @FXML
  void onSearchMapsButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }
    enterSearchResultState();

    mapService.lookupMap(searchTextField.getText(), 100)
        .thenAccept(this::displaySearchResult);
  }

  private void displaySearchResult(List<MapBean> maps) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);

    populateMaps(maps, searchResultPane);
  }

  public void onUploadMapButtonClicked() {
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory().toFile());
      directoryChooser.setTitle(i18n.get("mapVault.upload.chooseDirectory"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      openMapUploadWindow(result.toPath());
    });
  }

  public Node getRoot() {
    return mapVaultRoot;
  }

  private void openMapUploadWindow(Path path) {
    MapUploadController mapUploadController = applicationContext.getBean(MapUploadController.class);
    mapUploadController.setMapPath(path);

    Stage mapUploadWindow = new Stage(StageStyle.TRANSPARENT);
    mapUploadWindow.initModality(Modality.NONE);
    mapUploadWindow.initOwner(getRoot().getScene().getWindow());

    WindowController windowController = applicationContext.getBean(WindowController.class);
    windowController.configure(mapUploadWindow, mapUploadController.getRoot(), true, CLOSE);

    mapUploadWindow.show();
  }

  public void onRefreshClicked() {
    displayShowroomMaps();
  }

  @Subscribe
  public void onMapUploaded(MapUploadedEvent event) {
    onRefreshClicked();
  }
}
