package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Joiner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GameTooltipController extends NodeController<Node> {

  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final BooleanProperty showMods = new SimpleBooleanProperty(true);
  private final ObservableValue<Map<Integer, List<Integer>>> teams = game.flatMap(GameBean::teamsProperty)
                                                                         .orElse(Map.of());
  private final ObservableValue<String> leaderboard = game.flatMap(GameBean::leaderboardProperty);
  private final SimpleChangeListener<Map<Integer, List<Integer>>> teamsListener = this::populateTeamsContainer;

  public TitledPane modsPane;
  public TilePane teamsPane;
  public Label modsLabel;
  public VBox gameTooltipRoot;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(modsPane);
    modsPane.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
    modsLabel.textProperty()
             .bind(game.flatMap(GameBean::simModsProperty)
                       .map(mods -> Joiner.on(System.getProperty("line.separator")).join(mods.values()))
                       .flatMap(mods -> showMods.map(show -> show ? mods : ""))
                       .when(showing));

    teamsPane.prefColumnsProperty().bind(teams.map(Map::size));
    teams.addListener(teamsListener);
  }

  private void populateTeamsContainer(Map<Integer, List<Integer>> newValue) {
    CompletableFuture.supplyAsync(() -> createTeamCardControllers(newValue))
                     .thenAcceptAsync(controllers -> teamsPane.getChildren()
                                                              .setAll(controllers.stream()
                                                                                 .map(TeamCardController::getRoot)
                                                                                 .toList()),
                                      fxApplicationThreadExecutor);
  }

  private List<TeamCardController> createTeamCardControllers(Map<Integer, List<Integer>> teamsValue) {
    return teamsValue.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
      Integer team = entry.getKey();
      List<Integer> playerIds = entry.getValue();

      TeamCardController controller = uiService.loadFxml("theme/team_card.fxml");
      controller.setRatingPrecision(RatingPrecision.ROUNDED);
      controller.ratingProviderProperty()
                .bind(leaderboard.map(
                                     name -> (Function<PlayerBean, Integer>) player -> RatingUtil.getLeaderboardRating(player, name))
                                 .when(showing));
      controller.setTeamId(team);
      controller.setPlayerIds(playerIds);
      controller.bindPlayersToPlayerIds();

      return controller;
    }).toList();
  }

  public void setGame(GameBean game) {
    this.game.set(game);
  }

  public GameBean getGame() {
    return game.get();
  }

  public ObjectProperty<GameBean> gameProperty() {
    return game;
  }

  public void setShowMods(boolean showMods) {
    this.showMods.set(showMods);
  }

  @Override
  public Node getRoot() {
    return gameTooltipRoot;
  }
}
