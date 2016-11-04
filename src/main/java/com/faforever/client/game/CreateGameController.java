package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapSize;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.ThemeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateGameController {

  public static final int MAX_RATING_LENGTH = 4;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @FXML
  Label mapSizeLabel;
  @FXML
  Label mapPlayersLabel;
  @FXML
  Label mapDescriptionLabel;
  @FXML
  Label mapNameLabel;
  @FXML
  TextField mapSearchTextField;
  @FXML
  ImageView mapImageView;
  @FXML
  TextField titleTextField;
  @FXML
  ListView<ModInfoBean> modListView;
  @FXML
  TextField passwordTextField;
  @FXML
  TextField minRankingTextField;
  @FXML
  TextField maxRankingTextField;
  @FXML
  ListView<FeaturedModBean> featuredModListView;
  @FXML
  ListView<MapBean> mapListView;
  @FXML
  Node createGameRoot;
  @FXML
  Button createGameButton;

  @Resource
  Environment environment;
  @Resource
  MapService mapService;
  @Resource
  ModService modService;
  @Resource
  GameService gameService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  I18n i18n;
  @Resource
  Locale locale;
  @VisibleForTesting
  FilteredList<MapBean> filteredMapBeans;
  @Resource
  ThemeService themeService;
  @Resource
  NotificationService notificationService;
  @Resource
  ReportingService reportingService;

  private Popup createGamePopup;

  @FXML
  void initialize() {
    mapSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.isEmpty()) {
        filteredMapBeans.setPredicate(mapInfoBean -> true);
      } else {
        filteredMapBeans.setPredicate(mapInfoBean -> mapInfoBean.getDisplayName().toLowerCase().contains(newValue.toLowerCase())
            || mapInfoBean.getFolderName().toLowerCase().contains(newValue.toLowerCase()));
      }
      if (!filteredMapBeans.isEmpty()) {
        mapListView.getSelectionModel().select(0);
      }
    });
    mapSearchTextField.setOnKeyPressed(event -> {
      MultipleSelectionModel<MapBean> selectionModel = mapListView.getSelectionModel();
      int currentMapIndex = selectionModel.getSelectedIndex();
      int newMapIndex = currentMapIndex;
      if (KeyCode.DOWN == event.getCode()) {
        if (filteredMapBeans.size() > currentMapIndex + 1) {
          newMapIndex++;
        }
        event.consume();
      } else if (KeyCode.UP == event.getCode()) {
        if (currentMapIndex > 0) {
          newMapIndex--;
        }
        event.consume();
      }
      selectionModel.select(newMapIndex);
      mapListView.scrollTo(newMapIndex);
    });

    featuredModListView.setCellFactory(param -> new StringListCell<>(FeaturedModBean::getDisplayName));

    JavaFxUtil.makeNumericTextField(minRankingTextField, MAX_RATING_LENGTH);
    JavaFxUtil.makeNumericTextField(maxRankingTextField, MAX_RATING_LENGTH);
  }

  @PostConstruct
  void postConstruct() {
    gameService.getFeaturedMods().thenAccept(featuredModBeans -> {
      featuredModListView.setItems(new FilteredList<>(FXCollections.observableList(featuredModBeans), FeaturedModBean::isVisible));
      selectLastOrDefaultGameType();
    });

    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      preferencesService.addUpdateListener(preferences -> {
        if (preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
          init();
        }
      });
    } else {
      init();
    }
  }

  private void init() {
    initModList();
    initMapSelection();
    initFeaturedModList();
    initRatingBoundaries();
    selectLastMap();
    setLastGameTitle();
    titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameTitle(newValue);
      preferencesService.storeInBackground();
    });

    createGameButton.textProperty().bind(Bindings.createStringBinding(() -> {
      if (Strings.isNullOrEmpty(titleTextField.getText())) {
        return i18n.get("game.create.titleMissing");
      }
      return i18n.get("game.create.create");
    }, titleTextField.textProperty()));

    createGameButton.disableProperty().bind(titleTextField.textProperty().isEmpty());
  }

  private void initModList() {
    modListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    modListView.setCellFactory(modListCellFactory());
    modListView.setItems(modService.getInstalledMods());
  }

  private void initMapSelection() {
    filteredMapBeans = new FilteredList<>(mapService.getInstalledMaps());

    mapListView.setItems(filteredMapBeans);
    mapListView.setCellFactory(param -> new StringListCell<>(MapBean::getDisplayName));
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        Platform.runLater(() -> mapNameLabel.setText(""));
        return;
      }

      preferencesService.getPreferences().setLastMap(newValue.getFolderName());
      preferencesService.storeInBackground();

      Image largePreview = mapService.loadLargePreview(newValue.getFolderName());
      if (largePreview == null) {
        new Image(themeService.getThemeFile(ThemeService.UNKNOWN_MAP_IMAGE), true);
      }

      MapSize mapSize = newValue.getSize();

      mapImageView.setImage(largePreview);
      mapNameLabel.setText(newValue.getDisplayName());
      mapSizeLabel.setText(i18n.get("mapPreview.size", mapSize.getWidth(), mapSize.getHeight()));
      mapPlayersLabel.setText(i18n.get("mapPreview.maxPlayers", newValue.getPlayers()));
      mapDescriptionLabel.setText(newValue.getDescription());
    });
  }

  private void initFeaturedModList() {
    featuredModListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameType(newValue.getTechnicalName());
      preferencesService.storeInBackground();
    });
  }

  private void initRatingBoundaries() {
    int lastGameMinRating = preferencesService.getPreferences().getLastGameMinRating();
    int lastGameMaxRating = preferencesService.getPreferences().getLastGameMaxRating();

    minRankingTextField.setText(String.format(locale, "%d", lastGameMinRating));
    maxRankingTextField.setText(String.format(locale, "%d", lastGameMaxRating));

    minRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameMinRating(Integer.parseInt(newValue));
      preferencesService.storeInBackground();
    });
    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setLastGameMaxRating(Integer.parseInt(newValue));
      preferencesService.storeInBackground();
    });
  }

  private void selectLastMap() {
    String lastMap = preferencesService.getPreferences().getLastMap();
    for (MapBean mapBean : mapListView.getItems()) {
      if (mapBean.getFolderName().equalsIgnoreCase(lastMap)) {
        mapListView.getSelectionModel().select(mapBean);
        return;
      }
    }
    if (mapListView.getSelectionModel().isEmpty()) {
      mapListView.getSelectionModel().selectFirst();
    }
  }

  private void setLastGameTitle() {
    titleTextField.setText(Strings.nullToEmpty(preferencesService.getPreferences().getLastGameTitle()));
  }

  @NotNull
  private Callback<ListView<ModInfoBean>, ListCell<ModInfoBean>> modListCellFactory() {
    return param -> {
      ListCell<ModInfoBean> cell = new StringListCell<>(ModInfoBean::getName);
      cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        modListView.requestFocus();
        MultipleSelectionModel<ModInfoBean> selectionModel = modListView.getSelectionModel();
        if (!cell.isEmpty()) {
          int index = cell.getIndex();
          if (selectionModel.getSelectedIndices().contains(index)) {
            selectionModel.clearSelection(index);
          } else {
            selectionModel.select(index);
          }
          event.consume();
        }
      });
      return cell;
    };
  }

  private void selectLastOrDefaultGameType() {
    String lastGameMod = preferencesService.getPreferences().getLastGameType();
    if (lastGameMod == null) {
      lastGameMod = KnownFeaturedMod.DEFAULT.getString();
    }

    for (FeaturedModBean mod : featuredModListView.getItems()) {
      if (Objects.equals(mod.getTechnicalName(), lastGameMod)) {
        featuredModListView.getSelectionModel().select(mod);
        break;
      }
    }
  }

  @FXML
  void onRandomMapButtonClicked() {
    int mapIndex = (int) (Math.random() * filteredMapBeans.size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  @FXML
  void onCreateButtonClicked() {
    ObservableList<ModInfoBean> selectedMods = modListView.getSelectionModel().getSelectedItems();

    Set<String> simMods = selectedMods.stream()
        .map(ModInfoBean::getId)
        .collect(Collectors.toSet());

    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        Strings.emptyToNull(passwordTextField.getText()),
        featuredModListView.getSelectionModel().getSelectedItem(),
        mapListView.getSelectionModel().getSelectedItem().getFolderName(),
        simMods);

    gameService.hostGame(newGameInfo).exceptionally(throwable -> {
      logger.warn("Game could not be hosted", throwable);
      notificationService.addNotification(
          new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("game.create.failed"),
              Severity.WARN,
              throwable,
              Collections.singletonList(new ReportAction(i18n, reportingService, throwable))));
      return null;
    });

    createGamePopup.hide();
  }

  public Node getRoot() {
    return createGameRoot;
  }

  @FXML
  void onSelectDefaultGameTypeButtonClicked(ActionEvent event) {
    featuredModListView.getSelectionModel().select(0);
  }

  @FXML
  void onDeselectModsButtonClicked(ActionEvent event) {
    modListView.getSelectionModel().clearSelection();
  }

  @FXML
  void onReloadModsButtonClicked(ActionEvent event) {
    modService.loadInstalledMods();
  }

  public void show(Window window, double minX, double maxY) {
    createGamePopup = new Popup();
    createGamePopup.setAutoFix(false);
    createGamePopup.setAutoHide(true);
    createGamePopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    createGamePopup.getContent().setAll(createGameRoot);
    createGamePopup.show(window, minX, maxY);
  }
}
