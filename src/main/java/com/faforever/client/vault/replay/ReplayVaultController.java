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
import com.faforever.client.vault.map.MapPreviewTableCellController;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
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

  public TableView<Replay> replayVaultRoot;
  public TableColumn<Replay, Number> idColumn;
  public TableColumn<Replay, String> titleColumn;
  public TableColumn<Replay, String> playersColumn;
  public TableColumn<Replay, Temporal> timeColumn;
  public TableColumn<Replay, Duration> durationColumn;
  public TableColumn<Replay, String> gameTypeColumn;
  public TableColumn<Replay, MapBean> mapColumn;

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

    replayVaultRoot.setRowFactory(param -> replayRowFactory());
    replayVaultRoot.getSortOrder().setAll(Collections.singletonList(timeColumn));

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

    loadLocalReplaysInBackground();
  }

  @NotNull
  private TableRow<Replay> replayRowFactory() {
    TableRow<Replay> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
      // If ID == 0, this isn't an entry but root node
      if (event.getClickCount() == 2 && !row.isEmpty() && row.getItem().getId() != 0) {
        replayService.runReplay(row.getItem());
      }
    });
    return row;
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

  private TableCell<Replay, MapBean> mapCellFactory(TableColumn<Replay, MapBean> column) {
    MapPreviewTableCellController controller = uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml");
    final ImageView imageView = controller.getRoot();

    TableCell<Replay, MapBean> cell = new TableCell<Replay, MapBean>() {

      @Override
      protected void updateItem(MapBean map, boolean empty) {
        super.updateItem(map, empty);

        if (empty || map == null) {
          setText(null);
          setGraphic(null);
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

  private TableCell<Replay, Duration> durationCellFactory(TableColumn<Replay, Duration> column) {
    TextFieldTableCell<Replay, Duration> cell = new TextFieldTableCell<>();
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
  private ObservableValue<Duration> durationCellValueFactory(TableColumn.CellDataFeatures<Replay, Duration> param) {
    Replay replay = param.getValue();
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

    replayVaultRoot.getItems().clear();
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
    Collection<Replay> items = result.stream()
        .collect(Collectors.toCollection(ArrayList::new));
    Platform.runLater(() -> replayVaultRoot.getItems().addAll(items));
  }

//  public void loadOnlineReplaysInBackground() {
//    replayService.getOnlineReplays()
//        .thenAccept(this::addOnlineReplays)
//        .exceptionally(throwable -> {
//          logger.warn("Error while loading online replays", throwable);
//          notificationService.addNotification(new PersistentNotification(
//              i18n.valueOf("replays.loadingOnlineTask.failed"),
//              Severity.ERROR,
//              Collections.singletonList(new Action(i18n.valueOf("report"), event -> reportingService.reportError(throwable)))
//          ));
//          return null;
//        });
//  }

//  private void addOnlineReplays(Collection<ReplayInfoBean> result) {
//    Collection<Item<ReplayInfoBean>> items = result.stream()
//        .map(Item::new).collect(Collectors.toCollection(ArrayList::new));
//    Platform.runLater(() -> onlineReplaysRoot.getChildren().addAll(items));
//  }

  public Node getRoot() {
    return replayVaultRoot;
  }
}
