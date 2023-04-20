package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
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

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GameTooltipController implements Controller<Node> {

  private final UiService uiService;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final BooleanProperty showMods = new SimpleBooleanProperty(true);
  private final ObservableValue<Map<Integer, List<Integer>>> teams = game.flatMap(GameBean::teamsProperty).orElse(Map.of());
  private final ObservableValue<List<Integer>> teamIds = teams.map(teamMap -> teamMap.keySet()
      .stream()
      .sorted()
      .toList());
  private final ObservableValue<String> leaderboard = game.flatMap(GameBean::leaderboardProperty);


  public TitledPane modsPane;
  public TilePane teamsPane;
  public Label modsLabel;
  public VBox gameTooltipRoot;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(modsPane);
    modsPane.visibleProperty().bind(modsLabel.textProperty().isNotEmpty());
    modsLabel.textProperty()
        .bind(game.flatMap(GameBean::simModsProperty)
            .map(mods -> Joiner.on(System.getProperty("line.separator")).join(mods.values()))
            .flatMap(mods -> showMods.map(show -> show ? mods : "")));

    teamsPane.prefColumnsProperty().bind(teams.map(Map::size));

    for (int i = -1; i < 8; i++) {
      TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
      teamCardController.bindPlayersToPlayerIds();
      teamCardController.setRatingPrecision(RatingPrecision.ROUNDED);
      teamCardController.ratingProviderProperty()
          .bind(leaderboard.map(name -> player -> RatingUtil.getLeaderboardRating(player, name)));
      teamCardController.playerIdsProperty()
          .bind(Bindings.createObjectBinding(() -> teams.getValue()
              .get(teamCardController.getTeamId()), teams, teamCardController.teamIdProperty()));
      int index = i + 1;
      teamCardController.teamIdProperty().bind(teamIds.map(ids -> index < ids.size() ? ids.get(index) : null));
      Node teamCardControllerRoot = teamCardController.getRoot();
      teamCardControllerRoot.visibleProperty()
          .bind(teamCardController.playerIdsProperty().map(players -> !players.isEmpty()));
      JavaFxUtil.bindManagedToVisible(teamCardControllerRoot);
      teamsPane.getChildren().add(teamCardControllerRoot);
    }
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

  public Node getRoot() {
    return gameTooltipRoot;
  }
}
