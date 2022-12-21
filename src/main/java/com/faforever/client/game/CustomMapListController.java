package com.faforever.client.game;

import com.faforever.client.domain.MapBean.MapType;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.filter.MapFilterController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.util.PopupUtil;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class CustomMapListController implements Controller<VBox> {

  private final UiService uiService;
  private final MapGeneratorService mapGeneratorService;
  private final NotificationService notificationService;
  private final PreferencesService preferencesService;
  private final MapService mapService;
  private final I18n i18n;
  public VBox root;
  public ToggleButton mapFilterToggleButton;
  public TextField mapSearchTextField;
  public ListView<MapVersionBean> mapListView;
  public Popup mapFilterPopup;
  private FilteredList<MapVersionBean> maps;
  private Supplier<StackPane> onStackPaneRequest;
  private MapFilterController mapFilterController;

  @Override
  public void initialize() {
    initMapFilter();
    initMapList();
    selectLastMap();
  }

  private void initMapList() {
    maps = mapService.getInstalledMaps()
        .sorted(Comparator.comparing(mapVersion -> mapVersion.getMap().getDisplayName().toLowerCase()))
        .filtered(mapVersion -> mapVersion.getMap().getMapType() == MapType.SKIRMISH);
    JavaFxUtil.addListener(maps.predicateProperty(), observable -> mapListView.getSelectionModel().selectFirst());
    mapListView.setItems(maps);
    mapListView.setCellFactory(param -> new StringListCell<>(mapVersion -> mapVersion.getMap().getDisplayName()));
  }


  private void initMapFilter() {
    mapFilterController = uiService.loadFxml("theme/filter/filter.fxml", MapFilterController.class);
    mapFilterController.bindExternalFilter(mapSearchTextField.textProperty(),
        (text, mapVersion) -> text.isEmpty() || mapVersion.getMap()
            .getDisplayName()
            .toLowerCase()
            .contains(text.toLowerCase()) || mapVersion.getFolderName().toLowerCase().contains(text.toLowerCase()));
    mapFilterController.completeSetting();

    JavaFxUtil.addAndTriggerListener(mapFilterController.getFilterStateProperty(), (observable, oldValue, newValue) -> mapFilterToggleButton.setSelected(newValue));
    JavaFxUtil.addAndTriggerListener(mapFilterToggleButton.selectedProperty(), observable -> mapFilterToggleButton.setSelected(mapFilterController.getFilterState()));
    JavaFxUtil.addListener(mapFilterController.predicateProperty(), (observable, oldValue, newValue) -> maps.setPredicate(newValue));

    mapSearchTextField.setOnKeyPressed(event -> {
      KeyCode code = event.getCode();
      if (code == KeyCode.DOWN || code == KeyCode.UP) {
        event.consume();
        final MultipleSelectionModel<MapVersionBean> selection = mapListView.getSelectionModel();
        final int selectedIndex = selection.getSelectedIndex();
        if (code == KeyCode.UP && selectedIndex != 0) {
          selection.selectPrevious();
        } else if (code == KeyCode.DOWN && selectedIndex + 1 != mapListView.getItems().size()) {
          selection.selectNext();
        }
        mapListView.scrollTo(getSelectedMap());
      }
    });
  }

  private void selectLastMap() {
    String lastMap = preferencesService.getPreferences().getLastGame().getLastMap();
    for (MapVersionBean mapVersion : mapListView.getItems()) {
      if (mapVersion.getFolderName().equalsIgnoreCase(lastMap)) {
        mapListView.getSelectionModel().select(mapVersion);
        mapListView.scrollTo(mapVersion);
        return;
      }
    }
    mapListView.getSelectionModel().selectFirst();
  }

  public void onRandomMapClicked() {
    int mapIndex = (int) (Math.random() * mapListView.getItems().size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  public void onGenerateMapClicked() {
    GenerateMapController generateMapController = uiService.loadFxml("theme/play/generate_map.fxml");
    generateMapController.setOnGeneratedMapHandler(generatedMapName -> {
      mapListView.refresh();
      mapListView.getItems().stream()
          .filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(generatedMapName))
          .findAny().ifPresent(mapBean -> {
            mapListView.getSelectionModel().select(mapBean);
            mapListView.scrollTo(mapBean);
          });
    });

    mapGeneratorService.getNewestGenerator()
        .thenCompose(aVoid -> mapGeneratorService.getGeneratorStyles())
        .thenAccept(generateMapController::setStyles)
        .thenRun(() -> JavaFxUtil.runLater(() -> {
          Pane root = generateMapController.getRoot();
          Dialog dialog = uiService.showInDialog(onStackPaneRequest.get(), root, i18n.get("game.generateMap.dialog"));
          generateMapController.setOnCloseControllerRequest(() -> {
            dialog.close();
            preferencesService.storeInBackground();
          });
          root.requestFocus();
        }))
        .exceptionally(throwable -> {
          log.error("Opening map generation ui failed", throwable);
          notificationService.addImmediateErrorNotification(throwable, "mapGenerator.generationUIFailed");
          return null;
        });
  }

  public void onMapFilterButtonClicked() {
    if (mapFilterPopup == null) {
      mapFilterPopup = PopupUtil.createPopup(AnchorLocation.CONTENT_TOP_RIGHT, mapFilterController.getRoot());
    }

    if (mapFilterPopup.isShowing()) {
      mapFilterPopup.hide();
    } else {
      Bounds screenBounds = mapFilterToggleButton.localToScreen(mapFilterToggleButton.getBoundsInLocal());
      mapFilterPopup.show(mapFilterToggleButton.getScene()
          .getWindow(), screenBounds.getMinX() - 10, screenBounds.getMinY());
    }
  }

  public void setOnStackPaneRequest(Supplier<StackPane> onStackPaneRequest) {
    this.onStackPaneRequest = onStackPaneRequest;
  }

  public ReadOnlyObjectProperty<MapVersionBean> selectedMapProperty() {
    return mapListView.getSelectionModel().selectedItemProperty();
  }

  public MapVersionBean getSelectedMap() {
    return mapListView.getSelectionModel().getSelectedItem();
  }

  @Override
  public VBox getRoot() {
    return root;
  }

  public void selectMap(@Nullable String mapFolderName) {
    Optional.ofNullable(mapFolderName).ifPresent(folderName -> mapListView.getItems()
        .stream()
        .filter(map -> map.getFolderName().equalsIgnoreCase(folderName))
        .findAny().ifPresentOrElse(map -> {
          mapListView.getSelectionModel().select(map);
          mapListView.scrollTo(map);
        }, () -> log.warn("Map with folder name '{}' could not be found in map list", folderName)));
  }
}
