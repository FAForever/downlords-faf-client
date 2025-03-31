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
import com.faforever.client.util.DeepCopyUtil;
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
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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

  private static final int TILE_SIZE = 180;
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
  private final ObservableValue<MatchmakerQueueMapPool> currentBracket = Bindings.createObjectBinding(
      this::getCurrentBracket, sortedMapPools, currentBracketIndex);

  private final ObservableValue<List<MapPoolAssignment>> currentBracketMaps = Bindings.createObjectBinding(
      this::getCurrentBracketMaps, currentBracket);

  private final ObservableValue<Integer> playerBracketIndex = Bindings.createObjectBinding(this::calculateBracketIndex,
                                                                                           sortedMapPools,
                                                                                           playerRating);
  public ComboBox<String> bracketComboBox;

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
    this.currentBracketIndex.subscribe(currentBracketIndex -> {
      log.debug("CURRENT BRACKET INDEX: {}", currentBracketIndex);
    });

    this.sortedMapPools.subscribe(pools -> {
      this.bracketComboBox.getItems().setAll(pools.stream().map(this::getBracketTitle).toList());
      this.bracketComboBox.getSelectionModel().select(Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0));
    });

    bracketComboBox.getSelectionModel().selectedIndexProperty().subscribe(index -> this.currentBracketIndex.set(index.intValue()));
    this.currentBracketIndex.addListener(v->bracketComboBox.getSelectionModel().select((Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0))));

    tilesContainer.getChildren().subscribe(() -> this.loadingPane.setVisible(false));

    vetoTokensApplied = Bindings.createObjectBinding(this::calculateVetoTokensApplied, currentBracketMaps,
                                                     matchmakerPrefs.getAppliedVetoes(), currentBracket);
    vetoTokensLeft = Bindings.createObjectBinding(
        () -> Optional.ofNullable(currentBracket.getValue().vetoTokensPerPlayer()).orElse(0) - Optional.ofNullable(
            vetoTokensApplied.getValue()).orElse(0), currentBracket, vetoTokensApplied);

    this.currentBracket.subscribe(this::updateContent);
    this.currentBracketMaps.when(showing).subscribe(this::updateContent);
    this.currentBracketIndex.subscribe(this::updateContent);

    currentBracket.subscribe(this::updateVetoes);
    vetoTokensApplied.subscribe(this::updateVetoes);

    log.debug("INITIALLLLLLLLLLIIIIIIIIIIIIIIIIZIIIIIIIIIIIIIIIING");

    this.queue.when(showing).subscribe(value -> {
      if (value == null) {
        return;
      }
      loadingPane.setVisible(true);
      mapService.getMatchmakerBrackets(value).subscribe(this.brackets::setValue);
    });
    this.playerBracketIndex.subscribe(currentBracketIndex::set);

    this.brackets.subscribe(brackets -> {
      log.debug("BRACKETS: {}", brackets);
    });
    this.sortedMapPools.subscribe(pools -> {
      log.debug("MAP POOLS: {}", pools);
      if(pools.size() > 0) {
        this.currentBracketIndex.setValue(Optional.ofNullable(this.currentBracketIndex.getValue()).orElse(0));
      }
    });

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
    log.debug("CURRENT BRACKET v is: {}", v);
    if ((v == null) || (v > this.sortedMapPools.getValue().size() - 1) || (v < 0)) {
      return null;
    }

    log.debug("CURRENT BRACKET UPDATED: {}", this.sortedMapPools.getValue().get(this.currentBracketIndex.get()));

    return this.sortedMapPools.getValue().get(this.currentBracketIndex.get());
  }

  private String getBracketTitle(MatchmakerQueueMapPool bracket) {
    if (bracket == null) {
      return "";
    }

    Double min = bracket.minRating(), max = bracket.maxRating();
    if (min == null && max == null) {
      return "Any rating";
    }

    if (min == null) {
      return "Rating < " + Math.round(Math.ceil(max));
    }

    if (max == null) {
      return "Rating > " + Math.round(Math.floor(min));
    }
    return "Rating " + Math.round(bracket.minRating()) + " - " + Math.round(bracket.maxRating());
  }

  private List<MapPoolAssignment> getCurrentBracketMaps() {
    if (currentBracket.getValue() == null) {
      return Collections.emptyList();
    }

    log.debug("getCURRENTBRACKETMAPS TRIGGERED WITH {}", this.brackets.getValue().get(currentBracket.getValue()));

    return this.brackets.getValue()
                        .get(currentBracket.getValue())
                        .stream()
                        .sorted((m1, m2) -> MAP_VERSION_COMPARATOR.compare(m1.mapVersion(), m2.mapVersion()))
                        .toList();
  }


  private List<MatchmakerQueueMapPool> getSortedMapPools(
      Map<MatchmakerQueueMapPool, List<MapPoolAssignment>> brackets) {
    log.debug("SORTED MAP POOLS UPDATED WITH BRACKET SIZE {}", brackets.size());
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
    log.debug("VETOES IN CURRENT BRACKET:");
    log.debug(vetoesInCurrentBracket.toString());
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
    controller.setVetoTokensMax(currentBracket.getValue().vetoTokensPerPlayer());
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
      fxApplicationThreadExecutor.execute(() -> this.vetoTokensViewer.getChildren().clear());
      return;
    }
    int maxTokens = v.vetoTokensPerPlayer();
    int usedTokens = vetoTokensApplied.getValue();

    List<SVGPath> tokens = new ArrayList<>();
    for (int i = 0; i < maxTokens; i++) {
      SVGPath token = new SVGPath();
      token.setContent(
          "M18.1643 2.85951L18.1642 3.8033L18.1628 15.1153L20.5427 13.3444C21.6355 12.5312 23.139 12.5563 24.204 13.4056L26.1661 14.9701L26.8328 15.5017L26.4132 16.244L21.9549 24.1317L21.2295 25.5147C19.2025 29.3793 15.0673 31.664 10.7167 31.3229C4.9923 30.874 0.59593 26.0643 0.661914 20.3227L0.831081 5.60293L0.841517 4.6949L1.74634 4.61801L5.89383 4.26554L6.03253 1.45284L6.07784 0.533815L6.99746 0.502663L12.0103 0.332857L13.0572 0.297391L13.044 1.34488L13.0287 2.5616L17.2221 2.80485L18.1643 2.85951ZM13.0035 4.5635L12.874 14.8414L10.8741 14.8162L11.0168 3.49198L11.031 2.36718L7.98478 2.47037L7.8487 5.23004L7.51282 14.8291L5.51404 14.7592L5.81075 6.27981L2.82051 6.53393L2.66178 20.3457C2.6079 25.0346 6.1982 28.9624 10.873 29.329C14.426 29.6076 17.803 27.7418 19.4583 24.5857L20.191 23.1889L20.1983 23.175L20.206 23.1613L24.2525 16.0022L22.9571 14.9693C22.6021 14.6862 22.1009 14.6778 21.7367 14.9489L17.7595 17.9084L16.1626 17.106L16.1641 4.74684L13.0035 4.5635Z");
      if (i >= maxTokens - usedTokens) {token.setFill(Paint.valueOf("#000000"));} else {
        token.setFill(Paint.valueOf("#ffffff"));
      }
      tokens.add(token);
    }
    fxApplicationThreadExecutor.execute(() -> this.vetoTokensViewer.getChildren().setAll(tokens));
  }

  private void updateContent() {
    List<Pane> mapTiles = currentBracketMaps.getValue().stream().map(this::createMapTile).toList();
    log.debug("UPDATE CONTENT WITH BRACKET {} AND TILE LENGTH {}", currentBracket.getValue(), mapTiles.size());
    fxApplicationThreadExecutor.execute(() -> {
      //this.setBracketTitle();
      this.tilesContainer.getChildren().setAll(mapTiles);
      this.resizeToContent();
    });
  }

  public void goToPreviousBracket(ActionEvent actionEvent) {
    this.currentBracketIndex.set(Math.max(this.currentBracketIndex.get() - 1, 0));
  }

  public void goToNextBracket(ActionEvent actionEvent) {
    this.currentBracketIndex.set(
        Math.min(this.currentBracketIndex.get() + 1, this.sortedMapPools.getValue().size() - 1));
  }
}