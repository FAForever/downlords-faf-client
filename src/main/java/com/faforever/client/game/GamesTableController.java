package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.DecimalCell;
import com.faforever.client.fx.IconCell;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
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
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  public TableView<GameBean> gamesTable;
  public TableColumn<GameBean, Image> mapPreviewColumn;
  public TableColumn<GameBean, String> gameTitleColumn;
  public TableColumn<GameBean, PlayerFill> playersColumn;
  public TableColumn<GameBean, Number> averageRatingColumn;
  public TableColumn<GameBean, RatingRange> ratingRangeColumn;
  public TableColumn<GameBean, String> modsColumn;
  public TableColumn<GameBean, String> hostColumn;
  public TableColumn<GameBean, Boolean> passwordProtectionColumn;
  public TableColumn<GameBean, String> coopMissionName;
  private final ChangeListener<Boolean> showModdedGamesChangedListener = (observable, oldValue, newValue) -> modsColumn.setVisible(newValue);
  private final ChangeListener<Boolean> showPasswordProtectedGamesChangedListener = (observable, oldValue, newValue) -> passwordProtectionColumn.setVisible(newValue);
  private GameTooltipController gameTooltipController;
  private Tooltip tooltip;

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
    tooltip.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        gameTooltipController.displayGame();
      } else {
        gameTooltipController.setGame(null);
      }
    });

    SortedList<GameBean> sortedList = new SortedList<>(games);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setPlaceholder(new Label(i18n.get("games.noGamesAvailable")));
    gamesTable.setRowFactory(param1 -> gamesRowFactory());
    gamesTable.setItems(sortedList);

    applyLastSorting(gamesTable);
    gamesTable.setOnSort(this::onColumnSorted);

    passwordProtectionColumn.setCellValueFactory(param -> param.getValue().passwordProtectedProperty());
    passwordProtectionColumn.setCellFactory(param -> passwordIndicatorColumn());

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(uiService));
    mapPreviewColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
        () -> mapService.loadPreview(param.getValue().getMapFolderName(), PreviewSize.SMALL),
        param.getValue().mapFolderNameProperty()
    ));

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(title -> title));
    playersColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
        () -> new PlayerFill(param.getValue().getNumPlayers(), param.getValue().getMaxPlayers()),
        param.getValue().numPlayersProperty(), param.getValue().maxPlayersProperty())
    );
    playersColumn.setCellFactory(param -> playersCell());
    ratingRangeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(new RatingRange(param.getValue().getRatingMin(), param.getValue().getRatingMax())));
    ratingRangeColumn.setCellFactory(param -> ratingTableCell());
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(String::toString));
    modsColumn.setCellValueFactory(this::modCell);
    modsColumn.setCellFactory(param -> new StringCell<>(String::toString));
    coopMissionName.setVisible(coopMissionNameProvider != null);

    if (averageRatingColumn != null) {
      averageRatingColumn.setCellValueFactory(param -> param.getValue().averageRatingProperty());
      averageRatingColumn.setCellFactory(param -> new DecimalCell<>(
          new DecimalFormat("0"),
          number -> Math.round(number.doubleValue() / 100.0) * 100.0)
      );
    }

    if (coopMissionNameProvider != null) {
      coopMissionName.setCellFactory(param -> new StringCell<>(name -> name));
      coopMissionName.setCellValueFactory(param -> new SimpleObjectProperty<>(coopMissionNameProvider.apply(param.getValue().getMapFolderName())));
    }

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
        -> JavaFxUtil.runLater(() -> selectedGame.set(newValue)));

    //bindings do not work as that interferes with some bidirectional bindings in the TableView itself
    if (listenToFilterPreferences && coopMissionNameProvider == null) {
      modsColumn.setVisible(preferencesService.getPreferences().isShowModdedGames());
      passwordProtectionColumn.setVisible(preferencesService.getPreferences().isShowPasswordProtectedGames());
      JavaFxUtil.addListener(preferencesService.getPreferences().showModdedGamesProperty(), new WeakChangeListener<>(showModdedGamesChangedListener));
      JavaFxUtil.addListener(preferencesService.getPreferences().showPasswordProtectedGamesProperty(), new WeakChangeListener<>(showPasswordProtectedGamesChangedListener));
    }
  }

  private void applyLastSorting(TableView<GameBean> gamesTable) {
    final Map<String, SortType> lookup = new HashMap<>();
    final ObservableList<TableColumn<GameBean, ?>> sortOrder = gamesTable.getSortOrder();
    preferencesService.getPreferences().getGameTableSorting().forEach(lookup::put);
    sortOrder.clear();
    gamesTable.getColumns().forEach(gameTableColumn -> {
      if (lookup.containsKey(gameTableColumn.getId())) {
        gameTableColumn.setSortType(lookup.get(gameTableColumn.getId()));
        sortOrder.add(gameTableColumn);
      }
    });
  }

  private void onColumnSorted(@NotNull SortEvent<TableView<GameBean>> event) {
    ObservableMap<String, SortType> gameListSorting = preferencesService.getPreferences().getGameTableSorting();

    gameListSorting.clear();
    event.getSource().getSortOrder()
        .forEach(column -> gameListSorting.put(column.getId(), column.getSortType()));

    preferencesService.storeInBackground();
  }

  @NotNull
  private ObservableValue<String> modCell(CellDataFeatures<GameBean, String> param) {
    Map<String, String> simMods = param.getValue().getSimMods();
    int simModCount = simMods.size();
    List<String> modNames;
      modNames = simMods.values().stream()
          .limit(2)
          .collect(Collectors.toList());
    if (simModCount > 2) {
      return new SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames.get(0), modNames.size() - 1));
    }
    return new SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames));
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
          pseudoClassStateChanged(FRIEND_IN_GAME_PSEUDO_CLASS, playerService.areFriendsInGame(game)
              && game.getGameType() != GameType.COOP); // do not highlight coop games
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
    return new IconCell<>(
        isPasswordProtected -> isPasswordProtected ? "lock-icon" : "");
  }

  private TableCell<GameBean, PlayerFill> playersCell() {
    return new StringCell<>(playerFill -> i18n.get("game.players.format",
        playerFill.getPlayers(), playerFill.getMaxPlayers()));
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
}
