package com.faforever.client.game;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapBean.MapType;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.DualStringListCell;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.MapSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModManagerController;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.PreferenceUpdateListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.user.UserService;
import com.faforever.commons.lobby.GameVisibility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.client.net.ConnectionState.CONNECTED;
import static javafx.scene.layout.BackgroundPosition.CENTER;
import static javafx.scene.layout.BackgroundRepeat.NO_REPEAT;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class CreateGameController implements Controller<Pane> {

  public static final String STYLE_CLASS_DUAL_LIST_CELL = "create-game-dual-list-cell";
  public static final PseudoClass PSEUDO_CLASS_INVALID = PseudoClass.getPseudoClass("invalid");
  private static final int MAX_RATING_LENGTH = 4;
  private final MapService mapService;
  private final ModService modService;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final UserService userService;
  private final MapGeneratorService mapGeneratorService;
  private final UiService uiService;
  public Label mapSizeLabel;
  public Label mapPlayersLabel;
  public Label mapDescriptionLabel;
  public Label mapNameLabel;
  public ModManagerController modManagerController;
  public TextField mapSearchTextField;
  public TextField titleTextField;
  public TextField passwordTextField;
  public TextField minRankingTextField;
  public TextField maxRankingTextField;
  public CheckBox enforceRankingCheckBox;
  public ListView<FeaturedModBean> featuredModListView;
  public ListView<MapVersionBean> mapListView;
  public StackPane gamesRoot;
  public Pane createGameRoot;
  public Button createGameButton;
  public Pane mapPreviewPane;
  public Label versionLabel;
  public CheckBox onlyForFriendsCheckBox;
  public Button generateMapButton;
  public ToggleButton mapFilterButton;
  public Popup mapFilterPopup;
  @VisibleForTesting
  FilteredList<MapVersionBean> filteredMaps;
  private Runnable onCloseButtonClickedListener;
  private PreferenceUpdateListener preferenceUpdateListener;
  private MapFilterController mapFilterController;
  private InvalidationListener createButtonStateListener;
  /**
   * Remembers if the controller's init method was called, to avoid memory leaks by adding several listeners
   */
  private boolean initialized;

  public void initialize() {
    JavaFxUtil.addLabelContextMenus(uiService, mapNameLabel, mapDescriptionLabel);
    JavaFxUtil.bindManagedToVisible(versionLabel);
    JavaFxUtil.bind(mapPreviewPane.prefHeightProperty(), mapPreviewPane.widthProperty());
    modManagerController.setCloseable(false);
    mapSearchTextField.setOnKeyPressed(event -> {
      MultipleSelectionModel<MapVersionBean> selectionModel = mapListView.getSelectionModel();
      int currentMapIndex = selectionModel.getSelectedIndex();
      int newMapIndex = currentMapIndex;
      if (KeyCode.DOWN == event.getCode()) {
        if (filteredMaps.size() > currentMapIndex + 1) {
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

    createButtonStateListener = observable -> setCreateGameButtonState();

    Function<FeaturedModBean, String> isDefaultModString = mod ->
        Objects.equals(mod.getTechnicalName(), KnownFeaturedMod.DEFAULT.getTechnicalName()) ?
            " " + i18n.get("game.create.defaultGameTypeMarker") : null;

    featuredModListView.setCellFactory(param ->
        new DualStringListCell<>(
            FeaturedModBean::getDisplayName,
            isDefaultModString,
            FeaturedModBean::getDescription,
            STYLE_CLASS_DUAL_LIST_CELL, uiService
        )
    );

    JavaFxUtil.makeNumericTextField(minRankingTextField, MAX_RATING_LENGTH, true);
    JavaFxUtil.makeNumericTextField(maxRankingTextField, MAX_RATING_LENGTH, true);

    modService.getFeaturedMods().thenAccept(featuredModBeans -> JavaFxUtil.runLater(() -> {
      featuredModListView.setItems(FXCollections.observableList(featuredModBeans).filtered(FeaturedModBean::getVisible));
      selectLastOrDefaultGameType();
    }));

    if (preferencesService.getPreferences().getForgedAlliance().getInstallationPath() == null) {
      preferenceUpdateListener = preferences -> {
        if (!initialized && preferencesService.getPreferences().getForgedAlliance().getInstallationPath() != null) {
          initialized = true;

          JavaFxUtil.runLater(this::init);
        }
      };
      preferencesService.addUpdateListener(new WeakReference<>(preferenceUpdateListener));
    } else {
      init();
    }
  }

  public void onCloseButtonClicked() {
    onCloseButtonClickedListener.run();
  }


  private void init() {
    bindGameVisibility();
    initMapSelection();
    initFeaturedModList();
    initRatingBoundaries();
    selectLastMap();
    setLastGameTitle();
    initPassword();
    JavaFxUtil.addAndTriggerListener(titleTextField.textProperty(), (observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getLastGame().setLastGameTitle(newValue);
      preferencesService.storeInBackground();
      validateTitle(newValue);
    });

    JavaFxUtil.addAndTriggerListener(userService.connectionStateProperty(), new WeakInvalidationListener(createButtonStateListener));
    JavaFxUtil.addListener(titleTextField.textProperty(), new WeakInvalidationListener(createButtonStateListener));
    JavaFxUtil.addListener(featuredModListView.getSelectionModel().selectedItemProperty(), new WeakInvalidationListener(createButtonStateListener));

    initMapFilterPopup();
  }

  private void setCreateGameButtonState() {
    boolean disable = titleTextField.getText().isEmpty()
        || featuredModListView.getSelectionModel().getSelectedItem() == null
        || userService.getConnectionState() != CONNECTED;

    ConnectionState lobbyConnectionState = userService.getConnectionState();
    String createGameButtonText = switch (lobbyConnectionState) {
      case DISCONNECTED -> i18n.get("game.create.disconnected");
      case CONNECTING -> i18n.get("game.create.connecting");
      case CONNECTED -> {
        if (Strings.isNullOrEmpty(titleTextField.getText())) {
          yield i18n.get("game.create.titleMissing");
        } else if (featuredModListView.getSelectionModel().getSelectedItem() == null) {
          yield i18n.get("game.create.featuredModMissing");
        } else {
          yield i18n.get("game.create.create");
        }
      }
    };

    JavaFxUtil.runLater(() -> createGameButton.setDisable(disable));
    JavaFxUtil.runLater(() -> createGameButton.setText(createGameButtonText));
  }

  private void initMapFilterPopup() {
    mapFilterPopup = new Popup();
    mapFilterPopup.setAutoFix(false);
    mapFilterPopup.setAutoHide(true);
    mapFilterPopup.setAnchorLocation(AnchorLocation.CONTENT_BOTTOM_LEFT);

    mapFilterController = uiService.loadFxml("theme/play/map_filter.fxml");
    mapFilterController.setMapNameTextField(mapSearchTextField);
    mapFilterController.getFilterAppliedProperty().addListener(((observable, old, newValue) -> mapFilterButton.setSelected(newValue)));
    mapFilterController.setFilteredMapList(filteredMaps);
    mapFilterPopup.getContent().setAll(mapFilterController.getRoot());
  }

  private void validateTitle(String gameTitle) {
    titleTextField.pseudoClassStateChanged(PSEUDO_CLASS_INVALID, Strings.isNullOrEmpty(gameTitle));
  }

  private void initPassword() {
    LastGamePrefs lastGamePrefs = preferencesService.getPreferences().getLastGame();
    passwordTextField.setText(lastGamePrefs.getLastGamePassword());
    JavaFxUtil.addListener(passwordTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGamePrefs.setLastGamePassword(newValue);
      preferencesService.storeInBackground();
    });
  }

  private void bindGameVisibility() {
    onlyForFriendsCheckBox.selectedProperty().bindBidirectional(
        preferencesService.getPreferences()
            .getLastGame()
            .lastGameOnlyFriendsProperty()
    );

    onlyForFriendsCheckBox.selectedProperty().addListener(observable -> preferencesService.storeInBackground());
  }

  protected void initMapSelection() {
    filteredMaps = new FilteredList<>(
        mapService.getInstalledMaps().filtered(mapVersion -> mapVersion.getMap().getMapType() == MapType.SKIRMISH).sorted(Comparator.comparing(mapVersion -> mapVersion.getMap().getDisplayName()))
    );
    JavaFxUtil.addListener(filteredMaps.predicateProperty(), (observable, oldValue, newValue) -> {
      if (!filteredMaps.isEmpty()) {
        mapListView.getSelectionModel().select(0);
      }
    });

    mapListView.setItems(filteredMaps);
    mapListView.setCellFactory(param -> new StringListCell<>(mapVersion -> mapVersion.getMap().getDisplayName()));
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setSelectedMap(newValue));
  }

  protected void setSelectedMap(MapVersionBean mapVersion) {
    if (mapVersion == null) {
      JavaFxUtil.runLater(() -> mapNameLabel.setText(""));
      return;
    }

    ComparableVersion version = mapVersion.getVersion();
    MapSize mapSize = mapVersion.getSize();
    Image largePreview = mapService.loadPreview(mapVersion.getFolderName(), PreviewSize.LARGE);
    preferencesService.getPreferences().getLastGame().setLastMap(mapVersion.getFolderName());
    preferencesService.storeInBackground();

    JavaFxUtil.runLater(() -> {
      mapPreviewPane.setBackground(new Background(new BackgroundImage(largePreview, NO_REPEAT, NO_REPEAT, CENTER,
          new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false))));
      mapSizeLabel.setText(i18n.get("mapPreview.size", mapSize.getWidthInKm(), mapSize.getHeightInKm()));
      mapNameLabel.setText(mapVersion.getMap().getDisplayName());
      mapPlayersLabel.setText(i18n.number(mapVersion.getMaxPlayers()));
      mapDescriptionLabel.setText(Optional.ofNullable(mapVersion.getDescription())
          .map(Strings::emptyToNull)
          .map(FaStrings::removeLocalizationTag)
          .orElseGet(() -> i18n.get("map.noDescriptionAvailable")));
      if (version == null) {
        versionLabel.setVisible(false);
      } else {
        versionLabel.setVisible(true);
        versionLabel.setText(i18n.get("map.versionFormat", version));
      }
    });
  }

  private void initFeaturedModList() {
    featuredModListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getLastGame().setLastGameType(newValue.getTechnicalName());
      preferencesService.storeInBackground();
    });
  }

  private void initRatingBoundaries() {
    Integer lastGameMinRating = preferencesService.getPreferences().getLastGame().getLastGameMinRating();
    Integer lastGameMaxRating = preferencesService.getPreferences().getLastGame().getLastGameMaxRating();

    if (lastGameMinRating != null) {
      minRankingTextField.setText(i18n.number(lastGameMinRating));
    }

    if (lastGameMaxRating != null) {
      maxRankingTextField.setText(i18n.number(lastGameMaxRating));
    }

    minRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      Integer minRating = null;
      if (!newValue.isEmpty()) {
        minRating = Integer.parseInt(newValue);
      }

      preferencesService.getPreferences().getLastGame().setLastGameMinRating(minRating);
      preferencesService.storeInBackground();
    });

    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      Integer maxRating = null;
      if (!newValue.isEmpty()) {
        maxRating = Integer.parseInt(newValue);
      }
      preferencesService.getPreferences().getLastGame().setLastGameMaxRating(maxRating);
      preferencesService.storeInBackground();
    });

    enforceRankingCheckBox.selectedProperty()
        .bindBidirectional(preferencesService.getPreferences().getLastGame().lastGameEnforceRatingProperty());
    enforceRankingCheckBox.selectedProperty().addListener(observable -> preferencesService.storeInBackground());
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
    if (mapListView.getSelectionModel().isEmpty()) {
      mapListView.getSelectionModel().selectFirst();
    }
  }

  private void setLastGameTitle() {
    titleTextField.setText(Strings.nullToEmpty(preferencesService.getPreferences().getLastGame().getLastGameTitle()));
  }

  private void selectLastOrDefaultGameType() {
    JavaFxUtil.assertApplicationThread();
    String lastGameMod = preferencesService.getPreferences().getLastGame().getLastGameType();
    if (lastGameMod == null) {
      lastGameMod = KnownFeaturedMod.DEFAULT.getTechnicalName();
    }

    for (FeaturedModBean mod : featuredModListView.getItems()) {
      if (Objects.equals(mod.getTechnicalName(), lastGameMod)) {
        featuredModListView.getSelectionModel().select(mod);
        featuredModListView.scrollTo(mod);
        break;
      }
    }
  }

  public void onRandomMapButtonClicked() {
    int mapIndex = (int) (Math.random() * filteredMaps.size());
    mapListView.getSelectionModel().select(mapIndex);
    mapListView.scrollTo(mapIndex);
  }

  public void onGenerateMapButtonClicked() {
    onGenerateMap();
  }

  private void onGenerateMap() {
    try {
      mapGeneratorService.setGeneratorVersion(mapGeneratorService.queryMaxSupportedVersion());
      GenerateMapController generateMapController = uiService.loadFxml("theme/play/generate_map.fxml");
      mapGeneratorService.downloadGeneratorIfNecessary(mapGeneratorService.getGeneratorVersion())
          .thenCompose(aVoid -> mapGeneratorService.getGeneratorStyles().thenAccept(generateMapController::setStyles));

      Pane root = generateMapController.getRoot();
      generateMapController.setCreateGameController(this);
      Dialog dialog = uiService.showInDialog(gamesRoot, root, i18n.get("game.generateMap.dialog"));
      generateMapController.setOnCloseButtonClickedListener(dialog::close);

      root.requestFocus();
    } catch (Exception e) {
      log.error("Map generation failed", e);
      notificationService.addImmediateErrorNotification(e, "mapGenerator.generationFailed");
    }
  }

  public void onCreateButtonClicked() {
    onCloseButtonClicked();
    hostGameAfterMapAndModUpdate();
  }

  private void hostGameAfterMapAndModUpdate() {
    MapVersionBean selectedMap = mapListView.getSelectionModel().getSelectedItem();
    List<ModVersionBean> selectedModVersions = modManagerController.getSelectedModVersions();

    mapService.updateLatestVersionIfNecessary(selectedMap)
        .exceptionally(throwable -> {
          log.error("Error when updating the map", throwable);
          return selectedMap;
        })
        .thenCombine(modService.updateAndActivateModVersions(selectedModVersions)
            .exceptionally(throwable -> {
              log.error("Error when updating selected mods and preparing game", throwable);
              notificationService.addImmediateErrorNotification(throwable, "game.create.errorUpdatingMods");
              return selectedModVersions;
            }), (mapBean, mods) -> {
          hostGame(mapBean, getUUIDsFromModVersions(mods));
          return null;
        });

  }

  @NotNull
  private Set<String> getUUIDsFromModVersions(List<ModVersionBean> modVersions) {
    return modVersions.stream()
        .map(ModVersionBean::getUid)
        .collect(Collectors.toSet());
  }

  private void hostGame(MapVersionBean mapVersion, Set<String> mods) {
    Integer minRating = null;
    Integer maxRating = null;
    boolean enforceRating;

    if (!minRankingTextField.getText().isEmpty()) {
      minRating = Integer.parseInt(minRankingTextField.getText());
    }

    if (!maxRankingTextField.getText().isEmpty()) {
      maxRating = Integer.parseInt(maxRankingTextField.getText());
    }

    enforceRating = enforceRankingCheckBox.isSelected();

    NewGameInfo newGameInfo = new NewGameInfo(
        titleTextField.getText(),
        Strings.emptyToNull(passwordTextField.getText()),
        featuredModListView.getSelectionModel().getSelectedItem(),
        mapVersion.getFolderName(),
        mods,
        onlyForFriendsCheckBox.isSelected() ? GameVisibility.PRIVATE : GameVisibility.PUBLIC,
        minRating,
        maxRating,
        enforceRating);

    gameService.hostGame(newGameInfo).exceptionally(throwable -> {
      log.warn("Game could not be hosted", throwable);
      notificationService.addImmediateErrorNotification(throwable, "game.create.failed");
      return null;
    });
  }

  public Pane getRoot() {
    return createGameRoot;
  }

  public void setGamesRoot(StackPane root) {
    gamesRoot = root;
  }

  /**
   * @return returns true of the map was found and false if not
   */
  boolean selectMap(String mapFolderName) {
    Optional<MapVersionBean> mapBeanOptional = mapListView.getItems().stream().filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(mapFolderName)).findAny();
    if (mapBeanOptional.isEmpty()) {
      return false;
    }
    mapListView.getSelectionModel().select(mapBeanOptional.get());
    mapListView.scrollTo(mapBeanOptional.get());
    return true;
  }

  void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }

  public void onMapFilterButtonClicked() {
    mapFilterButton.setSelected(mapFilterController.getFilterAppliedProperty().getValue());
    if (mapFilterPopup.isShowing()) {
      mapFilterPopup.hide();
    } else {
      Bounds screenBounds = mapSearchTextField.localToScreen(mapSearchTextField.getBoundsInLocal());
      mapFilterPopup.show(mapSearchTextField.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMinY());
    }
  }
}
