package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.lobby.VetoData;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor

public class TeamMatchmakingMapListController extends NodeController<Pane> {

  private static final int TILE_SIZE = 180;
  private static final int PADDING = 20;
  private static final Paint VETO_TOKEN_USED_PAINT = Paint.valueOf("#000000");
  private static final Paint VETO_TOKEN_AVAILABLE_PAINT = Paint.valueOf("#ffffff");

  public static final String VETO_ICON_SVG_PATH = "M 11.8068 1.8587 L 11.8067 2.4721 L 11.8058 9.8249 L 13.3528 8.6739 C 14.0631 8.1453 15.0404 8.1616 15.7326 8.7136 L 17.008 9.7306 L 17.4413 10.0761 L 17.1686 10.5586 L 14.2707 15.6856 L 13.7992 16.5846 C 12.4816 19.0965 9.7937 20.5816 6.9659 20.3599 C 3.245 20.0681 0.3874 16.9418 0.4302 13.2098 L 0.5402 3.6419 L 0.547 3.0517 L 1.1351 3.0017 L 3.831 2.7726 L 3.9211 0.9443 L 3.9506 0.347 L 4.5483 0.3267 L 7.8067 0.2164 L 8.4872 0.1933 L 8.4786 0.8742 L 8.4687 1.665 L 11.1944 1.8232 L 11.8068 1.8587 Z M 8.4523 2.9663 L 8.3681 9.6469 L 7.0682 9.6305 L 7.1609 2.2698 L 7.1702 1.5387 L 5.1901 1.6057 L 5.1017 3.3995 L 4.8833 9.6389 L 3.5841 9.5935 L 3.777 4.0819 L 1.8333 4.2471 L 1.7302 13.2247 C 1.6951 16.2725 4.0288 18.8256 7.0675 19.0639 C 9.3769 19.2449 11.572 18.0322 12.6479 15.9807 L 13.1242 15.0728 L 13.1289 15.0638 L 13.1339 15.0548 L 15.7641 10.4014 L 14.9221 9.73 C 14.6914 9.546 14.3656 9.5406 14.1289 9.7168 L 11.5437 11.6405 L 10.5057 11.1189 L 10.5067 3.0854 L 8.4523 2.9663 Z";

  private final List<SVGPath> tokenViews = new ArrayList<>();

  private static final Comparator<MapVersion> MAP_VERSION_COMPARATOR = Comparator.nullsFirst(
                                                                                     Comparator.comparing(MapVersion::size))
                                                                                 .thenComparing(
                                                                                     mapVersion -> mapVersion.map()
                                                                                                             .displayName(),
                                                                                     String.CASE_INSENSITIVE_ORDER);

  private static final Comparator<MatchmakerQueueMapPool> MAP_POOL_COMPARATOR = Comparator.comparing(
                                                                                              MatchmakerQueueMapPool::minRating, Comparator.nullsFirst(Double::compare))
                                                                                          .thenComparing(
                                                                                              MatchmakerQueueMapPool::maxRating,
                                                                                              Comparator.nullsLast(
                                                                                                  Double::compare));


  private final MapService mapService;
  private final UiService uiService;
  private final PlayerService playerService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final I18n i18n;

  private final MatchmakerPrefs matchmakerPrefs;

  private final DoubleProperty maxWidth = new SimpleDoubleProperty(0);
  private final DoubleProperty maxHeight = new SimpleDoubleProperty(0);
  private final SimpleBooleanProperty vetoModeEnabled = new SimpleBooleanProperty(false);
  private final ObjectProperty<Map<MatchmakerQueueMapPool, List<MapPoolAssignment>>> brackets = new SimpleObjectProperty<>(
      Map.of());
  private final IntegerProperty playerRating = new SimpleIntegerProperty();
  private final ObjectProperty<MatchmakerQueueInfo> queue = new SimpleObjectProperty<>();
  private final ObjectProperty<Integer> currentBracketIndex = new SimpleObjectProperty<>(null);

  private final ObservableValue<List<MatchmakerQueueMapPool>> sortedMapPools = brackets.map(this::getSortedMapPools)
                                                                                       .orElse(List.of());
  private final ObservableValue<MatchmakerQueueMapPool> currentBracket = Bindings.createObjectBinding(
      this::getCurrentBracket, sortedMapPools, currentBracketIndex);

  private final ObservableValue<List<MapPoolAssignment>> currentBracketMaps = Bindings.createObjectBinding(
      this::getCurrentBracketMaps, currentBracket);

  private final ObservableValue<Integer> playerBracketIndex = Bindings.createObjectBinding(this::calculateBracketIndex,
                                                                                           sortedMapPools,
                                                                                           playerRating);
  public ComboBox<String> bracketComboBox;
  public Button applyVetoesButton;
  public Button vetoTokensWallet;

  private ObservableValue<Integer> vetoTokensApplied;
  private ObservableValue<Integer> vetoTokensLeft;

