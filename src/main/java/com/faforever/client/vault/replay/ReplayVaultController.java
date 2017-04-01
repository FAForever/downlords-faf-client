package com.faforever.client.vault.replay;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.replay.LoadLocalReplaysTask;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayVaultController implements Controller<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final NotificationService notificationService;
  private final ReplayService replayService;
  private final MapService mapService;
  private final TaskService taskService;
  private final I18n i18n;
  private final TimeService timeService;
  private final ReportingService reportingService;
  private final ApplicationContext applicationContext;
  private final UiService uiService;

  public TreeTableView<Replay> replayVaultRoot;
  public TreeTableColumn<Replay, Number> idColumn;
  public TreeTableColumn<Replay, String> titleColumn;
  public TreeTableColumn<Replay, String> playersColumn;
  public TreeTableColumn<Replay, Temporal> timeColumn;
  public TreeTableColumn<Replay, Duration> durationColumn;
  public TreeTableColumn<Replay, String> gameTypeColumn;
  public TreeTableColumn<Replay, MapBean> mapColumn;
  @VisibleForTesting
  TreeItem<Replay> localReplaysRoot;

  @Inject
  // TODO reduce dependencies
  public ReplayVaultController(NotificationService notificationService, ReplayService replayService, MapService mapService, TaskService taskService, I18n i18n, TimeService timeService, ReportingService reportingService, ApplicationContext applicationContext, UiService uiService) {
    this.notificationService = notificationService;
    this.replayService = replayService;
    this.mapService = mapService;
    this.taskService = taskService;
    this.i18n = i18n;
    this.timeService = timeService;
    this.reportingService = reportingService;
    this.applicationContext = applicationContext;
    this.uiService = uiService;
  }

  @SuppressWarnings("unchecked")
  public void initialize() {
    localReplaysRoot = new TreeItem<>();
    localReplaysRoot.setExpanded(true);

    TreeItem<Replay> tableRoot = new TreeItem<>(new Replay("invisibleRootItem"));

    replayVaultRoot.setRoot(tableRoot);
    replayVaultRoot.setRowFactory(param -> replayRowFactory());
    replayVaultRoot.getSortOrder().setAll(Collections.singletonList(timeColumn));

    idColumn.setCellValueFactory(param -> param.getValue().getValue().idProperty());
    idColumn.setCellFactory(this::idCellFactory);

    titleColumn.setCellValueFactory(param -> param.getValue().getValue().titleProperty());

    timeColumn.setCellValueFactory(param -> param.getValue().getValue().startTimeProperty());
    timeColumn.setCellFactory(this::timeCellFactory);
    timeColumn.setSortType(TreeTableColumn.SortType.DESCENDING);

    gameTypeColumn.setCellValueFactory(param -> param.getValue().getValue().getFeaturedMod().displayNameProperty());

    mapColumn.setCellValueFactory(param -> param.getValue().getValue().mapProperty());
    mapColumn.setCellFactory(this::mapCellFactory);

    playersColumn.setCellValueFactory(this::playersValueFactory);

    durationColumn.setCellValueFactory(this::durationCellValueFactory);
    durationColumn.setCellFactory(this::durationCellFactory);
  }

  @NotNull
  private TreeTableRow<Replay> replayRowFactory() {
    TreeTableRow<Replay> row = new TreeTableRow<>();
    row.setOnMouseClicked(event -> {
      // If ID == 0, this isn't an entry but root node
      if (event.getClickCount() == 2 && !row.isEmpty() && row.getItem().getId() != 0) {
        replayService.runReplay(row.getItem());
      }
    });
    return row;
  }

  private ObservableValue<String> playersValueFactory(TreeTableColumn.CellDataFeatures<Replay, String> features) {
    return new StringBinding() {
      @Override
      protected String computeValue() {
        Replay replay = features.getValue().getValue();

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

  private TreeTableCell<Replay, Temporal> timeCellFactory(TreeTableColumn<Replay, Temporal> column) {
    TextFieldTreeTableCell<Replay, Temporal> cell = new TextFieldTreeTableCell<>();
    cell.setConverter(new StringConverter<Temporal>() {
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

  private TreeTableCell<Replay, MapBean> mapCellFactory(TreeTableColumn<Replay, MapBean> column) {
    final ImageView imageVew = uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml");

    TreeTableCell<Replay, MapBean> cell = new TreeTableCell<Replay, MapBean>() {

      @Override
      protected void updateItem(MapBean map, boolean empty) {
        super.updateItem(map, empty);

        if (empty || map == null) {
          setText(null);
          setGraphic(null);
        } else {
          imageVew.setImage(mapService.loadPreview(map, PreviewSize.SMALL));
          setGraphic(imageVew);
          setText(map.getDisplayName());
        }
      }
    };
    cell.setGraphic(imageVew);

    return cell;
  }

  private TreeTableCell<Replay, Number> idCellFactory(TreeTableColumn<Replay, Number> column) {
    TextFieldTreeTableCell<Replay, Number> cell = new TextFieldTreeTableCell<>();
    cell.setConverter(new StringConverter<Number>() {
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

  private TreeTableCell<Replay, Duration> durationCellFactory(TreeTableColumn<Replay, Duration> column) {
    TextFieldTreeTableCell<Replay, Duration> cell = new TextFieldTreeTableCell<>();
    cell.setConverter(new StringConverter<Duration>() {
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
  private ObservableValue<Duration> durationCellValueFactory(TreeTableColumn.CellDataFeatures<Replay, Duration> param) {
    Replay replay = param.getValue().getValue();
    Temporal startTime = replay.getStartTime();
    Temporal endTime = replay.getEndTime();

    if (startTime == null || endTime == null) {
      return new SimpleObjectProperty<>(null);
    }

    return new SimpleObjectProperty<>(Duration.between(startTime, endTime));
  }

  public CompletableFuture<Void> loadLocalReplaysInBackground() {
    // TODO use replay service
    LoadLocalReplaysTask task = applicationContext.getBean(LoadLocalReplaysTask.class);

    localReplaysRoot.getChildren().clear();
    return taskService.submitTask(task).getFuture()
        .thenAccept(this::addLocalReplays)
        .exceptionally(throwable -> {
              logger.warn("Error while loading local replays", throwable);
              notificationService.addNotification(new PersistentNotification(
                  i18n.get("replays.loadingLocalTask.failed"),
                  Severity.ERROR, asList(new ReportAction(i18n, reportingService, throwable), new DismissAction(i18n))
              ));
              return null;
            }
        );
  }

  private void addLocalReplays(Collection<Replay> result) {
    Collection<TreeItem<Replay>> items = result.stream()
        .map(TreeItem::new).collect(Collectors.toCollection(ArrayList::new));
    Platform.runLater(() -> localReplaysRoot.getChildren().addAll(items));
  }

//  public void loadOnlineReplaysInBackground() {
//    replayService.getOnlineReplays()
//        .thenAccept(this::addOnlineReplays)
//        .exceptionally(throwable -> {
//          logger.warn("Error while loading online replays", throwable);
//          notificationService.addNotification(new PersistentNotification(
//              i18n.get("replays.loadingOnlineTask.failed"),
//              Severity.ERROR,
//              Collections.singletonList(new Action(i18n.get("report"), event -> reportingService.reportError(throwable)))
//          ));
//          return null;
//        });
//  }

//  private void addOnlineReplays(Collection<ReplayInfoBean> result) {
//    Collection<TreeItem<ReplayInfoBean>> items = result.stream()
//        .map(TreeItem::new).collect(Collectors.toCollection(ArrayList::new));
//    Platform.runLater(() -> onlineReplaysRoot.getChildren().addAll(items));
//  }

  public Node getRoot() {
    return replayVaultRoot;
  }
}
