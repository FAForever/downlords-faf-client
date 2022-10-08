package com.faforever.client.vault.replay;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.LiveGamesFilterController;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.DecimalCell;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.GameService;
import com.faforever.client.game.MapPreviewTableCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLiveReplayViewEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.table.NoSelectionModelTableView;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.base.Joiner;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LiveReplayController extends AbstractViewController<Node> {

  private final GameService gameService;
  private final UiService uiService;
  private final I18n i18n;
  private final MapService mapService;
  private final TimeService timeService;

  public VBox root;
  public ToggleButton filterButton;
  public TableView<GameBean> tableView;
  public TableColumn<GameBean, Image> mapPreviewColumn;
  public TableColumn<GameBean, OffsetDateTime> startTimeColumn;
  public TableColumn<GameBean, String> gameTitleColumn;
  public TableColumn<GameBean, Number> playersColumn;
  public TableColumn<GameBean, Number> averageRatingColumn;
  public TableColumn<GameBean, String> modsColumn;
  public TableColumn<GameBean, String> hostColumn;
  public TableColumn<GameBean, GameBean> watchColumn;

  private boolean initialized = false;
  private LiveGamesFilterController liveGamesFilterController;
  private Popup gameFilterPopup;

  @Override
  public void initialize() {
    tableView.setSelectionModel(new NoSelectionModelTableView<>(tableView));
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (!initialized && navigateEvent instanceof OpenLiveReplayViewEvent) {
      initializeFilterController();
      initializeGameTable();
      initialized = true;
    }
  }

  private void initializeFilterController() {
    liveGamesFilterController = uiService.loadFxml("theme/filter/filter.fxml", LiveGamesFilterController.class);
    liveGamesFilterController.setDefaultPredicate(game -> game.getStatus() == GameStatus.PLAYING);
    liveGamesFilterController.completeSetting();

    JavaFxUtil.addAndTriggerListener(liveGamesFilterController.getFilterStateProperty(), (observable, oldValue, newValue) -> filterButton.setSelected(newValue));
    JavaFxUtil.addAndTriggerListener(filterButton.selectedProperty(), observable -> filterButton.setSelected(liveGamesFilterController.getFilterState()));
  }

  private void initializeGameTable() {
    FilteredList<GameBean> filteredGameList = new FilteredList<>(gameService.getGames());
    JavaFxUtil.addAndTriggerListener(liveGamesFilterController.getPredicateProperty(),
        (observable, oldValue, newValue) -> filteredGameList.setPredicate(newValue));

    SortedList<GameBean> sortedList = new SortedList<>(filteredGameList);
    sortedList.comparatorProperty().bind(tableView.comparatorProperty());

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(uiService));
    mapPreviewColumn.setCellValueFactory(param -> Bindings.createObjectBinding(
        () -> mapService.loadPreview(param.getValue().getMapFolderName(), PreviewSize.SMALL),
        param.getValue().mapFolderNameProperty())
    );

    startTimeColumn.setCellValueFactory(param -> param.getValue().startTimeProperty());
    startTimeColumn.setCellFactory(param -> new StringCell<>(this.timeService::asShortTime));
    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(StringUtils::normalizeSpace));
    playersColumn.setCellValueFactory(param -> param.getValue().numPlayersProperty());
    playersColumn.setCellFactory(param -> new StringCell<>(number -> i18n.number(number.intValue())));
    averageRatingColumn.setCellValueFactory(param -> param.getValue().averageRatingProperty());
    averageRatingColumn.setCellFactory(param -> new DecimalCell<>(new DecimalFormat("0"),
        number -> Math.round(number.doubleValue() / 100.0) * 100.0));
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(String::toString));
    modsColumn.setCellValueFactory(this::modCell);
    modsColumn.setCellFactory(param -> new StringCell<>(String::toString));
    watchColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
    watchColumn.setCellFactory(param -> new NodeTableCell<>(this::watchReplayButton));

    tableView.setItems(sortedList);

    startTimeColumn.setSortType(SortType.DESCENDING);
    tableView.getSortOrder().add(startTimeColumn);
    tableView.sort();
  }

  private Node watchReplayButton(GameBean game) {
    WatchButtonController controller = uiService.loadFxml("theme/vault/replay/watch_button.fxml");
    controller.setGame(game);
    return controller.getRoot();
  }

  @Override
  public Node getRoot() {
    return root;
  }

  @NotNull
  private ObservableValue<String> modCell(CellDataFeatures<GameBean, String> param) {
    Map<String, String> simMods = param.getValue().getSimMods();
    int simModCount = simMods.size();
    List<String> modNames;
    modNames = simMods.entrySet().stream()
        .limit(2)
        .map(Entry::getValue)
        .collect(Collectors.toList());
    if (simModCount > 2) {
      return new SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames.get(0), simMods.size() - 1));
    }
    return new SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames));
  }

  public void onFilterButtonClicked() {
    if (gameFilterPopup == null) {
      gameFilterPopup = PopupUtil.createPopup(AnchorLocation.CONTENT_TOP_LEFT, liveGamesFilterController.getRoot());
    }

    if (gameFilterPopup.isShowing()) {
      gameFilterPopup.hide();
    } else {
      Bounds screenBounds = filterButton.localToScreen(filterButton.getBoundsInLocal());
      gameFilterPopup.show(filterButton.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY() + 10);
    }
  }
}
