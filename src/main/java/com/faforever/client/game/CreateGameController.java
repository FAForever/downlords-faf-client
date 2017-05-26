package com.faforever.client.game;

import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.MapSize;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferenceUpdateListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.faforever.client.net.ConnectionState.CONNECTED;
import static javafx.scene.layout.BackgroundPosition.CENTER;
import static javafx.scene.layout.BackgroundRepeat.NO_REPEAT;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateGameController implements Controller<Pane> {

  private static final int MAX_RATING_LENGTH = 4;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MapService mapService;
  private final ModService modService;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final ReportingService reportingService;
  private final FafService fafService;
  private final ConnectivityService connectivityService;
  public Label mapSizeLabel;
  public Label mapPlayersLabel;
  public Label mapDescriptionLabel;
  public Label mapNameLabel;
  public TextField mapSearchTextField;
  public TextField titleTextField;
  public ListView<ModVersion> modListView;
  public TextField passwordTextField;
  public TextField minRankingTextField;
  public TextField maxRankingTextField;
  public ListView<FeaturedMod> featuredModListView;
  public ListView<MapBean> mapListView;
  public Pane createGameRoot;
  public Button createGameButton;
  public Pane mapPreviewPane;
  public Label versionLabel;
  public CheckBox onlyForFriendsCheckBox;
  @VisibleForTesting
  FilteredList<MapBean> filteredMapBeans;
  private Runnable onClosedButtonClickedListener;
  private PreferenceUpdateListener preferenceUpdateListener;
  /**
   * Remembers if the controller's init method was called, to avoid memory leaks by adding several listeners
   */
  private boolean initialized;

  @Inject
  public CreateGameController(FafService fafService, MapService mapService, ModService modService,
                              GameService gameService, PreferencesService preferencesService, I18n i18n,
                              NotificationService notificationService, ReportingService reportingService,
                              ConnectivityService connectivityService) {
    this.mapService = mapService;
    this.modService = modService;
    this.gameService = gameService;
    this.preferencesService = preferencesService;
    this.i18n = i18n;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.fafService = fafService;
    this.connectivityService = connectivityService;
  }

  public void initialize() {
    versionLabel.managedProperty().bind(versionLabel.visibleProperty());

    mapPreviewPane.prefHeightProperty().bind(mapPreviewPane.widthProperty());
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

    featuredModListView.setCellFactory(param -> new StringListCell<>(FeaturedMod::getDisplayName));

    JavaFxUtil.makeNumericTextField(minRankingTextField, MAX_RATING_LENGTH);
    JavaFxUtil.makeNumericTextField(maxRankingTextField, MAX_RATING_LENGTH);

    modService.getFeaturedMods().thenAccept(featuredModBeans -> Platform.runLater(() -> {
      featuredModListView.setItems(FXCollections.observableList(featuredModBeans).filtered(FeaturedMod::isVisible));
      selectLastOrDefaultGameType();
    }));

    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      preferenceUpdateListener = preferences -> {
        if (!initialized && preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
          initialized = true;

          Platform.runLater(this::init);
        }
      };
      preferencesService.addUpdateListener(new WeakReference<>(preferenceUpdateListener));
    } else {
      init();
    }
  }

  public void onCloseButtonClicked() {
    onClosedButtonClickedListener.run();
  }


  private void init() {
    bindGameVisibility();
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
      switch (fafService.connectionStateProperty().get()) {
        case DISCONNECTED:
          return i18n.get("game.create.disconnected");
        case CONNECTING:
          return i18n.get("game.create.connecting");
        default:
          break;
      }
      if (Strings.isNullOrEmpty(titleTextField.getText())) {
        return i18n.get("game.create.titleMissing");
      } else if (featuredModListView.getSelectionModel().getSelectedItem() == null) {
        return i18n.get("game.create.featuredModMissing");
      }
      switch (connectivityService.getConnectivityState()) {
        case BLOCKED:
          return i18n.get("game.create.portUnreachable");
        case RUNNING:
        case UNKNOWN:
          return i18n.get("game.create.connectivityCheckPending");
        default:
          return i18n.get("game.create.create");
      }
    }, titleTextField.textProperty(), featuredModListView.getSelectionModel().selectedItemProperty(), fafService.connectionStateProperty(), connectivityService.connectivityStateProperty()));

    createGameButton.disableProperty().bind(
        titleTextField.textProperty().isEmpty()
            .or(featuredModListView.getSelectionModel().selectedItemProperty().isNull())
            .or(fafService.connectionStateProperty().isNotEqualTo(CONNECTED))
            .or(connectivityService.connectivityStateProperty().isEqualTo(ConnectivityState.BLOCKED))
            .or(connectivityService.connectivityStateProperty().isEqualTo(ConnectivityState.UNKNOWN))
    );
  }

  private void bindGameVisibility() {
    preferencesService.getPreferences()
        .lastGameOnlyFriendsProperty()
        .bindBidirectional(onlyForFriendsCheckBox.selectedProperty());
    onlyForFriendsCheckBox.selectedProperty().addListener(observable -> preferencesService.storeInBackground());
  }

  private void initModList() {
    modListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    modListView.setCellFactory(modListCellFactory());
    modListView.getItems().setAll(modService.getInstalledModVersions());
    try {
      modService.getActivatedSimAndUIMods().forEach(mod -> modListView.getSelectionModel().select(mod));
    } catch (IOException e) {
      logger.error("Activated mods could not be loaded", e);
    }
    modListView.scrollTo(modListView.getSelectionModel().getSelectedItem());
  }

  private void initMapSelection() {
    filteredMapBeans = new FilteredList<>(
        mapService.getInstalledMaps().sorted((o1, o2) -> o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()))
    );

    mapListView.setItems(filteredMapBeans);
    mapListView.setCellFactory(param -> new StringListCell<>(MapBean::getDisplayName));
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> setSelectedMap(newValue)));
  }

  private void setSelectedMap(MapBean newValue) {
    JavaFxUtil.assertApplicationThread();

    if (newValue == null) {
      mapNameLabel.setText("");
      return;
    }

    preferencesService.getPreferences().setLastMap(newValue.getFolderName());
    preferencesService.storeInBackground();

    Image largePreview = mapService.loadPreview(newValue.getFolderName(), PreviewSize.LARGE);
    mapPreviewPane.setBackground(new Background(new BackgroundImage(largePreview, NO_REPEAT, NO_REPEAT, CENTER,
        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false))));

    MapSize mapSize = newValue.getSize();
    mapSizeLabel.setText(i18n.get("mapPreview.size", mapSize.getWidthInKm(), mapSize.getHeightInKm()));
    mapNameLabel.setText(newValue.getDisplayName());
    mapPlayersLabel.setText(i18n.number(newValue.getPlayers()));
    mapDescriptionLabel.setText(Optional.ofNullable(newValue.getDescription())
        .map(Strings::emptyToNull)
        .map(FaStrings::removeLocalizationTag)
        .orElseGet(() -> i18n.get("map.noDescriptionAvailable")));

    ComparableVersion mapVersion = newValue.getVersion();
    if (mapVersion == null) {
      versionLabel.setVisible(false);
    } else {
      versionLabel.setText(i18n.get("map.versionFormat", mapVersion));
    }
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

    minRankingTextField.setText(i18n.number(lastGameMinRating));
    maxRankingTextField.setText(i18n.number(lastGameMaxRating));

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
        mapListView.scrollTo(mapBean);
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
  private Callback<ListView<ModVersion>, ListCell<ModVersion>> modListCellFactory() {
    return param -> {
      ListCell<ModVersion> cell = new StringListCell<>(ModVersion::getDisplayName);
      cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        modListView.requestFocus();
        MultipleSelectionModel<ModVersion> selectionModel = modListView.getSelectionModel();
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
      lastGameMod = KnownFeaturedMod.DEFAULT.getTechnicalName();
    }

    for (FeaturedMod mod : featuredModListView.getItems()) {
      if (Objects.equals(mod.getTechnicalName(), lastGameMod)) {
        featuredModListView.getSelectionModel().select(mod);
        featuredModListView.scrollTo(mod);
        break;
      }
    }
  }

  public void onRandomMapButtonClicked() {
    int mapIndex = (int) (Math.random() * filteredMapBeans.size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  public void onCreateButtonClicked() {
    ObservableList<ModVersion> selectedModVersions = modListView.getSelectionModel().getSelectedItems();

    try {
      modService.overrideActivatedMods(modListView.getSelectionModel().getSelectedItems());
    } catch (IOException e) {
      logger.warn("Activated mods could not be updated", e);
    }

    Set<String> simMods = selectedModVersions.stream()
        .map(ModVersion::getUid)
        .collect(Collectors.toSet());

    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        Strings.emptyToNull(passwordTextField.getText()),
        featuredModListView.getSelectionModel().getSelectedItem(),
        mapListView.getSelectionModel().getSelectedItem().getFolderName(),
        simMods,
        onlyForFriendsCheckBox.isSelected() ? GameVisibility.PRIVATE : GameVisibility.PUBLIC);

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

    onCloseButtonClicked();
  }

  public Pane getRoot() {
    return createGameRoot;
  }

  public void onSelectDefaultGameTypeButtonClicked() {
    featuredModListView.getSelectionModel().select(0);
  }

  public void onDeselectModsButtonClicked() {
    modListView.getSelectionModel().clearSelection();
  }

  public void onReloadModsButtonClicked() {
    modService.loadInstalledMods();
    initModList();
  }

  /**
   * @return returns true of the map was found and false if not
   */
  boolean selectMap(String mapFolderName) {
    Optional<MapBean> mapBeanOptional = mapListView.getItems().stream().filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(mapFolderName)).findAny();
    if (!mapBeanOptional.isPresent()) {
      return false;
    }
    mapListView.getSelectionModel().select(mapBeanOptional.get());
    mapListView.scrollTo(mapBeanOptional.get());
    return true;
  }

  void setOnClosedButtonClickedListener(Runnable onClosedButtonClickedListener) {
    this.onClosedButtonClickedListener = onClosedButtonClickedListener;
  }
}
