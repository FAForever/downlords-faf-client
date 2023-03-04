package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GameTooltipController implements Controller<Node> {

  private final UiService uiService;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final BooleanProperty showMods = new SimpleBooleanProperty(true);
  private final MapProperty<Integer, List<Integer>> teams = new SimpleMapProperty<>(FXCollections.emptyObservableMap());
  private final ObservableList<Integer> teamIds = new SortedList<>(JavaFxUtil.attachListToMapKeys(FXCollections.observableArrayList(), teams), Comparator.naturalOrder());
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

    teams.bind(game.flatMap(GameBean::teamsProperty));

    teamsPane.prefColumnsProperty().bind(Bindings.min(2, Bindings.size(teams)));

    for (int i = -1; i < 8; i++) {
      TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
      teamCardController.bindPlayersToPlayerIds();
      teamCardController.setRatingPrecision(RatingPrecision.ROUNDED);
      teamCardController.ratingProviderProperty()
          .bind(leaderboard.map(name -> player -> RatingUtil.getLeaderboardRating(player, name)));
      teamCardController.playerIdsProperty()
          .bind(Bindings.valueAt(teams, teamCardController.teamIdProperty().asObject())
              .map(FXCollections::observableList));
      teamCardController.teamIdProperty().bind(Bindings.valueAt(teamIds, i + 1));
      Node teamCardControllerRoot = teamCardController.getRoot();
      teamCardControllerRoot.visibleProperty().bind(teamCardController.playerIdsProperty().emptyProperty().not());
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
