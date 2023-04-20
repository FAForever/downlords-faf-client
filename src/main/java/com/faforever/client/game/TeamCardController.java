package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Faction;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class TeamCardController implements Controller<Node> {
  private final UiService uiService;
  private final I18n i18n;
  private final PlayerService playerService;

  public Pane teamPaneRoot;
  public VBox teamPane;
  public Label teamNameLabel;

  private final ObjectProperty<List<Integer>> playerIds = new SimpleObjectProperty<>(List.of());
  private final ObjectProperty<List<PlayerBean>> players = new SimpleObjectProperty<>(List.of());
  private final ObjectProperty<Function<PlayerBean, Integer>> ratingProvider = new SimpleObjectProperty<>();
  private final ObjectProperty<Function<PlayerBean, Faction>> factionProvider = new SimpleObjectProperty<>();
  private final ObjectProperty<RatingPrecision> ratingPrecision = new SimpleObjectProperty<>();
  private final IntegerProperty teamId = new SimpleIntegerProperty();
  private final ObservableValue<Integer> teamRating = ratingProvider.flatMap(provider -> ratingPrecision.flatMap(precision -> players.map(playerBeans -> playerBeans.stream()
      .map(provider)
      .filter(Objects::nonNull)
      .map(rating -> precision == RatingPrecision.ROUNDED ? RatingUtil.getRoundedRating(rating) : rating)
      .reduce(0, Integer::sum))));

  private final List<Pair<PlayerCardController, RatingChangeLabelController>> playerInfoControllers = new ArrayList<>();

  public void initialize() {
    teamNameLabel.textProperty()
        .bind(teamRating.flatMap(teamRating -> teamId.map(id -> switch (id.intValue()) {
          case 0, GameBean.NO_TEAM -> i18n.get("game.tooltip.teamTitleNoTeam");
          case GameBean.OBSERVERS_TEAM -> i18n.get("game.tooltip.observers");
          default -> {
            try {
              yield i18n.get("game.tooltip.teamTitle", id.intValue() - 1, teamRating);
            } catch (NumberFormatException e) {
              yield "";
            }
          }
        })));

    for (int i = 0; i < 16; i++) {
      PlayerCardController playerCardController = uiService.loadFxml("theme/player_card.fxml");
      RatingChangeLabelController ratingChangeLabelController = uiService.loadFxml("theme/rating_change_label.fxml");
      Pair<PlayerCardController, RatingChangeLabelController> pair = new Pair<>(playerCardController, ratingChangeLabelController);

      int index = i;
      ObservableValue<PlayerBean> playerBinding = players.map(playerBeans -> index < playerBeans.size() ? playerBeans.get(index) : null);
      playerCardController.ratingProperty()
          .bind(playerBinding.flatMap(player -> ratingProvider.map(ratingFunction -> ratingFunction.apply(player))
              .flatMap(rating -> ratingPrecision.map(precision -> precision == RatingPrecision.ROUNDED ? RatingUtil.getRoundedRating(rating) : rating))));
      playerCardController.factionProperty()
          .bind(playerBinding.flatMap(player -> factionProvider.map(factionFunction -> factionFunction.apply(player))));
      playerCardController.playerProperty().bind(playerBinding);
      HBox playerRoot = new HBox(pair.getKey().getRoot(), pair.getValue().getRoot());
      playerRoot.visibleProperty().bind(playerCardController.playerProperty().isNotNull());
      JavaFxUtil.bindManagedToVisible(playerRoot);

      playerInfoControllers.add(pair);
      teamPane.getChildren().add(playerRoot);
    }
  }

  public void bindPlayersToPlayerIds() {
    players.bind(playerIds.map(ids -> ids.stream()
        .map(playerService::getPlayerByIdIfOnline)
        .flatMap(Optional::stream)
        .collect(Collectors.toCollection(FXCollections::observableArrayList))));
  }

  public void setRatingProvider(Function<PlayerBean, Integer> ratingProvider) {
    this.ratingProvider.set(ratingProvider);
  }

  public void setFactionProvider(Function<PlayerBean, Faction> factionProvider) {
    this.factionProvider.set(factionProvider);
  }

  public void setRatingPrecision(RatingPrecision ratingPrecision) {
    this.ratingPrecision.set(ratingPrecision);
  }

  public void setTeamId(int teamId) {
    this.teamId.set(teamId);
  }

  public int getTeamId() {
    return teamId.get();
  }

  public void setPlayerIds(Collection<Integer> playerIds) {
    this.playerIds.set(List.copyOf(playerIds));
  }

  public void setPlayers(Collection<PlayerBean> players) {
    this.players.set(List.copyOf(players));
  }

  public ObjectProperty<List<Integer>> playerIdsProperty() {
    return playerIds;
  }

  public IntegerProperty teamIdProperty() {
    return teamId;
  }

  public ObjectProperty<Function<PlayerBean, Integer>> ratingProviderProperty() {
    return ratingProvider;
  }

  public void setStats(List<GamePlayerStatsBean> teamPlayerStats) {
    List<PlayerBean> playerBeans = players.get();
    if (playerBeans == null) {
      return;
    }

    for (GamePlayerStatsBean playerStats : teamPlayerStats) {
      int index = playerBeans.indexOf(playerStats.getPlayer());

      if (index < 0 || index >= playerInfoControllers.size()) {
        continue;
      }

      playerInfoControllers.get(index)
          .getValue()
          .setRatingChange(playerStats);
    }
  }

  public Node getRoot() {
    return teamPaneRoot;
  }
}
