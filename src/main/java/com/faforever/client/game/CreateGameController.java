package com.faforever.client.game;

import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.DualStringListCell;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBean.Type;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.MapSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModManagerController;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.PreferenceUpdateListener;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
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
  private final FafService fafService;
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
  public ListView<FeaturedMod> featuredModListView;
  public ListView<MapBean> mapListView;
  public StackPane gamesRoot;
  public Pane createGameRoot;
  public Button createGameButton;
  public Pane mapPreviewPane;
  public Label versionLabel;
  public CheckBox onlyForFriendsCheckBox;
  public Button generateMapButton;
  public Label moreFiltersLabel;
  public VBox additionalFiltersBox;
  public TextField numberOfPlayersTextField;
  public TextField mapWidthTextField;
  public TextField mapHeightTextField;
  @VisibleForTesting
  FilteredList<MapBean> filteredMapBeans;
  private Runnable onCloseButtonClickedListener;
  private PreferenceUpdateListener preferenceUpdateListener;
  /**
   * Remembers if the controller's init method was called, to avoid memory leaks by adding several listeners
   */
  private boolean initialized;

  public void initialize() {
    JavaFxUtil.addLabelContextMenus(uiService, mapNameLabel, mapDescriptionLabel);
    JavaFxUtil.bindManagedToVisible(additionalFiltersBox, versionLabel);
    JavaFxUtil.bind(mapPreviewPane.prefHeightProperty(), mapPreviewPane.widthProperty());
    prepareListenersForSearchTextFields();

    Function<FeaturedMod, String> isDefaultModString = mod ->
        Objects.equals(mod.getTechnicalName(), KnownFeaturedMod.DEFAULT.getTechnicalName()) ?
            " " + i18n.get("game.create.defaultGameTypeMarker") : null;

    featuredModListView.setCellFactory(param ->
        new DualStringListCell<>(
            FeaturedMod::getDisplayName,
            isDefaultModString,
            FeaturedMod::getDescription,
            STYLE_CLASS_DUAL_LIST_CELL, uiService
        )
    );

    JavaFxUtil.makeNumericTextField(minRankingTextField, MAX_RATING_LENGTH, true);
    JavaFxUtil.makeNumericTextField(maxRankingTextField, MAX_RATING_LENGTH, true);

    modService.getFeaturedMods().thenAccept(featuredModBeans -> JavaFxUtil.runLater(() -> {
      featuredModListView.setItems(FXCollections.observableList(featuredModBeans).filtered(FeaturedMod::isVisible));
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

  private void prepareListenersForSearchTextFields() {
    JavaFxUtil.makeNumericTextField(numberOfPlayersTextField, 2, false);
    JavaFxUtil.makeNumericTextField(mapWidthTextField, 2, false);
    JavaFxUtil.makeNumericTextField(mapHeightTextField, 2, false);

    JavaFxUtil.addListener(mapSearchTextField.textProperty(), (obs, old, value) -> runMapSearch());
    JavaFxUtil.addListener(numberOfPlayersTextField.textProperty(), (obs, old, value) -> runMapSearch());
    JavaFxUtil.addListener(mapWidthTextField.textProperty(), (obs, old, value) -> runMapSearch());
    JavaFxUtil.addListener(mapHeightTextField.textProperty(), (obs, old, value) -> runMapSearch());
    mapSearchTextField.setOnKeyPressed(getOnDownKeyEventHandler());
    numberOfPlayersTextField.setOnKeyPressed(getOnDownKeyEventHandler());
    mapWidthTextField.setOnKeyPressed(getOnDownKeyEventHandler());
    mapHeightTextField.setOnKeyPressed(getOnDownKeyEventHandler());
  }

  private EventHandler<KeyEvent> getOnDownKeyEventHandler() {
    return (event) -> {
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
    };
  }

  private void runMapSearch() {
    filteredMapBeans.setPredicate((map) ->
        isEqualToNumberOfPlayers(map) && isEqualToMapWidth(map) && isEqualToMapHeight(map) && containsMapName(map));
    if (!filteredMapBeans.isEmpty()) {
      mapListView.getSelectionModel().select(0);
    }
  }

  private boolean containsMapName(MapBean map) {
    String value = mapSearchTextField.getText().toLowerCase();
    return map.getDisplayName().toLowerCase().contains(value) || map.getFolderName().toLowerCase().contains(value);
  }

  private boolean isEqualToNumberOfPlayers(MapBean map) {
    String value = numberOfPlayersTextField.getText();
    return value.isEmpty() || map.getPlayers() == Integer.parseInt(value);
  }

  private boolean isEqualToMapWidth(MapBean map) {
    String value = mapWidthTextField.getText();
    return value.isEmpty() || map.getSize().getWidthInKm() == Integer.parseInt(value);
  }

  private boolean isEqualToMapHeight(MapBean map) {
    String value = mapHeightTextField.getText();
    return value.isEmpty() || map.getSize().getHeightInKm() == Integer.parseInt(value);
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
    titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getLastGame().setLastGameTitle(newValue);
      preferencesService.storeInBackground();
      validateTitle(newValue);
    });
    validateTitle(titleTextField.getText());

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
      return i18n.get("game.create.create");
    }, titleTextField.textProperty(), featuredModListView.getSelectionModel().selectedItemProperty(), fafService.connectionStateProperty()));

    createGameButton.disableProperty().bind(
        titleTextField.textProperty().isEmpty()
            .or(featuredModListView.getSelectionModel().selectedItemProperty().isNull().or(fafService.connectionStateProperty().isNotEqualTo(CONNECTED)))
    );
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
    filteredMapBeans = new FilteredList<>(
        mapService.getInstalledMaps().filtered(mapBean -> mapBean.getType() == Type.SKIRMISH).sorted((o1, o2) -> o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()))
    );

    mapListView.setItems(filteredMapBeans);
    mapListView.setCellFactory(param -> new StringListCell<>(MapBean::getDisplayName));
    mapListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setSelectedMap(newValue));
  }

  protected void setSelectedMap(MapBean newValue) {
    JavaFxUtil.assertApplicationThread();

    if (newValue == null) {
      mapNameLabel.setText("");
      return;
    }

    preferencesService.getPreferences().getLastGame().setLastMap(newValue.getFolderName());
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
    titleTextField.setText(Strings.nullToEmpty(preferencesService.getPreferences().getLastGame().getLastGameTitle()));
  }

  private void selectLastOrDefaultGameType() {
    JavaFxUtil.assertApplicationThread();
    String lastGameMod = preferencesService.getPreferences().getLastGame().getLastGameType();
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
    MapBean selectedMap = mapListView.getSelectionModel().getSelectedItem();
    onCloseButtonClicked();
    if (mapService.isOfficialMap(selectedMap)) {
      hostGame(selectedMap);
    } else {
      mapService.updateLatestVersionIfNecessary(selectedMap)
          .exceptionally(throwable -> {
            log.error("error when updating the map", throwable);
            hostGame(selectedMap);
            return null;
          })
          .thenAccept(this::hostGame);
    }
  }

  private void hostGame(MapBean map) {
    Set<String> mods = modManagerController.apply().stream()
        .map(ModVersion::getUid)
        .collect(Collectors.toSet());

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
        map.getFolderName(),
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
    Optional<MapBean> mapBeanOptional = mapListView.getItems().stream().filter(mapBean -> mapBean.getFolderName().equalsIgnoreCase(mapFolderName)).findAny();
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

  public void onMoreFiltersLabelClicked() {
    moreFiltersLabel.setVisible(false);
    additionalFiltersBox.setVisible(true);
  }
}
