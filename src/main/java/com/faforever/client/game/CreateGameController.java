package com.faforever.client.game;

import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapType;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.filter.MapFilterController;
import com.faforever.client.fx.DualStringListCell;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyLabelMenuItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModManagerController;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import com.faforever.commons.lobby.GameVisibility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javafx.scene.layout.BackgroundPosition.CENTER;
import static javafx.scene.layout.BackgroundRepeat.NO_REPEAT;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class CreateGameController extends NodeController<Pane> {

  public static final String STYLE_CLASS_DUAL_LIST_CELL = "create-game-dual-list-cell";
  public static final PseudoClass PSEUDO_CLASS_INVALID = PseudoClass.getPseudoClass("invalid");
  private static final int MAX_RATING_LENGTH = 4;

  private final GameRunner gameRunner;
  private final MapService mapService;
  private final FeaturedModService featuredModService;
  private final ModService modService;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final LoginService loginService;
  private final MapGeneratorService mapGeneratorService;
  private final UiService uiService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final LastGamePrefs lastGamePrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;


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
  public ListView<MapVersion> mapListView;
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
  FilteredList<MapVersion> filteredMaps;
  private Runnable onCloseButtonClickedListener;
  private MapFilterController mapFilterController;

  private final ObjectProperty<MapVersion> selectedMap = new SimpleObjectProperty<>();
  private final SimpleInvalidationListener createButtonStateListener = this::setCreateGameButtonState;

  @Override
  protected void onInitialize() {
    contextMenuBuilder.addCopyLabelContextMenu(mapDescriptionLabel);
    JavaFxUtil.bindManagedToVisible(versionLabel);
    JavaFxUtil.bind(mapPreviewPane.prefHeightProperty(), mapPreviewPane.widthProperty());
    modManagerController.setCloseable(false);
    mapSearchTextField.setOnKeyPressed(event -> {
      MultipleSelectionModel<MapVersion> selectionModel = mapListView.getSelectionModel();
      int currentMapIndex = selectionModel.getSelectedIndex();
      int newMapIndex = currentMapIndex;
      if (KeyCode.DOWN == event.getCode()) {
        if (filteredMaps.size() > currentMapIndex + 1) {
          newMapIndex++;
        }
      } else if (KeyCode.UP == event.getCode()) {
        if (currentMapIndex > 0) {
          newMapIndex--;
        }
      }
      selectionModel.select(newMapIndex);
      mapListView.scrollTo(newMapIndex);
      event.consume();
    });

    Function<FeaturedMod, String> isDefaultModString = mod -> Objects.equals(mod.technicalName(),
                                                                             KnownFeaturedMod.DEFAULT.getTechnicalName()) ? " " + i18n.get(
        "game.create.defaultGameTypeMarker") : null;

    featuredModListView.setCellFactory(
        param -> new DualStringListCell<>(FeaturedMod::displayName, isDefaultModString, FeaturedMod::description,
                                          STYLE_CLASS_DUAL_LIST_CELL, uiService,
                                          fxApplicationThreadExecutor));

    JavaFxUtil.makeNumericTextField(minRankingTextField, MAX_RATING_LENGTH, true);
    JavaFxUtil.makeNumericTextField(maxRankingTextField, MAX_RATING_LENGTH, true);

    featuredModService.getFeaturedMods()
                      .collectList()
                      .map(FXCollections::observableList)
                      .map(observableList -> observableList.filtered(FeaturedMod::visible))
                      .publishOn(fxApplicationThreadExecutor.asScheduler())
                      .subscribe(featuredModBeans -> {
                        featuredModListView.setItems(featuredModBeans);
                        selectLastOrDefaultGameType();
                      });

    bindGameVisibility();
    initMapSelection();
    initMapFilterPopup();
    initFeaturedModList();
    initRatingBoundaries();
    selectLastMap();
    setLastGameTitle();
    initPassword();
    JavaFxUtil.addAndTriggerListener(titleTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGamePrefs.setLastGameTitle(newValue);
      validateTitle(newValue);
    });

    JavaFxUtil.addAndTriggerListener(loginService.connectionStateProperty(),
                                     new WeakInvalidationListener(createButtonStateListener));
    JavaFxUtil.addListener(titleTextField.textProperty(), createButtonStateListener);
    JavaFxUtil.addListener(passwordTextField.textProperty(), createButtonStateListener);
    JavaFxUtil.addListener(featuredModListView.getSelectionModel().selectedItemProperty(),
                           new WeakInvalidationListener(createButtonStateListener));
  }

  public void onCloseButtonClicked() {
    onCloseButtonClickedListener.run();
  }

  @Override
  public void onShow() {
    addShownSubscription(() -> modManagerController.apply());
  }

  private void setCreateGameButtonState() {
    String title = titleTextField.getText();
    String password = passwordTextField.getText();

    ConnectionState lobbyConnectionState = loginService.getConnectionState();
    String createGameTextKey = switch (lobbyConnectionState) {
      case DISCONNECTED -> "game.create.disconnected";
      case CONNECTING -> "game.create.connecting";
      case CONNECTED -> {
        CharsetEncoder charsetEncoder = StandardCharsets.US_ASCII.newEncoder();
        if (StringUtils.isBlank(title)) {
          yield "game.create.titleMissing";
        } else if (!charsetEncoder.canEncode(title)) {
          yield "game.create.titleNotAscii";
        } else if (password != null && !charsetEncoder.canEncode(password)) {
          yield "game.create.passwordNotAscii";
        } else if (featuredModListView.getSelectionModel().getSelectedItem() == null) {
          yield "game.create.featuredModMissing";
        } else {
          yield "game.create.create";
        }
      }
    };

    fxApplicationThreadExecutor.execute(() -> {
      createGameButton.setDisable(!Objects.equals(createGameTextKey, "game.create.create"));
      createGameButton.setText(i18n.get(createGameTextKey));
    });
  }

  private void initMapFilterPopup() {
    mapFilterController = uiService.loadFxml("theme/filter/filter.fxml", MapFilterController.class);
    mapFilterController.addExternalFilter(mapSearchTextField.textProperty().when(showing),
                                          (text, mapVersion) -> text.isEmpty() || mapVersion.map().displayName()
                                                                                            .toLowerCase()
                                                                                            .contains(
                                                                                                text.toLowerCase()) || mapVersion.folderName()
                                                                                                                                 .toLowerCase()
                                                                                                                                 .contains(
                                                                                                                                     text.toLowerCase()));
    mapFilterController.completeSetting();

    mapFilterController.filterActiveProperty().when(showing).subscribe(mapFilterButton::setSelected);
    mapFilterButton.selectedProperty()
                   .when(showing)
                   .subscribe(() -> mapFilterButton.setSelected(mapFilterController.getFilterActive()));
    filteredMaps.predicateProperty().bind(mapFilterController.predicateProperty().when(showing));
  }

  private void validateTitle(String gameTitle) {
    titleTextField.pseudoClassStateChanged(PSEUDO_CLASS_INVALID,
                                           StringUtils.isBlank(gameTitle) || !StandardCharsets.US_ASCII.newEncoder()
                                                                                                       .canEncode(
                                                                                                           gameTitle));
  }

  private void initPassword() {
    passwordTextField.setText(lastGamePrefs.getLastGamePassword());
    JavaFxUtil.addListener(passwordTextField.textProperty(), (observable, oldValue, newValue) -> {
      lastGamePrefs.setLastGamePassword(newValue);
    });
  }

  private void bindGameVisibility() {
    onlyForFriendsCheckBox.selectedProperty().bindBidirectional(lastGamePrefs.lastGameOnlyFriendsProperty());
  }

  protected void initMapSelection() {
    mapNameLabel.textProperty().bind(selectedMap.map(MapVersion::map).map(Map::displayName).when(showing));
    mapSizeLabel.textProperty().bind(selectedMap.map(MapVersion::size)
                                                .map(mapSize -> i18n.get("mapPreview.size", mapSize.widthInKm(),
                                                                         mapSize.heightInKm()))
                                 .when(showing));
    BackgroundSize backgroundSize = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true,
                                                       false);
    mapPreviewPane.backgroundProperty().bind(selectedMap.map(MapVersion::folderName)
                                   .map(folderName -> mapService.loadPreview(folderName, PreviewSize.LARGE))
                                   .map(preview -> new BackgroundImage(preview, NO_REPEAT, NO_REPEAT, CENTER,
                                                                       backgroundSize))
                                   .map(Background::new)
                                   .when(showing));
    mapPlayersLabel.textProperty().bind(selectedMap.map(MapVersion::maxPlayers).map(i18n::number).when(showing));
    mapDescriptionLabel.textProperty().bind(selectedMap.map(MapVersion::description)
                                        .map(Strings::emptyToNull)
                                        .map(FaStrings::removeLocalizationTag)
                                        .orElse(i18n.get("map.noDescriptionAvailable"))
                                        .when(showing));
    versionLabel.textProperty().bind(selectedMap.map(MapVersion::version)
                                 .map(version -> i18n.get("versionFormat", version))
                                 .when(showing));
    versionLabel.visibleProperty().bind(versionLabel.textProperty().isNotEmpty());

    mapListView.setCellFactory(
        param -> new StringListCell<>(mapVersion -> mapVersion.map().displayName(), fxApplicationThreadExecutor));
    mapListView.getSelectionModel().selectedItemProperty().when(showing).subscribe((oldItem, newItem) -> {
      if (newItem == null && filteredMaps.contains(oldItem)) {
        mapListView.getSelectionModel().select(oldItem);
      } else {
        lastGamePrefs.setLastMap(newItem != null ? newItem.folderName() : null);
        selectedMap.set(newItem);
      }
    });

    FilteredList<MapVersion> skirmishMaps = mapService.getInstalledMaps()
                                                      .filtered(
                                                          mapVersion -> mapVersion.map().mapType() == MapType.SKIRMISH);
    filteredMaps = new FilteredList<>(skirmishMaps.sorted(
        Comparator.comparing(mapVersion -> mapVersion.map().displayName(), String.CASE_INSENSITIVE_ORDER)));
    skirmishMaps.addListener((ListChangeListener<MapVersion>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          MapVersion map = change.getAddedSubList().getFirst();
          fxApplicationThreadExecutor.execute(() -> {
            mapListView.getSelectionModel().select(map);
            mapListView.scrollTo(map);
          });
        }
      }
    });

    mapListView.setItems(filteredMaps);
  }

  private void initFeaturedModList() {
    featuredModListView.getSelectionModel().selectedItemProperty().map(FeaturedMod::technicalName)
                       .when(showing)
                       .subscribe(lastGamePrefs::setLastGameType);
  }

  private void initRatingBoundaries() {
    Integer lastGameMinRating = lastGamePrefs.getLastGameMinRating();
    Integer lastGameMaxRating = lastGamePrefs.getLastGameMaxRating();

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

      lastGamePrefs.setLastGameMinRating(minRating);
    });

    maxRankingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      Integer maxRating = null;
      if (!newValue.isEmpty()) {
        maxRating = Integer.parseInt(newValue);
      }
      lastGamePrefs.setLastGameMaxRating(maxRating);
    });

    enforceRankingCheckBox.selectedProperty().bindBidirectional(lastGamePrefs.lastGameEnforceRatingProperty());
  }

  private void selectLastMap() {
    String lastMap = lastGamePrefs.getLastMap();
    for (MapVersion mapVersion : mapListView.getItems()) {
      if (mapVersion.folderName().equalsIgnoreCase(lastMap)) {
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
    titleTextField.setText(Strings.nullToEmpty(lastGamePrefs.getLastGameTitle()));
  }

  private void selectLastOrDefaultGameType() {
    JavaFxUtil.assertApplicationThread();
    String lastGameMod = lastGamePrefs.getLastGameType();
    if (lastGameMod == null) {
      lastGameMod = KnownFeaturedMod.DEFAULT.getTechnicalName();
    }

    for (FeaturedMod mod : featuredModListView.getItems()) {
      if (Objects.equals(mod.technicalName(), lastGameMod)) {
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
    GenerateMapController generateMapController = uiService.loadFxml("theme/play/generate_map.fxml");
    mapGeneratorService.getNewestGenerator()
                       .then(Mono.defer(() -> Mono.when(mapGeneratorService.getGeneratorSymmetries()
                                                                           .publishOn(
                                                                               fxApplicationThreadExecutor.asScheduler())
                                                                           .doOnNext(generateMapController::setSymmetries),
                                                        mapGeneratorService.getGeneratorStyles()
                                                                           .publishOn(
                                                                               fxApplicationThreadExecutor.asScheduler())
                                                                           .doOnNext(generateMapController::setStyles),
                                                        mapGeneratorService.getGeneratorTerrainStyles()
                                                                           .publishOn(
                                                                               fxApplicationThreadExecutor.asScheduler())
                                                                           .doOnNext(
                                                                               generateMapController::setTerrainStyles),
                                                        mapGeneratorService.getGeneratorTextureStyles()
                                                                           .publishOn(
                                                                               fxApplicationThreadExecutor.asScheduler())
                                                                           .doOnNext(
                                                                               generateMapController::setTextureStyles),
                                                        mapGeneratorService.getGeneratorResourceStyles()
                                                                           .publishOn(
                                                                               fxApplicationThreadExecutor.asScheduler())
                                                                           .doOnNext(
                                                                               generateMapController::setResourceStyles),
                                                        mapGeneratorService.getGeneratorPropStyles()
                                                                           .publishOn(
                                                                               fxApplicationThreadExecutor.asScheduler())
                                                                           .doOnNext(
                                                                               generateMapController::setPropStyles))))
                       .publishOn(fxApplicationThreadExecutor.asScheduler())
                       .subscribe(null, throwable -> {
                         log.error("Opening map generation ui failed", throwable);
                         notificationService.addImmediateErrorNotification(throwable,
                                                                           "mapGenerator.generationUIFailed");
                       }, () -> {
                         Pane root = generateMapController.getRoot();
                         Dialog dialog = uiService.showInDialog(gamesRoot, root, i18n.get("game.generateMap.dialog"));
                         generateMapController.setOnCloseButtonClickedListener(dialog::close);

                         root.requestFocus();
                       });
  }

  public void onCreateButtonClicked() {
    onCloseButtonClicked();
    hostGameAfterMapAndModUpdate();
  }

  private void hostGameAfterMapAndModUpdate() {
    MapVersion selectedMap = mapListView.getSelectionModel().getSelectedItem();
    Collection<ModVersion> selectedModVersions = modManagerController.getSelectedModVersions();

    CompletableFuture<MapVersion> mapUpdateFuture = mapService.updateLatestVersionIfNecessary(selectedMap)
                                                              .doOnError(throwable -> log.error(
                                                                      "Error when updating the map", throwable))
                                                              .onErrorReturn(selectedMap)
                                                              .toFuture();
    CompletableFuture<List<ModVersion>> modUpdateFuture = modService.updateAndActivateModVersions(
        selectedModVersions).onErrorResume(throwable -> {
      log.error("Error when updating selected mods", throwable);
      notificationService.addImmediateErrorNotification(throwable, "game.create.errorUpdatingMods");
      return Mono.just(List.copyOf(selectedModVersions));
    }).toFuture();

    mapUpdateFuture.thenAcceptBoth(modUpdateFuture, this::hostGame).exceptionally(throwable -> {
      throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
      log.error("Game could not be hosted", throwable);
      if (throwable instanceof NotifiableException notifiableException) {
        notificationService.addErrorNotification(notifiableException);
      } else {
        notificationService.addImmediateErrorNotification(throwable, "game.create.failed");
      }
      return null;
    });

  }

  @NotNull
  private Set<String> getUUIDsFromModVersions(Collection<ModVersion> modVersions) {
    return modVersions.stream().map(ModVersion::uid).collect(Collectors.toSet());
  }

  private void hostGame(MapVersion mapVersion, Collection<ModVersion> mods) {
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

    String featuredModName = featuredModListView.getSelectionModel().getSelectedItem().technicalName();
    NewGameInfo newGameInfo = new NewGameInfo(titleTextField.getText().trim(),
                                              Strings.emptyToNull(passwordTextField.getText()), featuredModName,
                                              mapVersion.folderName(), getUUIDsFromModVersions(mods),
                                              onlyForFriendsCheckBox.isSelected() ? GameVisibility.PRIVATE : GameVisibility.PUBLIC,
                                              minRating, maxRating, enforceRating);

    gameRunner.host(newGameInfo);
  }

  @Override
  public Pane getRoot() {
    return createGameRoot;
  }

  public void setGamesRoot(StackPane root) {
    gamesRoot = root;
  }

  /**
   * @return returns true if the map was found and false if not
   */
  boolean selectMap(String mapFolderName) {
    Optional<MapVersion> mapBeanOptional = mapListView.getItems()
                                                      .stream()
                                                      .filter(mapBean -> mapBean.folderName()
                                                                                    .equalsIgnoreCase(mapFolderName))
                                                      .findAny();
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
    if (mapFilterPopup == null) {
      mapFilterPopup = PopupUtil.createPopup(AnchorLocation.CONTENT_TOP_RIGHT, mapFilterController.getRoot());
    }

    if (mapFilterPopup.isShowing()) {
      mapFilterPopup.hide();
    } else {
      Bounds screenBounds = mapFilterButton.localToScreen(mapFilterButton.getBoundsInLocal());
      mapFilterPopup.show(mapFilterButton.getScene().getWindow(), screenBounds.getMinX() - 10, screenBounds.getMinY());
    }
  }

  public void onMapPreviewImageClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
      Optional.ofNullable(mapListView.getSelectionModel())
              .map(SelectionModel::getSelectedItem)
              .map(MapVersion::folderName)
              .ifPresent(mapName -> PopupUtil.showImagePopup(mapService.loadPreview(mapName, PreviewSize.LARGE)));
    }
  }

  public void onMapNameLabelContextMenuRequest(ContextMenuEvent contextMenuEvent) {
    // No other way to make same design without using StackPane class therefore we use this dirty hack
    if (mapNameLabel.getBoundsInParent().contains(contextMenuEvent.getX(), contextMenuEvent.getY())) {
      contextMenuBuilder.newBuilder()
                        .addItem(CopyLabelMenuItem.class, mapNameLabel)
                        .build()
                        .show(mapNameLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
    }
  }
}
