package com.faforever.client.vault.replay;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.MapPreviewTableCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LiveReplayController extends AbstractViewController<Node> {
  private final ObjectProperty<Game> selectedGame;
  private final GameService gameService;
  private final UiService uiService;
  private final I18n i18n;
  private final MapService mapService;
  private final TimeService timeService;
  public TableView<Game> liveReplayControllerRoot;
  public TableColumn<Game, Image> mapPreviewColumn;
  public TableColumn<Game, Instant> startTimeColumn;
  public TableColumn<Game, String> gameTitleColumn;
  public TableColumn<Game, Number> playersColumn;
  public TableColumn<Game, String> modsColumn;
  public TableColumn<Game, String> hostColumn;
  public TableColumn<Game, Game> watchColumn;

  public LiveReplayController(GameService gameService, UiService uiService, I18n i18n,
                              MapService mapService, TimeService timeService) {
    this.gameService = gameService;
    this.uiService = uiService;
    this.i18n = i18n;
    this.mapService = mapService;
    this.timeService = timeService;

    selectedGame = new SimpleObjectProperty<>();
  }

  public void initialize() {
    initializeGameTable(gameService.getGames());
  }

  private void initializeGameTable(ObservableList<Game> games) {
    FilteredList<Game> filteredGameList = new FilteredList<>(games);
    filteredGameList.setPredicate(game -> game.getStatus() == GameStatus.PLAYING);
    SortedList<Game> sortedList = new SortedList<>(filteredGameList);
    sortedList.comparatorProperty().bind(liveReplayControllerRoot.comparatorProperty());

    startTimeColumn.setSortType(SortType.DESCENDING);
    liveReplayControllerRoot.getSortOrder().add(startTimeColumn);
    liveReplayControllerRoot.sort();

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(uiService));
    mapPreviewColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
        () -> mapService.loadPreview(param.getValue().getMapFolderName(), PreviewSize.SMALL),
        param.getValue().mapFolderNameProperty()
    ));

    startTimeColumn.setCellValueFactory(param -> param.getValue().startTimeProperty());
    startTimeColumn.setCellFactory(param -> new StringCell<>(this.timeService::asShortTime));
    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(title -> title));
    playersColumn.setCellValueFactory(param -> param.getValue().numPlayersProperty());
    playersColumn.setCellFactory(param -> new StringCell<>(number -> i18n.number(number.intValue())));
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(String::toString));
    modsColumn.setCellValueFactory(this::modCell);
    modsColumn.setCellFactory(param -> new StringCell<>(String::toString));
    watchColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
    watchColumn.setCellFactory(param -> new NodeTableCell<>(this::watchReplayButton));

    liveReplayControllerRoot.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
        -> Platform.runLater(() -> selectedGame.set(newValue)));

    liveReplayControllerRoot.setItems(sortedList);
  }

  private Node watchReplayButton(Game game) {
    WatchButtonController controller = uiService.loadFxml("theme/vault/replay/watch_button.fxml");
    controller.setGame(game);
    return controller.getRoot();
  }

  @Override
  public Node getRoot() {
    return liveReplayControllerRoot;
  }

  @NotNull
  private ObservableValue<String> modCell(CellDataFeatures<Game, String> param) {
    ObservableMap<String, String> simMods = param.getValue().getSimMods();
    int simModCount = simMods.size();
    List<String> modNames = simMods.entrySet().stream()
        .limit(2)
        .map(Entry::getValue)
        .collect(Collectors.toList());
    if (simModCount > 2) {
      return new SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames.get(0), modNames.size()));
    }
    return new SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames));
  }
}
