package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
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
  private final ListProperty<TeamCardController> teamCardControllers = new SimpleListProperty<>(FXCollections.observableArrayList());
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
    teams.addListener((SimpleInvalidationListener) this::onTeamsInvalidated);

    teamsPane.prefColumnsProperty().bind(Bindings.min(2, Bindings.size(teams)));
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

  private void onTeamsInvalidated() {
    int numTeams = teams.size();
    int numControllers = teamCardControllers.size();
    int difference = numTeams - numControllers;
    if (difference > 0) {
      TeamCardController teamCardController = uiService.loadFxml("theme/team_card.fxml");
      teamCardController.bindPlayersToPlayerIds();
      teamCardController.setRatingPrecision(RatingPrecision.ROUNDED);
      teamCardController.ratingProviderProperty().bind(leaderboard.map(name -> player -> RatingUtil.getLeaderboardRating(player, name)));
      teamCardController.playerIdsProperty().bind(Bindings.valueAt(teams, teamCardController.teamIdProperty().asObject()).map(FXCollections::observableList));
      IntegerBinding indexBinding = Bindings.createIntegerBinding(() -> teamCardControllers.indexOf(teamCardController), teamCardControllers);
      teamCardController.teamIdProperty().bind(Bindings.valueAt(teamIds, indexBinding));
      teamCardControllers.add(teamCardController);
      teamsPane.getChildren().add(teamCardController.getRoot());
    } else if (difference < 0) {
      int from = numControllers + difference;
      teamCardControllers.remove(from, numControllers);
      teamsPane.getChildren().remove(from, numControllers);
    }
  }

  public void setShowMods(boolean showMods) {
    this.showMods.set(showMods);
  }

  public Node getRoot() {
    return gameTooltipRoot;
  }
}
