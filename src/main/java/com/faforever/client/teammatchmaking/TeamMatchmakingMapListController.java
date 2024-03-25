package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.LeaderboardRating;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;

import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.fx.NodeController;
import com.faforever.client.map.MapService;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor

public class TeamMatchmakingMapListController extends NodeController<Pane> {

  public Pane root;
  public FlowPane tilesContainer;
  private final MapService mapService;
  private final UiService uiService;
  private final I18n i18n;
  private final PlayerService playerService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final int TILE_SIZE = 125;
  public ScrollPane scrollContainer;
  public VBox loadingPane;
  private Map<MatchmakerQueueMapPool, List<MapVersion>> rawBrackets;
  private SortedMap<MatchmakerQueueMapPool, List<MapVersion>> sortedBrackets;
  private SortedMap<MatchmakerQueueMapPool, List<MapVersion>> sortedBracketsWithDuplicates;
  private Integer playerBracketIndex = null;
  private Pane dialogRoot;


  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(loadingPane);
  }

  @Override
  public Pane getRoot() {
    return root;
  }


  public void init(MatchmakerQueueInfo queue, Pane dialogRoot) {
    this.dialogRoot = dialogRoot;
    mapService.getMatchmakerBrackets(queue).subscribe(rawBrackets -> {
      loadingPane.setVisible(false);
      this.rawBrackets = rawBrackets;
      this.sortedBracketsWithDuplicates = this.getSortedBrackets(rawBrackets);
      this.sortedBrackets = this.removeDuplicates(this.getSortedBrackets(rawBrackets));

      PlayerInfo player = playerService.getCurrentPlayer();
      LeaderboardRating ratingBean = player.getLeaderboardRatings().get(queue.getLeaderboard().technicalName());
      double rating = ratingBean.mean() - 3 * ratingBean.deviation();
      this.playerBracketIndex = this.getPlayerBracketIndex(this.sortedBrackets, rating);

      List<MapVersion> values = this.sortedBrackets.values().stream().flatMap(List::stream).toList();
      this.resizeToContent(values.size(), this.TILE_SIZE);

      Flux.fromIterable(values)
          .publishOn(fxApplicationThreadExecutor.asScheduler())
          .doOnNext(this::addMapTile)
          .subscribe();
    });

  }


  private SortedMap<MatchmakerQueueMapPool, List<MapVersion>> getSortedBrackets(Map<MatchmakerQueueMapPool, List<MapVersion>> brackets) {

    Comparator<MapVersion> mapVersionComparator = Comparator.nullsFirst(Comparator.comparing(MapVersion::size))
                                                            .thenComparing(mapVersion -> mapVersion.map().displayName(),
                                                                           String.CASE_INSENSITIVE_ORDER);

    Comparator<MatchmakerQueueMapPool> mapPoolComparator = Comparator.comparingDouble(MatchmakerQueueMapPool::minRating)
                                                                     .thenComparingDouble(
                                                                         MatchmakerQueueMapPool::maxRating);

    SortedMap<MatchmakerQueueMapPool, List<MapVersion>> sortedMap = new TreeMap<>(mapPoolComparator);

    brackets.forEach((mapPool, mapVersions) -> {
      List<MapVersion> sortedList = mapVersions.stream().sorted(mapVersionComparator).collect(Collectors.toList());
      sortedMap.put(mapPool, sortedList);
    });

    return sortedMap;
  }

  private SortedMap<MatchmakerQueueMapPool, List<MapVersion>> removeDuplicates(SortedMap<MatchmakerQueueMapPool, List<MapVersion>> sortedBrackets) {
    HashSet<String> usedMaps = new HashSet<>();
    sortedBrackets.replaceAll((k, v) -> sortedBrackets.get(k).stream().filter(mapVersion -> {
      String name = mapVersion.map().displayName();
      if (usedMaps.contains(name))
        return false;
      usedMaps.add(name);
      return true;
    }).collect(Collectors.toList()));
    return sortedBrackets;
  }

  private Integer getPlayerBracketIndex(SortedMap <MatchmakerQueueMapPool, List<MapVersion>> sortedBrackets, double rating) {
    int i = 0;
    for (var entry : sortedBrackets.entrySet()) {
      MatchmakerQueueMapPool pool = entry.getKey();
      double min = pool.minRating();
      double max = pool.maxRating();
      if (min == 0.0) {min = Double.NEGATIVE_INFINITY;}
      if (max == 0.0) {max = Double.POSITIVE_INFINITY;}

      if (rating < max && rating > min)
        return i;
      i++;
    }
    return null;
  }

  private void addMapTile(MapVersion mapVersion) {
    int i = 0;
    List<Integer> indexes = new ArrayList<>();
    for (var maps : this.sortedBracketsWithDuplicates.values()) {
      if (maps.contains(mapVersion)) indexes.add(i);
      i++;
    }
    int diff = Collections.min(indexes.stream().map(idx -> Math.abs(idx - this.playerBracketIndex)).toList());
    double relevanceLevel = switch (diff) {
      case 0 -> 1;
      case 1 -> 0.2;
      default -> 0;
    };

    TeamMatchmakingMapTileController controller = uiService.loadFxml(
        "theme/play/teammatchmaking/matchmaking_map_tile.fxml");
    controller.init(mapVersion, relevanceLevel);
    this.tilesContainer.getChildren().add(controller.getRoot());
  }

  private void resizeToContent(int tilecount, int tileSize) {
    double viewportWidth = Screen.getPrimary().getVisualBounds().getWidth();
    double viewportHeight = Screen.getPrimary().getVisualBounds().getHeight();
    double hgap = tilesContainer.getHgap();
    double vgap = tilesContainer.getVgap();

    int maxTilesInLine = (int) Math.min(10, Math.floor((viewportWidth * 0.9 + hgap) / (tileSize + hgap)));
    double preservedSpace = this.dialogRoot.getBoundsInLocal().getHeight() - scrollContainer.getBoundsInLocal().getHeight();

    int maxLinesWithoutScroll = (int) Math.floor((viewportHeight * 0.9 - preservedSpace + vgap) / (tileSize + vgap));
    int scrollWidth = 18;
    double maxScrollPaneHeight = maxLinesWithoutScroll * (tileSize + vgap) - vgap;
    this.scrollContainer.setMaxHeight(maxScrollPaneHeight);

    int tilesInOneLine = Math.min(maxTilesInLine, Math.max(Math.max(4, Math.ceilDiv(tilecount, maxLinesWithoutScroll)), (int) Math.ceil(Math.sqrt(tilecount))));
    int numberOfLines = Math.ceilDiv(tilecount, tilesInOneLine);

    double preferredWidth = (tileSize + hgap) * tilesInOneLine - hgap;
    double gridHeight = (tileSize + vgap) * numberOfLines - vgap;

    if (gridHeight > maxScrollPaneHeight) {
      scrollContainer.setPrefWidth(preferredWidth + scrollWidth);
      scrollContainer.setPrefHeight(maxScrollPaneHeight);
    }
    else {
      scrollContainer.setPrefWidth(preferredWidth);
      scrollContainer.setPrefHeight(gridHeight);
    }

    tilesContainer.setPrefWidth(preferredWidth);
  }

}