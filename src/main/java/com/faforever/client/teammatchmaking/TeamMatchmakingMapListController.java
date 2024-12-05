package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor

public class TeamMatchmakingMapListController extends NodeController<Pane> {

  private static final int TILE_SIZE = 160;
  private static final int PADDING = 20;

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

  private final MatchmakerPrefs matchmakerPrefs;

  private final DoubleProperty maxWidth = new SimpleDoubleProperty(0);
  private final DoubleProperty maxHeight = new SimpleDoubleProperty(0);
  private final ObjectProperty<Map<MatchmakerQueueMapPool, List<MapPoolAssignment>>> brackets = new SimpleObjectProperty<>(
      Map.of());
  private final IntegerProperty playerRating = new SimpleIntegerProperty();
  private final ObjectProperty<MatchmakerQueueInfo> queue = new SimpleObjectProperty<>();
  private final ObjectProperty<Integer> currentBracketIndex = new SimpleObjectProperty<>(null);

  private final ObservableValue<List<MatchmakerQueueMapPool>> sortedMapPools = brackets.map(this::getSortedMapPools)
                                                                                       .orElse(List.of());

  private final ObservableValue<MatchmakerQueueMapPool> currentBracket = Bindings.createObjectBinding(this::getCurrentBracket, sortedMapPools, currentBracketIndex);
  private final ObservableValue<List<MapPoolAssignment>> currentBracketMaps = Bindings.createObjectBinding(this::getCurrentBracketMaps, currentBracket);

  private final ObservableValue<Integer> playerBracketIndex = Bindings.createObjectBinding(this::calculateBracketIndex,
                                                                                           sortedMapPools,
                                                                                           playerRating);

  private ObservableValue<Integer> vetoTokensApplied;
  private ObservableValue<Integer> vetoTokensLeft;

  public Pane root;
  public TilePane tilesContainer;
  public ScrollPane scrollContainer;
  public VBox loadingPane;
  public Label bracketTitle;
  public Button prevBracketButton;
  public Button nextBracketButton;
  public VBox headerContainer;
  public HBox vetoTokensContainer;
  public HBox vetoTokensViewer;

  @Override
  protected void onInitialize() {
    this.bindProperties();
  }

  private void bindProperties() {
    JavaFxUtil.bindManagedToVisible(loadingPane);
    tilesContainer.getChildren().subscribe(()->this.loadingPane.setVisible(false));

    vetoTokensApplied = Bindings.createObjectBinding(this::calculateVetoTokensApplied, currentBracketMaps, matchmakerPrefs.getAppliedVetoes(), currentBracket);
    vetoTokensLeft = Bindings.createObjectBinding(()-> Optional.ofNullable(currentBracket.getValue().vetoTokensPerPlayer()).orElse(0) - Optional.ofNullable(vetoTokensApplied.getValue()).orElse(0), currentBracket, vetoTokensApplied);

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

    playerRating.bind(playerService.currentPlayerProperty()
                                   .flatMap(player -> queue.flatMap(MatchmakerQueueInfo::leaderboardProperty)
                                                           .map(leaderboard -> RatingUtil.getLeaderboardRating(player,
                                                                                                               leaderboard)))
                                   .orElse(0)
                                   .when(showing));

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

  private void setBracketTitle() {
    MatchmakerQueueMapPool bracket = currentBracket.getValue();

    if (bracket == null) {
      bracketTitle.setText("");
      return;
    }

    Double min = bracket.minRating(), max = bracket.maxRating();
    if (min == null && max == null) {
      bracketTitle.setText("Any rating");
      return;
    }

    if (min == null) {
      bracketTitle.setText("Rating < " + Math.round(Math.ceil(max)));
      return;
    }

    if (max == null) {
      bracketTitle.setText("Rating > " + Math.round(Math.floor(min)));
      return;
    }
    bracketTitle.setText("Rating " + Math.round(bracket.minRating()) + " - " + Math.round(bracket.maxRating()));
  }

  private List<MapPoolAssignment> getCurrentBracketMaps() {
    if (currentBracket.getValue() == null) {
      return Collections.emptyList();
    }

    return this.brackets.getValue().get(currentBracket.getValue()).stream()
                                          .sorted((m1, m2) -> MAP_VERSION_COMPARATOR.compare(m1.mapVersion(), m2.mapVersion()))
                                          .toList();
  }


  private List<MatchmakerQueueMapPool> getSortedMapPools(Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets) {
    return brackets.keySet().stream().sorted(MAP_POOL_COMPARATOR).toList();
  }

  private Integer calculateVetoTokensApplied() {
    if (matchmakerPrefs == null) {
      return 0;
    }
    Set<Integer> IDs = currentBracketMaps.getValue().stream().map(MapPoolAssignment::id).collect(Collectors.toSet());
    List<VetoData> vetoesInCurrentBracket = matchmakerPrefs.getAppliedVetoes().stream().filter((data) -> IDs.contains(data.getMapPoolMapVersionId())).toList();

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
    controller.setMapAssignment(mapAssignment);
    log.debug("HERE)))))))))))))))000000000000000000000000");
    log.debug(currentBracket.getValue().toString());
    controller.setVetoTokensMax(Math.min(currentBracket.getValue().vetoTokensPerPlayer(), 5));
    controller.setVetoTokensLeft(vetoTokensLeft);
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
      fxApplicationThreadExecutor.execute(()->this.vetoTokensViewer.getChildren().clear());
      return;
    }
    int maxTokens = v.vetoTokensPerPlayer();
    int usedTokens = vetoTokensApplied.getValue();

    List<SVGPath> tokens = new ArrayList<>();
    for (int i = 0; i < maxTokens; i++) {
      SVGPath token = new SVGPath();
      token.setContent("M18.148 12.48l5.665-5.66c1.563-1.56 1.563-4.1 0-5.66-1.565-1.57-4.101-1.57-5.665 0l-5.664 5.66L6.82 1.16c-1.563-1.57-4.099-1.57-5.664 0-1.563 1.56-1.563 4.1 0 5.66l5.664 5.66-5.664 5.67c-1.563 1.56-1.563 4.1 0 5.66 1.565 1.57 4.101 1.57 5.664 0l5.664-5.66 5.664 5.66c1.564 1.57 4.1 1.57 5.665 0 1.563-1.56 1.563-4.1 0-5.66l-5.665-5.67");
      if (i >= maxTokens - usedTokens)
        token.setFill(Paint.valueOf("#000000"));
      else
        token.setFill(Paint.valueOf("#ff0000"));
      tokens.add(token);
    }
    fxApplicationThreadExecutor.execute(() -> this.vetoTokensViewer.getChildren().setAll(tokens));
  }

  private void updateContent() {
    List<Pane> mapTiles = currentBracketMaps.getValue().stream().map(this::createMapTile).toList();

    fxApplicationThreadExecutor.execute(() -> {
      this.setBracketTitle();
      this.tilesContainer.getChildren().setAll(mapTiles);
      this.resizeToContent();
    });
  }

  public void goToPreviousBracket(ActionEvent actionEvent) {
    this.currentBracketIndex.set(Math.max(this.currentBracketIndex.get() - 1, 0));
  }

  public void goToNextBracket(ActionEvent actionEvent) {
    this.currentBracketIndex.set(Math.min(this.currentBracketIndex.get() + 1, this.sortedMapPools.getValue().size() - 1));
  }
}