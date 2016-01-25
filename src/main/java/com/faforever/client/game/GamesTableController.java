package com.faforever.client.game;

import com.faforever.client.ThemeService;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
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

import javax.annotation.Resource;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class GamesTableController {

  @FXML
  TableView<GameInfoBean> gamesTable;
  @FXML
  TableColumn<GameInfoBean, Image> mapPreviewColumn;
  @FXML
  TableColumn<GameInfoBean, String> gameTitleColumn;
  @FXML
  TableColumn<GameInfoBean, PlayerFill> playersColumn;
  @FXML
  TableColumn<GameInfoBean, RatingRange> ratingColumn;
  @FXML
  TableColumn<GameInfoBean, String> hostColumn;
  @FXML
  TableColumn<GameInfoBean, Boolean> passwordProtectionColumn;

  @Resource
  FxmlLoader fxmlLoader;
  @Resource
  MapService mapService;
  // TODO replace with gamecontroller listener
  @Resource
  GamesController gamesController;

  @Resource
  I18n i18n;

  public void initializeGameTable(ObservableList<GameInfoBean> gameInfoBeans) {
    SortedList<GameInfoBean> sortedList = new SortedList<>(gameInfoBeans);
    sortedList.comparatorProperty().bind(gamesTable.comparatorProperty());
    gamesTable.setPlaceholder(new Label(i18n.get("games.noGamesAvailable")));
    gamesTable.setItems(sortedList);
    gamesTable.setRowFactory(param1 -> gamesRowFactory());

    gameInfoBeans.addListener((ListChangeListener<GameInfoBean>) change -> {
      while (change.next()) {
        if (change.wasAdded() && gamesTable.getSelectionModel().getSelectedItem() == null) {
          Platform.runLater(() -> gamesTable.getSelectionModel().select(0));
        }
      }
    });

    passwordProtectionColumn.setCellValueFactory(param -> param.getValue().passwordProtectedProperty());
    passwordProtectionColumn.setCellFactory(param -> passwordIndicatorColumn());
    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(fxmlLoader));
    mapPreviewColumn.setCellValueFactory(param -> new ObjectBinding<Image>() {
      @Override
      protected Image computeValue() {
        return mapService.loadSmallPreview(param.getValue().getMapTechnicalName());
      }

      {
        bind(param.getValue().mapTechnicalNameProperty());
      }
    });

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(title -> title));
    playersColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
        (Callable<PlayerFill>) () -> new PlayerFill(param.getValue().getNumPlayers(), param.getValue().getMaxPlayers()),
        param.getValue().numPlayersProperty(), param.getValue().maxPlayersProperty())
    );
    playersColumn.setCellFactory(param -> playersCell());
    ratingColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(new RatingRange(param.getValue().getMinRating(), param.getValue().getMaxRating())));
    ratingColumn.setCellFactory(param -> ratingTableCell());
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(title -> title));

    gamesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> gamesController.setSelectedGame(newValue));
    });
  }

  @NotNull
  private TableRow<GameInfoBean> gamesRowFactory() {
    TableRow<GameInfoBean> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        GameInfoBean gameInfoBean = row.getItem();
        gamesController.onJoinGame(gameInfoBean, null, event.getScreenX(), event.getScreenY());
      }
    });
    return row;
  }

  private TableCell<GameInfoBean, Boolean> passwordIndicatorColumn() {
    return new StringCell<>(
        (Function<Boolean, String>) isPasswordProtected -> isPasswordProtected ? i18n.get("game.protected.symbol") : "",
        Pos.CENTER, ThemeService.CSS_CLASS_FONTAWESOME);
  }

  private TableCell<GameInfoBean, PlayerFill> playersCell() {
    return new StringCell<>((Function<PlayerFill, String>) playerFill -> i18n.get("game.players.format",
        playerFill.getPlayers(), playerFill.getMaxPlayers()), Pos.CENTER);
  }

  private TableCell<GameInfoBean, RatingRange> ratingTableCell() {
    return new StringCell<GameInfoBean, RatingRange>(ratingRange -> {
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

  public Node getRoot() {
    return gamesTable;
  }
}
