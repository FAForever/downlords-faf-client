package com.faforever.client.ui.statusbar;

import com.faforever.client.chat.ChatService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.Version;
import com.google.common.base.Strings;
import com.jfoenix.controls.JFXProgressBar;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static javafx.application.Platform.runLater;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StatusBarController implements Controller<Node>, DisposableBean {
  private static final PseudoClass CONNECTIVITY_CONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("connected");
  private static final PseudoClass CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("disconnected");
  private static final PseudoClass MEMORY_GOOD_PSEUDO_CLASS = PseudoClass.getPseudoClass("good");
  private static final PseudoClass MEMORY_WARN_PSEUDO_CLASS = PseudoClass.getPseudoClass("warn");
  private static final PseudoClass MEMORY_BAD_PSEUDO_CLASS = PseudoClass.getPseudoClass("bad");

  private final FafService fafService;
  private final I18n i18n;
  private final ChatService chatService;
  private final TaskService taskService;
  private final PreferencesService preferencesService;

  public Label chatConnectionStatusIcon;
  public Label fafConnectionStatusIcon;
  public MenuButton fafConnectionButton;
  public MenuButton chatConnectionButton;
  public ProgressBar taskProgressBar;
  public Pane taskPane;
  public Label taskProgressLabel;
  public Label versionLabel;
  public HBox memoryPane;
  public Label memoryLabel;
  public JFXProgressBar memoryProgressbar;
  public Label warningIcon;
  private Timeline memoryListener;

  public StatusBarController(FafService fafService, I18n i18n, ChatService chatService, TaskService taskService, PreferencesService preferencesService) {
    this.fafService = fafService;
    this.i18n = i18n;
    this.chatService = chatService;
    this.taskService = taskService;
    this.preferencesService = preferencesService;
  }

  @Override
  public void initialize() {
    setCurrentWorkerInStatusBar(null);
    versionLabel.setText(Version.getCurrentVersion());
    taskPane.managedProperty().bind(taskPane.visibleProperty());
    taskProgressBar.managedProperty().bind(taskProgressBar.visibleProperty());
    taskProgressLabel.managedProperty().bind(taskProgressBar.visibleProperty());

    JavaFxUtil.addListener(fafService.connectionStateProperty(), (observable, oldValue, newValue) -> runLater(() -> {
      switch (newValue) {
        case DISCONNECTED:
          fafConnectionButton.setText(i18n.get("statusBar.fafDisconnected"));
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true);
          break;
        case CONNECTING:
          fafConnectionButton.setText(i18n.get("statusBar.fafConnecting"));
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
          break;
        case CONNECTED:
          fafConnectionButton.setText(i18n.get("statusBar.fafConnected"));
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true);
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
          break;
      }
    }));

    JavaFxUtil.addListener(chatService.connectionStateProperty(), (observable, oldValue, newValue) -> runLater(() -> {
      chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
      chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
      switch (newValue) {
        case DISCONNECTED:
          chatConnectionButton.setText(i18n.get("statusBar.chatDisconnected"));
          chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true);
          break;
        case CONNECTING:
          chatConnectionButton.setText(i18n.get("statusBar.chatConnecting"));
          break;
        case CONNECTED:
          chatConnectionButton.setText(i18n.get("statusBar.chatConnected"));
          chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true);
          break;
      }
    }));

    JavaFxUtil.addListener(taskService.getActiveWorkers(), (Observable observable) -> {
      Collection<Worker<?>> runningWorkers = taskService.getActiveWorkers();
      if (runningWorkers.isEmpty()) {
        setCurrentWorkerInStatusBar(null);
      } else {
        setCurrentWorkerInStatusBar(runningWorkers.iterator().next());
      }
    });

    memoryListener = new Timeline(new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      long heapSize = Runtime.getRuntime().totalMemory();
      long maxHeap = Runtime.getRuntime().maxMemory();
      memoryLabel.setText(heapSize / 1000000 + " MB / " + maxHeap / 1000000 + " MB");
      double usageProcentage = heapSize / (double) maxHeap;
      memoryProgressbar.setProgress(usageProcentage);
      memoryProgressbar.pseudoClassStateChanged(MEMORY_WARN_PSEUDO_CLASS, (usageProcentage > 0.8 && usageProcentage < 0.9));
      memoryProgressbar.pseudoClassStateChanged(MEMORY_BAD_PSEUDO_CLASS, (usageProcentage > 0.9));
      memoryProgressbar.pseudoClassStateChanged(MEMORY_GOOD_PSEUDO_CLASS, (usageProcentage < 0.8));
      memoryPane.setVisible(usageProcentage > 0.8 || preferencesService.getPreferences().isShowMemoryPane());
      if (usageProcentage > 0.9) {
        warningIcon.setVisible(true);
        Tooltip.install(memoryPane, new Tooltip(i18n.get("warning.memoryLeak")));
      } else {
        warningIcon.setVisible(false);
        Tooltip.uninstall(memoryPane, null);
      }
    }), new KeyFrame(javafx.util.Duration.seconds(5)));
    memoryListener.setCycleCount(Timeline.INDEFINITE);
    memoryListener.play();
  }

  @Override
  public Node getRoot() {
    return null;
  }

  /**
   * @param worker the task to set, {@code null} to unset
   */
  private void setCurrentWorkerInStatusBar(Worker<?> worker) {
    runLater(() -> {
      if (worker == null) {
        taskPane.setVisible(false);
        taskProgressBar.progressProperty().unbind();
        taskProgressLabel.textProperty().unbind();
        return;
      }

      taskPane.setVisible(true);
      taskProgressBar.progressProperty().bind(worker.progressProperty());
      taskProgressLabel.textProperty().bind(Bindings.createStringBinding(
          () -> {
            String message = worker.getMessage();
            String title = worker.getTitle();
            if (Strings.isNullOrEmpty(message)) {
              return i18n.get("statusBar.taskWithoutMessage.format", title);
            }
            return i18n.get("statusBar.taskWithMessage.format", title, message);
          },
          worker.titleProperty(), worker.messageProperty()
      ));
    });
  }

  public void onFafReconnectClicked() {
    fafService.reconnect();
  }

  public void onChatReconnectClicked() {
    chatService.reconnect();
  }

  @Override
  public void destroy() throws Exception {
    memoryListener.stop();
  }
}
