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
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
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
import javafx.scene.control.Label;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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


  private final ThemeService themeService;
  private final MapService mapService;
  private final UiService uiService;
  private final PlayerService playerService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final I18n i18n;
  private final List<SVGPath> tokenViews = new ArrayList<>();

  private final MatchmakerPrefs matchmakerPrefs;

  private final DoubleProperty maxWidth = new SimpleDoubleProperty(0);
  private final DoubleProperty maxHeight = new SimpleDoubleProperty(0);
  private final BooleanProperty vetoModeEnabled = new SimpleBooleanProperty(false);
  private final ObjectProperty<Map<MatchmakerQueueMapPool, List<MapPoolAssignment>>> brackets = new SimpleObjectProperty<>(
      Map.of());
  private final IntegerProperty playerRating = new SimpleIntegerProperty();
  private final ObjectProperty<MatchmakerQueueInfo> queue = new SimpleObjectProperty<>();
  private final ObjectProperty<Integer> currentBracketIndex = new SimpleObjectProperty<>(null);

  private final ObservableValue<List<MatchmakerQueueMapPool>> sortedMapPools = brackets.map(this::getSortedMapPools)
                                                                                       .orElse(List.of());
  private final ObservableValue<MatchmakerQueueMapPool> currentBracket = Bindings.createObjectBinding(
      this::getCurrentBracket, sortedMapPools, currentBracketIndex);

  private final BooleanExpression hasVetoTokens = BooleanExpression.booleanExpression(
      currentBracket.map(bracket -> bracket.vetoTokensPerPlayer() > 0).orElse(false));
  private final BooleanBinding showVetoWallet = vetoModeEnabled.and(hasVetoTokens);

  private final ObservableValue<List<MapPoolAssignment>> currentBracketMaps = Bindings.createObjectBinding(
      this::getCurrentBracketMaps, currentBracket);

  private final ObservableValue<Integer> playerBracketIndex = Bindings.createObjectBinding(this::calculateBracketIndex,
                                                                                           sortedMapPools,
                                                                                           playerRating);

  private ObservableValue<Integer> vetoTokensApplied;
  private ObservableValue<Integer> vetoTokensLeft;
  private String VETO_ICON_SVG_PATH;

  public ComboBox<String> bracketComboBox;
  public Button applyVetoesButton;
  public Button vetoTokensWallet;
  public SVGPath applyVetoesSvg;
  public Label applyVetoesLabel;
  public Pane root;
  public TilePane tilesContainer;
  public ScrollPane scrollContainer;
  public VBox loadingPane;
  public VBox headerContainer;
  public HBox vetoTokensContainer;
  public HBox vetoTokensViewer;

  @Override
  protected void onInitialize() {
    this.getSvgPath();
    this.bindProperties();
  }

  private void getSvgPath() {
    try {
      URL url = URI.create(themeService.getThemeFile("theme/images/vector/veto_palm.svgpath")).toURL();
      try (InputStream inputStream = url.openStream()) {
        VETO_ICON_SVG_PATH = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
      }
    } catch (IOException e) {
      log.error("Failed to load veto icon", e);
      VETO_ICON_SVG_PATH = "";
    }
  }

  private void bindProperties() {
    JavaFxUtil.bindManagedToVisible(loadingPane);

    applyVetoesSvg.setContent(VETO_ICON_SVG_PATH);

    this.sortedMapPools.subscribe(pools -> {
      this.bracketComboBox.getItems().setAll(pools.stream().map(this::getBracketTitle).toList());
      this.bracketComboBox.getSelectionModel().select(Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0));
    });

    bracketComboBox.getSelectionModel().selectedIndexProperty().subscribe(index -> this.currentBracketIndex.set(index.intValue()));
    this.currentBracketIndex.subscribe(value -> bracketComboBox.getSelectionModel().select(value != null ? value : 0));

    tilesContainer.getChildren().subscribe(() -> this.loadingPane.setVisible(false));

    vetoTokensApplied = Bindings.createObjectBinding(this::calculateVetoTokensApplied, currentBracketMaps,
                                                     matchmakerPrefs.getAppliedVetoes(), currentBracket).when(showing);
    vetoTokensLeft = Bindings.createObjectBinding(
        () -> Optional.ofNullable(currentBracket.getValue())
                      .map(MatchmakerQueueMapPool::vetoTokensPerPlayer)
                      .orElse(0) - Optional.ofNullable(vetoTokensApplied.getValue()).orElse(0),
        currentBracket, vetoTokensApplied);

    this.queue.when(showing).subscribe(value -> {
      if (value == null) {
        return;
      }
      loadingPane.setVisible(true);
      mapService.getMatchmakerBrackets(value)
                .publishOn(fxApplicationThreadExecutor.asScheduler())
                .subscribe(this.brackets::setValue);
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

    applyVetoesButton.visibleProperty().bind(showVetoWallet.not());
    vetoTokensWallet.visibleProperty().bind(showVetoWallet);
    applyVetoesButton.managedProperty().bind(applyVetoesButton.visibleProperty());
    vetoTokensWallet.managedProperty().bind(vetoTokensWallet.visibleProperty());

    applyVetoesLabel.textProperty().bind(Bindings.when(hasVetoTokens)
        .then(i18n.get("teammatchmaking.applyVetoes"))
        .otherwise(i18n.get("teammatchmaking.noVetoes")));
    applyVetoesButton.disableProperty().bind(hasVetoTokens.not());
    applyVetoesSvg.visibleProperty().bind(hasVetoTokens);
    applyVetoesSvg.managedProperty().bind(hasVetoTokens);
  }

  @Override
  protected void onShow() {
    addShownSubscription(this.maxWidth.subscribe(this::resizeToContent));
    addShownSubscription(this.maxHeight.subscribe(this::resizeToContent));
    addShownSubscription(this.currentBracketMaps.subscribe(this::updateContent));
    addShownSubscription(this.currentBracket.subscribe(this::updateVetoes));
    addShownSubscription(this.vetoTokensApplied.subscribe(this::updateVetoes));

    if (!currentBracketMaps.getValue().isEmpty()) {
      updateContent();
    }
    if (vetoTokensApplied.getValue() != null) {
      updateVetoes();
    }
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
    Integer currentBracketIndexValue = this.currentBracketIndex.get();
    if ((currentBracketIndexValue == null) || (currentBracketIndexValue > this.sortedMapPools.getValue().size() - 1) || (currentBracketIndexValue < 0)) {
      return null;
    }

    return this.sortedMapPools.getValue().get(currentBracketIndexValue);
  }

  private String getBracketTitle(MatchmakerQueueMapPool bracket) {
    if (bracket == null) {
      return "";
    }

    Double min = bracket.minRating();
    Double max = bracket.maxRating();

    if (min == null && max == null) {
      return i18n.get("teammatchmaking.bracket.anyRating");
    }

    String rating = i18n.get("game.rating");

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
    if (getCurrentBracket() == null) {
      return 0;
    }
    int currentBracketId = getCurrentBracket().id();
    return matchmakerPrefs.getAppliedVetoes()
                          .entrySet()
                          .stream()
                          .filter(entry -> entry.getKey().matchmakerQueueMapPoolId() == currentBracketId)
                          .mapToInt(Entry::getValue)
                          .sum();
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
    controller.setMaxPerMap(currentBracket.getValue().maxTokensPerMap());
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
    MatchmakerQueueMapPool currentBracketValue = currentBracket.getValue();
    if (currentBracketValue == null) {
      fxApplicationThreadExecutor.execute(() -> this.vetoTokensViewer.getChildren().clear());
      return;
    }
    int maxTokens = currentBracketValue.vetoTokensPerPlayer();
    int usedTokens = vetoTokensApplied.getValue();

    fxApplicationThreadExecutor.execute(() -> {
      while (tokenViews.size() < maxTokens) {
        SVGPath token = new SVGPath();
        token.setContent(VETO_ICON_SVG_PATH);
        tokenViews.add(token);
      }

      while (tokenViews.size() > maxTokens) {
        tokenViews.remove(tokenViews.size() - 1);
      }

      for (int i = 0; i < maxTokens; i++) {
        tokenViews.get(i).setFill(i >= maxTokens - usedTokens ? VETO_TOKEN_USED_PAINT : VETO_TOKEN_AVAILABLE_PAINT);
      }

      this.vetoTokensViewer.getChildren().setAll(tokenViews.subList(0, maxTokens));
    });
  }

  private void updateContent() {
    List<Pane> mapTiles = currentBracketMaps.getValue().stream().map(this::createMapTile).toList();
    fxApplicationThreadExecutor.execute(() -> {
      this.tilesContainer.getChildren().setAll(mapTiles);
      this.resizeToContent();
    });
  }

  public void toggleVetoMode() {
    vetoModeEnabled.set(!vetoModeEnabled.get());
  }
}