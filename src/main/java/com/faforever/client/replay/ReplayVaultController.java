package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.TimeService;
import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReplayVaultController {

  @FXML
  TreeTableView<ReplayInfoBean> replaysTable;

  @FXML
  TreeTableColumn<ReplayInfoBean, Number> idColumn;

  @FXML
  TreeTableColumn<ReplayInfoBean, String> titleColumn;

  @FXML
  TreeTableColumn<ReplayInfoBean, String> playersColumn;

  @FXML
  TreeTableColumn<ReplayInfoBean, Instant> timeColumn;

  @FXML
  TreeTableColumn<ReplayInfoBean, Duration> durationColumn;

  @FXML
  TreeTableColumn<ReplayInfoBean, String> gameTypeColumn;

  @FXML
  TreeTableColumn<ReplayInfoBean, String> mapColumn;

  @Autowired
  NotificationService notificationService;

  @Autowired
  ReplayService replayService;

  @Autowired
  TaskService taskService;

  @Autowired
  I18n i18n;

  @Autowired
  TimeService timeService;

  private TreeItem<ReplayInfoBean> localReplaysRoot;

  private TreeItem<ReplayInfoBean> onlineReplaysRoot;


  @SuppressWarnings("unchecked")
  @PostConstruct
  void postConstruct() {
    localReplaysRoot = new TreeItem<>(new ReplayInfoBean(i18n.get("replays.localReplays")));
    localReplaysRoot.setExpanded(true);

    onlineReplaysRoot = new TreeItem<>(new ReplayInfoBean(i18n.get("replays.onlineReplays")));
    onlineReplaysRoot.setExpanded(true);

    TreeItem<ReplayInfoBean> tableRoot = new TreeItem<>(new ReplayInfoBean("invisibleRootItem"));
    tableRoot.getChildren().addAll(localReplaysRoot, onlineReplaysRoot);

    replaysTable.setRoot(tableRoot);
    replaysTable.setRowFactory(param -> replayRowFactory());

    idColumn.setCellValueFactory(param -> param.getValue().getValue().idProperty());
    idColumn.setCellFactory(this::idCellFactory);

    titleColumn.setCellValueFactory(param -> param.getValue().getValue().titleProperty());

    timeColumn.setCellValueFactory(param -> param.getValue().getValue().startTimeProperty());
    timeColumn.setCellFactory(this::timeCellFactory);

    gameTypeColumn.setCellValueFactory(param -> param.getValue().getValue().gameTypeProperty());
    mapColumn.setCellValueFactory(param -> param.getValue().getValue().mapProperty());

    playersColumn.setCellValueFactory(this::playersValueFactory);

    durationColumn.setCellValueFactory(this::durationCellValueFactory);
    durationColumn.setCellFactory(this::durationCellFactory);
  }

  @NotNull
  private TreeTableRow<ReplayInfoBean> replayRowFactory() {
    TreeTableRow<ReplayInfoBean> row = new TreeTableRow<>();
    row.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2 && !row.isEmpty()) {
        replayService.runReplay(row.getItem());
      }
    });
    return row;
  }

  private ObservableValue<String> playersValueFactory(TreeTableColumn.CellDataFeatures<ReplayInfoBean, String> features) {
    return new StringBinding() {
      @Override
      protected String computeValue() {
        ReplayInfoBean replayInfoBean = features.getValue().getValue();

        ObservableMap<String, List<String>> teams = replayInfoBean.getTeams();
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

  private TreeTableCell<ReplayInfoBean, Instant> timeCellFactory(TreeTableColumn<ReplayInfoBean, Instant> column) {
    TextFieldTreeTableCell<ReplayInfoBean, Instant> cell = new TextFieldTreeTableCell<>();
    cell.setConverter(new StringConverter<Instant>() {
      @Override
      public String toString(Instant object) {
        return timeService.lessThanOneDayAgo(object);
      }

      @Override
      public Instant fromString(String string) {
        return null;
      }
    });
    return cell;
  }

  private TreeTableCell<ReplayInfoBean, Number> idCellFactory(TreeTableColumn<ReplayInfoBean, Number> column) {
    TextFieldTreeTableCell<ReplayInfoBean, Number> cell = new TextFieldTreeTableCell<>();
    cell.setConverter(new StringConverter<Number>() {
      @Override
      public String toString(Number object) {
        if (object.intValue() == 0) {
          return "";
        }
        return String.valueOf(object.intValue());
      }

      @Override
      public Number fromString(String string) {
        return null;
      }
    });
    return cell;
  }

  private TreeTableCell<ReplayInfoBean, Duration> durationCellFactory(TreeTableColumn<ReplayInfoBean, Duration> column) {
    TextFieldTreeTableCell<ReplayInfoBean, Duration> cell = new TextFieldTreeTableCell<>();
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
  private ObservableValue<Duration> durationCellValueFactory(TreeTableColumn.CellDataFeatures<ReplayInfoBean, Duration> param) {
    ReplayInfoBean replayInfoBean = param.getValue().getValue();
    Instant startTime = replayInfoBean.getStartTime();
    Instant endTime = replayInfoBean.getEndTime();

    if (startTime == null || endTime == null) {
      return new SimpleObjectProperty<>(null);
    }

    return new SimpleObjectProperty<>(Duration.between(startTime, endTime));
  }

  public void loadLocalReplaysInBackground() {
    taskService.submitTask(TaskGroup.DISK, new PrioritizedTask<Collection<ReplayInfoBean>>(i18n.get("replays.loadingLocalTask.title")) {
      @Override
      protected Collection<ReplayInfoBean> call() throws Exception {
        return replayService.getLocalReplays();
      }
    }, new Callback<Collection<ReplayInfoBean>>() {
      @Override
      public void success(Collection<ReplayInfoBean> result) {
        addLocalReplays(result);
      }

      @Override
      public void error(Throwable e) {
        notificationService.addNotification(new PersistentNotification(
            i18n.get("replays.loadingLocalTask.failed"),
            Severity.ERROR
        ));
      }
    });
  }

  private void addLocalReplays(Collection<ReplayInfoBean> result) {
    Collection<TreeItem<ReplayInfoBean>> items = new ArrayList<>();

    for (ReplayInfoBean bean : result) {
      items.add(new TreeItem<>(bean));
    }

    Platform.runLater(() -> localReplaysRoot.getChildren().addAll(items));
  }

  public Node getRoot() {
    return replaysTable;
  }
}
