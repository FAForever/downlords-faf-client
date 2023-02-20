package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.DecimalCell;
import com.faforever.client.fx.IconCell;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import com.google.common.base.Joiner;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class GamesTableController implements Controller<Node> {

  private static final PseudoClass FRIEND_IN_GAME_PSEUDO_CLASS = PseudoClass.getPseudoClass("friendInGame");

  private final ObjectProperty<GameBean> selectedGame = new SimpleObjectProperty<>();
  private final MapService mapService;
  private final JoinGameHelper joinGameHelper;
  private final I18n i18n;
  private final UiService uiService;
  private final ImageViewHelper imageViewHelper;
  private final PlayerService playerService;
  private final Preferences preferences;

  public TableView<GameBean> gamesTable;
  public TableColumn<GameBean, Image> mapPreviewColumn;
  public TableColumn<GameBean, String> gameTitleColumn;
  public TableColumn<GameBean, PlayerFill> playersColumn;
  public TableColumn<GameBean, Double> averageRatingColumn;
  public TableColumn<GameBean, RatingRange> ratingRangeColumn;
  public TableColumn<GameBean, ObservableMap<String, String>> modsColumn;
  public TableColumn<GameBean, String> hostColumn;
  public TableColumn<GameBean, Boolean> passwordProtectionColumn;
  public TableColumn<GameBean, String> coopMissionName;
  private GameTooltipController gameTooltipController;
  private Tooltip tooltip;

  private final SimpleInvalidationListener tooltipShowingListener = () -> {
    if (tooltip.isShowing()) {
      gameTooltipController.displayGame();
    } else {
      gameTooltipController.setGame(null);
    }
  };
  private final SimpleInvalidationListener selectedItemListener = () -> JavaFxUtil.runLater(() -> selectedGame.setValue(gamesTable.getSelectionModel()
      .getSelectedItem()));

  public ObjectProperty<GameBean> selectedGameProperty() {
    return selectedGame;
  }

  public Node getRoot() {
    return gamesTable;
  }

  public void initializeGameTable(ObservableList<GameBean> games) {
    initializeGameTable(games, null, true);
  }

  public void initializeGameTable(ObservableList<GameBean> games, Function<String, String> coopMissionNameProvider, boolean listenToFilterPreferences) {
    gameTooltipController = uiService.loadFxml("theme/play/game_tooltip.fxml");
    tooltip = JavaFxUtil.createCustomTooltip(gameTooltipController.getRoot());
    JavaFxUtil.addListener(tooltip.showingProperty(), tooltipShowingListener);

    SortedList<GameBean> sortedList = new SortedList<>(games);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setPlaceholder(new Label(i18n.get("games.noGamesAvailable")));
    gamesTable.setRowFactory(param1 -> gamesRowFactory());
    gamesTable.setItems(sortedList);

    applyLastSorting(gamesTable);
    gamesTable.setOnSort(this::onColumnSorted);

    passwordProtectionColumn.setCellValueFactory(param -> param.getValue().passwordProtectedProperty());
    passwordProtectionColumn.setCellFactory(param -> passwordIndicatorColumn());

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(imageViewHelper));
    mapPreviewColumn.setCellValueFactory(param -> param.getValue()
        .mapFolderNameProperty()
        .map(mapFolderName -> mapService.loadPreview(mapFolderName, PreviewSize.SMALL)));

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(StringUtils::normalizeSpace));
    playersColumn.setCellValueFactory(param -> param.getValue().maxPlayersProperty().flatMap(max -> param.getValue().numActivePlayersProperty().map(active -> new PlayerFill(active, max.intValue()))));
    playersColumn.setCellFactory(param -> playersCell());
    ratingRangeColumn.setCellValueFactory(param -> param.getValue().ratingMaxProperty().flatMap(max -> param.getValue().ratingMinProperty().map(min -> new RatingRange(min, max))));
    ratingRangeColumn.setCellFactory(param -> ratingTableCell());
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new HostTableCell(playerService));
    modsColumn.setCellValueFactory(param -> param.getValue().simModsProperty());
    modsColumn.setCellFactory(param -> new StringCell<>(this::convertSimModsToContent));
    coopMissionName.setVisible(coopMissionNameProvider != null);

    if (averageRatingColumn != null) {
      averageRatingColumn.setCellValueFactory(param -> playerService.getAverageRatingPropertyForGame(param.getValue()));
      averageRatingColumn.setCellFactory(param -> new DecimalCell<>(new DecimalFormat("0"), number -> Math.round(number / 100.0) * 100.0));
    }

    if (coopMissionNameProvider != null) {
      coopMissionName.setCellFactory(param -> new StringCell<>(name -> name));
      coopMissionName.setCellValueFactory(param -> new SimpleObjectProperty<>(coopMissionNameProvider.apply(param.getValue()
          .getMapFolderName())));
    }

    JavaFxUtil.addListener(gamesTable.getSelectionModel().selectedItemProperty(), selectedItemListener);

    //bindings do not work as that interferes with some bidirectional bindings in the TableView itself
    if (listenToFilterPreferences && coopMissionNameProvider == null) {
      JavaFxUtil.addAndTriggerListener(preferences.hideModdedGamesProperty(), observable -> modsColumn.setVisible(!preferences.isHideModdedGames()));
      JavaFxUtil.addAndTriggerListener(preferences.hidePrivateGamesProperty(), observable -> passwordProtectionColumn.setVisible(!preferences.isHidePrivateGames()));
    }

    selectFirstGame();
  }

  private void selectFirstGame() {
    gamesTable.getSelectionModel().selectFirst();
  }

  private void applyLastSorting(TableView<GameBean> gamesTable) {
    final Map<String, SortType> lookup = new HashMap<>(preferences.getGameTableSorting());
    final ObservableList<TableColumn<GameBean, ?>> sortOrder = gamesTable.getSortOrder();
    sortOrder.clear();
    gamesTable.getColumns().forEach(gameTableColumn -> {
      if (lookup.containsKey(gameTableColumn.getId())) {
        gameTableColumn.setSortType(lookup.get(gameTableColumn.getId()));
        sortOrder.add(gameTableColumn);
      }
    });
  }

  private void onColumnSorted(@NotNull SortEvent<TableView<GameBean>> event) {
    ObservableMap<String, SortType> gameListSorting = preferences.getGameTableSorting();

    gameListSorting.clear();
    event.getSource().getSortOrder().forEach(column -> gameListSorting.put(column.getId(), column.getSortType()));
  }

  @NotNull
  private String convertSimModsToContent(Map<String, String> simMods) {
    List<String> modNames = simMods.values().stream().limit(2).collect(Collectors.toList());

    if (simMods.size() > 2) {
      return i18n.get("game.mods.twoAndMore", modNames.get(0), simMods.size() - 1);
    }
    return Joiner.on(i18n.get("textSeparator")).join(modNames);
  }

  @NotNull
  private TableRow<GameBean> gamesRowFactory() {
    TableRow<GameBean> row = new TableRow<>() {
      @Override
      protected void updateItem(GameBean game, boolean empty) {
        super.updateItem(game, empty);
        if (empty || game == null) {
          setTooltip(null);
          pseudoClassStateChanged(FRIEND_IN_GAME_PSEUDO_CLASS, false);
        } else {
          setTooltip(tooltip);
          pseudoClassStateChanged(FRIEND_IN_GAME_PSEUDO_CLASS, playerService.areFriendsInGame(game) && game.getGameType() != GameType.COOP); // do not highlight coop games
        }
      }
    };
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        GameBean game = row.getItem();
        joinGameHelper.join(game);
      }
    });
    row.setOnMouseEntered(event -> {
      if (row.getItem() == null) {
        return;
      }
      GameBean game = row.getItem();
      gameTooltipController.setGame(game);
      if (tooltip.isShowing()) {
        gameTooltipController.displayGame();
      }
    });
    return row;
  }

  private TableCell<GameBean, Boolean> passwordIndicatorColumn() {
    return new IconCell<>(isPasswordProtected -> isPasswordProtected ? "lock-icon" : "");
  }

  private TableCell<GameBean, PlayerFill> playersCell() {
    return new StringCell<>(playerFill -> i18n.get("game.players.format", playerFill.getPlayers(), playerFill.getMaxPlayers()));
  }

  private TableCell<GameBean, RatingRange> ratingTableCell() {
    return new StringCell<>(ratingRange -> {
      if (ratingRange.getMin() == null && ratingRange.getMax() == null) {
        return "";
      }

      if (ratingRange.getMin() != null && ratingRange.getMax() != null) {
        return i18n.get("game.ratingFormat.minMax", ratingRange.getMin(), ratingRange.getMax());
      }

      if (ratingRange.getMin() != null) {
        return i18n.get("game.ratingFormat.minOnly", ratingRange.getMin());
      }

      return i18n.get("game.ratingFormat.maxOnly", ratingRange.getMax());
    });
  }

  public TableColumn<GameBean, Image> getMapPreviewColumn() {
    return mapPreviewColumn;
  }

  public TableColumn<GameBean, RatingRange> getRatingRangeColumn() {
    return ratingRangeColumn;
  }

  public void removeListeners() {
    JavaFxUtil.removeListener(tooltip.showingProperty(), tooltipShowingListener);
    JavaFxUtil.removeListener(gamesTable.getSelectionModel().selectedItemProperty(), selectedItemListener);
  }

  public void refreshTable() {
    JavaFxUtil.runLater(() -> gamesTable.refresh());
  }
}
