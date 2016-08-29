package com.faforever.client.game;

import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.theme.ThemeService;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

public class GamesTableController {

  private final ObjectProperty<Game> selectedGame;
  @FXML
  TableView<Game> gamesTable;
  @FXML
  TableColumn<Game, Image> mapPreviewColumn;
  @FXML
  TableColumn<Game, String> gameTitleColumn;
  @FXML
  TableColumn<Game, PlayerFill> playersColumn;
  @FXML
  TableColumn<Game, RatingRange> ratingColumn;
  @FXML
  TableColumn<Game, String> hostColumn;
  @FXML
  TableColumn<Game, Boolean> passwordProtectionColumn;
  @Resource
  FxmlLoader fxmlLoader;
  @Resource
  MapService mapService;
  @Resource
  JoinGameHelper joinGameHelper;
  @Resource
  I18n i18n;

  public GamesTableController() {
    this.selectedGame = new SimpleObjectProperty<>();
  }

  public ObjectProperty<Game> selectedGameProperty() {
    return selectedGame;
  }

  @PostConstruct
  void postConstruct() {
    joinGameHelper.setParentNode(getRoot());
  }

  public Node getRoot() {
    return gamesTable;
  }

  public void initializeGameTable(ObservableList<Game> games) {
    SortedList<Game> sortedList = new SortedList<>(games);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setPlaceholder(new Label(i18n.get("games.noGamesAvailable")));
    gamesTable.setRowFactory(param1 -> gamesRowFactory());
    gamesTable.setItems(sortedList);
    sortedList.addListener((Observable observable) -> selectFirstGame());
    selectFirstGame();

    passwordProtectionColumn.setCellValueFactory(param -> param.getValue().passwordProtectedProperty());
    passwordProtectionColumn.setCellFactory(param -> passwordIndicatorColumn());
    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(fxmlLoader));
    mapPreviewColumn.setCellValueFactory(param -> new ObjectBinding<Image>() {
      {
        bind(param.getValue().mapFolderNameProperty());
      }

      @Override
      protected Image computeValue() {
        return mapService.loadPreview(param.getValue().getMapFolderName(), PreviewSize.SMALL);
      }
    });

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(title -> title));
    playersColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
        () -> new PlayerFill(param.getValue().getNumPlayers(), param.getValue().getMaxPlayers()),
        param.getValue().numPlayersProperty(), param.getValue().maxPlayersProperty())
    );
    playersColumn.setCellFactory(param -> playersCell());
    ratingColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(new RatingRange(param.getValue().getMinRating(), param.getValue().getMaxRating())));
    ratingColumn.setCellFactory(param -> ratingTableCell());
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(title -> title));

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> selectedGame.set(newValue));
    });
  }

  private void selectFirstGame() {
    TableView.TableViewSelectionModel<Game> selectionModel = gamesTable.getSelectionModel();
    if (selectionModel.getSelectedItem() == null && !gamesTable.getItems().isEmpty()) {
      Platform.runLater(() -> selectionModel.select(0));
    }
  }

  @NotNull
  private TableRow<Game> gamesRowFactory() {
    TableRow<Game> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        Game game = row.getItem();
        joinGameHelper.join(game);
      }
    });
    return row;
  }

  private TableCell<Game, Boolean> passwordIndicatorColumn() {
    return new StringCell<>(
        isPasswordProtected -> isPasswordProtected ? i18n.get("game.protected.symbol") : "",
        Pos.CENTER, ThemeService.CSS_CLASS_FONTAWESOME);
  }

  private TableCell<Game, PlayerFill> playersCell() {
    return new StringCell<>(playerFill -> i18n.get("game.players.format",
        playerFill.getPlayers(), playerFill.getMaxPlayers()), Pos.CENTER);
  }

  private TableCell<Game, RatingRange> ratingTableCell() {
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
    }, Pos.CENTER);
  }
}