  public Pane root;
  public TilePane tilesContainer;
  public ScrollPane scrollContainer;
  public VBox loadingPane;
  public VBox headerContainer;
  public HBox vetoTokensContainer;
  public HBox vetoTokensViewer;

  @Override
  protected void onInitialize() {
    this.bindProperties();
  }

  private void bindProperties() {
    JavaFxUtil.bindManagedToVisible(loadingPane);

    this.sortedMapPools.subscribe(pools -> {
      this.bracketComboBox.getItems().setAll(pools.stream().map(this::getBracketTitle).toList());
      this.bracketComboBox.getSelectionModel().select(Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0));
    });

    bracketComboBox.getSelectionModel().selectedIndexProperty().subscribe(index -> this.currentBracketIndex.set(index.intValue()));
    this.currentBracketIndex.addListener(v -> bracketComboBox.getSelectionModel().select((Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0))));

    tilesContainer.getChildren().subscribe(() -> this.loadingPane.setVisible(false));

    vetoTokensApplied = Bindings.createObjectBinding(this::calculateVetoTokensApplied, currentBracketMaps,
                                                     matchmakerPrefs.getAppliedVetoes(), currentBracket);
    vetoTokensLeft = Bindings.createObjectBinding(
        () -> Optional.ofNullable(currentBracket.getValue())
                      .map(MatchmakerQueueMapPool::vetoTokensPerPlayer)
                      .orElse(0) - Optional.ofNullable(vetoTokensApplied.getValue()).orElse(0),
        currentBracket, vetoTokensApplied);

    this.currentBracket.subscribe(this::updateContent);
    this.currentBracketMaps.when(showing).subscribe(this::updateContent);
    this.currentBracketIndex.subscribe(this::updateContent);

    currentBracket.subscribe(this::updateVetoes);
    vetoTokensApplied.subscribe(this::updateVetoes);

    this.queue.when(showing).subscribe(value -> {
      if (value == null) {
        return;
      }
      loadingPane.setVisible(true);
      mapService.getMatchmakerBrackets(value).subscribe(this.brackets::setValue);
    });
    this.playerBracketIndex.subscribe(currentBracketIndex::set);

    this.sortedMapPools.subscribe(pools -> {
      if (!pools.isEmpty()) {
        this.currentBracketIndex.setValue(Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0));
      }
    });

    playerRating.bind(playerService.currentPlayerProperty()
                                   .flatMap(player -> queue.flatMap(MatchmakerQueueInfo::leaderboardProperty)
                                                           .map(leaderboard -> RatingUtil.getLeaderboardRating(player,
                                                                                                               leaderboard)))
                                   .orElse(0)
                                   .when(showing));

    applyVetoesButton.visibleProperty().bind(vetoModeEnabled.not());
    vetoTokensWallet.visibleProperty().bind(vetoModeEnabled);
    applyVetoesButton.managedProperty().bind(vetoModeEnabled.not());
    vetoTokensWallet.managedProperty().bind(vetoModeEnabled);
  }

  @Override
  protected void onShow() {
    addShownSubscription(this.maxWidth.subscribe(this::resizeToContent));
    addShownSubscription(this.maxHeight.subscribe(this::resizeToContent));
  }

  @Override
  public Pane getRoot() {
    return root;
  }

  public double getMaxWidth() {
    return this.maxWidth.get();
  }

  public void setMaxWidth(double value) {
    this.maxWidth.set(value);
  }

  public DoubleProperty maxWidthProperty() {
    return this.maxWidth;
  }

  public double getMaxHeight() {
    return this.maxHeight.get();
  }

  public void setMaxHeight(double value) {
    this.maxHeight.set(value);
  }

  public DoubleProperty maxHeightProperty() {
    return this.maxHeight;
  }

  public MatchmakerQueueInfo getQueue() {
    return this.queue.get();
  }

  public void setQueue(MatchmakerQueueInfo queue) {
    this.queue.set(queue);
  }

  public ObjectProperty<MatchmakerQueueInfo> queueProperty() {
    return this.queue;
  }

  private MatchmakerQueueMapPool getCurrentBracket() {
    Integer v = this.currentBracketIndex.get();
    if ((v == null) || (v > this.sortedMapPools.getValue().size() - 1) || (v < 0)) {
      return null;
    }

    return this.sortedMapPools.getValue().get(this.currentBracketIndex.get());
  }

  private String getBracketTitle(MatchmakerQueueMapPool bracket) {
    if (bracket == null) {
      return "";
    }

    Double min = bracket.minRating(), max = bracket.maxRating();
    String rating = i18n.get("game.rating");

    if (min == null && max == null) {
      return i18n.get("teammatchmaking.bracket.anyRating");
    }

    if (min == null) {
      return rating + " < " + Math.round(Math.ceil(max));
    }

    if (max == null) {
      return rating + " > " + Math.round(Math.floor(min));
    }
    return rating + " " + Math.round(bracket.minRating()) + " - " + Math.round(bracket.maxRating());
  }

  private List<MapPoolAssignment> getCurrentBracketMaps() {
    if (currentBracket.getValue() == null) {
      return Collections.emptyList();
    }

    return this.brackets.getValue()
                        .get(currentBracket.getValue())
                        .stream()
                        .sorted((m1, m2) -> MAP_VERSION_COMPARATOR.compare(m1.mapVersion(), m2.mapVersion()))
                        .toList();
  }


  private List<MatchmakerQueueMapPool> getSortedMapPools(
      Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets) {
    return brackets.keySet().stream().sorted(MAP_POOL_COMPARATOR).toList();
  }

  private Integer calculateVetoTokensApplied() {
    if (matchmakerPrefs == null || getCurrentBracket() == null) {
      return 0;
    }
    List<VetoData> vetoesInCurrentBracket = matchmakerPrefs.getAppliedVetoes()
                                                           .stream()
                                                           .filter(
                                                               (data) -> data.getMatchmakerQueueMapPoolId() == getCurrentBracket().id())
                                                           .toList();
    return vetoesInCurrentBracket.stream().mapToInt(VetoData::getVetoTokensApplied).sum();
  }

  private Integer calculateBracketIndex() {
    int rating = playerRating.get();
    List<MatchmakerQueueMapPool> pools = this.sortedMapPools.getValue();
    return pools.stream()
                .filter(pool -> pool.minRating() == null || pool.minRating() < rating)
                .filter(pool -> pool.maxRating() == null || pool.maxRating() > rating)
                .findFirst()
                .map(pools::indexOf)
                .orElse(null);
  }

  private Pane createMapTile(MapPoolAssignment mapAssignment) {
    TeamMatchmakingMapTileController controller = uiService.loadFxml(
        "theme/play/teammatchmaking/matchmaking_map_tile.fxml");
    controller.setVetoIconPath(VETO_ICON_SVG_PATH);
    controller.setMapAssignment(mapAssignment);
    controller.setVetoTokensMax(currentBracket.getValue().vetoTokensPerPlayer());
    controller.setVetoTokensLeft(vetoTokensLeft);
    controller.setVetoModeEnabled(vetoModeEnabled);
    controller.bindVetoesBoxProperties();

    return controller.getRoot();
  }

  private void resizeToContent() {
    int tilecount = this.brackets.getValue().values().stream().map(List::size).reduce(0, Integer::max);
    if (tilecount == 0) {
      return;
    }

    double hgap = tilesContainer.getHgap();
    double vgap = tilesContainer.getVgap();

    double tileHSize = TILE_SIZE + hgap;
    double tileVSize = TILE_SIZE + vgap;

    int maxTilesInLine = (int) Math.min(10, Math.floor((getMaxWidth() * 0.95 - PADDING * 2 + hgap) / tileHSize));
    int maxLinesWithoutScroll = (int) Math.floor((getMaxHeight() * 0.95 - PADDING * 2 + vgap) / tileVSize);

    int tilesInOneLine = Math.min(maxTilesInLine,
                                  Math.max(Math.max(4, Math.ceilDiv(tilecount, Math.max(1, maxLinesWithoutScroll))),
                                           (int) Math.ceil(Math.sqrt(tilecount))));
    int lineCount = (int) Math.ceilDiv(tilecount, tilesInOneLine);

    tilesContainer.setPrefColumns(tilesInOneLine);
    tilesContainer.setMinHeight(lineCount * tileVSize - vgap);

  }

  private void updateVetoes() {
    MatchmakerQueueMapPool v = currentBracket.getValue();
    if (v == null) {
      fxApplicationThreadExecutor.execute(() -> this.vetoTokensViewer.getChildren().clear());
      return;
    }
    int maxTokens = v.vetoTokensPerPlayer();
    int usedTokens = vetoTokensApplied.getValue();

    while (tokenViews.size() < maxTokens) {
      SVGPath token = new SVGPath();
      token.setContent(VETO_ICON_SVG_PATH);
      tokenViews.add(token);
    }

    for (int i = 0; i < maxTokens; i++) {
      tokenViews.get(i).setFill(i >= maxTokens - usedTokens ? VETO_TOKEN_USED_PAINT : VETO_TOKEN_AVAILABLE_PAINT);
    }

    fxApplicationThreadExecutor.execute(() ->
      this.vetoTokensViewer.getChildren().setAll(tokenViews.subList(0, maxTokens))
    );
  }

  private void updateContent() {
    List<Pane> mapTiles = currentBracketMaps.getValue().stream().map(this::createMapTile).toList();
    fxApplicationThreadExecutor.execute(() -> {
      this.tilesContainer.getChildren().setAll(mapTiles);
      this.resizeToContent();
    });
  }

  public void changeMode() {
    vetoModeEnabled.set(!vetoModeEnabled.get());
  }
}