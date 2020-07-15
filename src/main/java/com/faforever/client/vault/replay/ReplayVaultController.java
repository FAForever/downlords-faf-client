package com.faforever.client.vault.replay;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.LocalReplaysChangedEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.map.MapPreviewTableCellController;
import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Pagination;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO reduce dependencies
public class ReplayVaultController extends AbstractViewController<Node> {

  private final ReplayService replayService;
  private final MapService mapService;
  private final I18n i18n;
  private final TimeService timeService;
  private final UiService uiService;
  private final EventBus eventBus;

  public Pane replayVaultRoot;
  public VBox loadingPane;
  public TableView<Replay> replayTableView;
  public TableColumn<Replay, Number> idColumn;
  public TableColumn<Replay, String> titleColumn;
  public TableColumn<Replay, String> playersColumn;
  public TableColumn<Replay, Temporal> timeColumn;
  public TableColumn<Replay, Duration> durationColumn;
  public TableColumn<Replay, String> gameTypeColumn;
  public TableColumn<Replay, MapBean> mapColumn;
  public Pagination pagination;

  private Boolean isDisplayingForFirstTime = true;

  @Override
  public void initialize() {
    replayTableView.setRowFactory(param -> replayRowFactory());
    replayTableView.getSortOrder().setAll(Collections.singletonList(timeColumn));

    idColumn.setCellValueFactory(param -> param.getValue().idProperty());
    idColumn.setCellFactory(this::idCellFactory);

    titleColumn.setCellValueFactory(param -> param.getValue().titleProperty());

    timeColumn.setCellValueFactory(param -> param.getValue().startTimeProperty());
    timeColumn.setCellFactory(this::timeCellFactory);
    timeColumn.setSortType(TableColumn.SortType.DESCENDING);

    gameTypeColumn.setCellValueFactory(param -> param.getValue().getFeaturedMod().displayNameProperty());

    mapColumn.setCellValueFactory(param -> param.getValue().mapProperty());
    mapColumn.setCellFactory(this::mapCellFactory);

    playersColumn.setCellValueFactory(this::playersValueFactory);

    durationColumn.setCellValueFactory(this::durationCellValueFactory);
    durationColumn.setCellFactory(this::durationCellFactory);

    pagination.managedProperty().bind(pagination.visibleProperty());
    pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() != oldValue.intValue()) {
        replayService.loadPage(newValue.intValue() + 1);
      }
    });

    eventBus.register(this);
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (isDisplayingForFirstTime) {
      replayTableView.setVisible(false);
      replayService.startLoadingAndWatchingLocalReplays();
      isDisplayingForFirstTime = false;
    }

    super.onDisplay(navigateEvent);
  }

  @NotNull
  private TableRow<Replay> replayRowFactory() {
    TableRow<Replay> row = new TableRow<>();
    ContextMenu contextMenu = replayRowContextMenuFactory(row);
    row.setContextMenu(contextMenu);

    row.setOnMouseClicked(event -> {
      // If ID == 0, this isn't an entry but root node
      if (event.getClickCount() == 2 && !row.isEmpty() && row.getItem().getId() != 0) {
        replayService.runReplay(row.getItem());
      }
    });
    return row;
  }

  @NotNull
  private ContextMenu replayRowContextMenuFactory(TableRow<Replay> row) {
    ContextMenu contextMenu = new ContextMenu();
    MenuItem copyReplayId = new MenuItem(i18n.get("vault.replays.copyReplayId"));
    copyReplayId.setOnAction(event -> {
      int replayId = row.getItem().getId();
      ClipboardUtil.copyToClipboard(String.valueOf(replayId));
    });
    MenuItem copyReplayPath = new MenuItem(i18n.get("vault.replays.copyReplayPath"));
    copyReplayPath.setOnAction(event -> {
      Path replayPath = row.getItem().getReplayFile();
      ClipboardUtil.copyToClipboard(replayPath.toFile().getAbsolutePath());
    });
    contextMenu.getItems().addAll(copyReplayId, copyReplayPath);

    return contextMenu;
  }

  private ObservableValue<String> playersValueFactory(TableColumn.CellDataFeatures<Replay, String> features) {
    return new StringBinding() {
      @Override
      protected String computeValue() {
        Replay replay = features.getValue();

        ObservableMap<String, List<String>> teams = replay.getTeams();
        if (teams.isEmpty()) {
          return "";
        }

        ArrayList<String> teamsAsStrings = new ArrayList<>();
        for (List<String> playerNames : teams.values()) {
          Collections.sort(playerNames);
          teamsAsStrings.add(Joiner.on(i18n.get("textSeparator")).join(playerNames));
        }

        return Joiner.on(i18n.get("vsSeparator")).join(teamsAsStrings);
      }
    };
  }

  private TableCell<Replay, Temporal> timeCellFactory(TableColumn<Replay, Temporal> column) {
    TextFieldTableCell<Replay, Temporal> cell = new TextFieldTableCell<>();
    cell.setConverter(new StringConverter<>() {
      @Override
      public String toString(Temporal object) {
        return timeService.lessThanOneDayAgo(object);
      }

      @Override
      public OffsetDateTime fromString(String string) {
        return null;
      }
    });
    return cell;
  }

  private TableCell<Replay, MapBean> mapCellFactory(TableColumn<Replay, MapBean> column) {
    MapPreviewTableCellController controller = uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml");
    final ImageView imageView = controller.getRoot();

    TableCell<Replay, MapBean> cell = new TableCell<>() {

      @Override
      protected void updateItem(MapBean map, boolean empty) {
        super.updateItem(map, empty);

        if (empty) {
          setText(null);
          setGraphic(null);
        } else if (map == null) {
          setGraphic(null);
          setText(i18n.get("map.unknown"));
        } else {
          imageView.setImage(mapService.loadPreview(map.getFolderName(), PreviewSize.SMALL));
          setGraphic(imageView);
          setText(map.getDisplayName());
        }
      }
    };
    cell.setGraphic(imageView);

    return cell;
  }

  private TableCell<Replay, Number> idCellFactory(TableColumn<Replay, Number> column) {
    TextFieldTableCell<Replay, Number> cell = new TextFieldTableCell<>();
    cell.setConverter(new StringConverter<>() {
      @Override
      public String toString(Number object) {
        if (object.intValue() == 0) {
          return "";
        }
        return i18n.number(object.intValue());
      }

      @Override
      public Number fromString(String string) {
        return null;
      }
    });
    return cell;
  }

  private TableCell<Replay, Duration> durationCellFactory(TableColumn<Replay, Duration> column) {
    TextFieldTableCell<Replay, Duration> cell = new TextFieldTableCell<>();
    cell.setConverter(new StringConverter<>() {
      @Override
      public String toString(Duration object) {
        if (object == null) {
          return "";
        }
        return timeService.shortDuration(object);
      }

      @Override
      public Duration fromString(String string) {
        return null;
      }
    });
    return cell;
  }

  @NotNull
  private ObservableValue<Duration> durationCellValueFactory(TableColumn.CellDataFeatures<Replay, Duration> param) {
    Replay replay = param.getValue();
    Temporal startTime = replay.getStartTime();
    Temporal endTime = replay.getEndTime();

    if (startTime == null || endTime == null) {
      return new SimpleObjectProperty<>(null);
    }

    return new SimpleObjectProperty<>(Duration.between(startTime, endTime));
  }

  @Subscribe
  public void onLocalReplaysChanged(LocalReplaysChangedEvent event) {
    Platform.runLater(() -> loadReplays(event));
  }

  private void loadReplays(LocalReplaysChangedEvent event) {
    pagination.setCurrentPageIndex(event.getPage() - 1);
    pagination.setPageCount(event.getTotalPages());
    replayTableView.getItems().setAll(event.getReplays());
    replayTableView.sort();
    replayTableView.setVisible(true);
    loadingPane.setVisible(false);
  }

  @Override
  public Node getRoot() {
    return replayVaultRoot;
  }
}
