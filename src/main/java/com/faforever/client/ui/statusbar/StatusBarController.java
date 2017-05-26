package com.faforever.client.ui.statusbar;

import com.faforever.client.chat.ChatService;
import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.Version;
import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static javafx.application.Platform.runLater;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StatusBarController implements Controller<Node> {
  private static final PseudoClass CONNECTIVITY_PUBLIC_PSEUDO_CLASS = PseudoClass.getPseudoClass("public");
  private static final PseudoClass CONNECTIVITY_STUN_PSEUDO_CLASS = PseudoClass.getPseudoClass("stun");
  private static final PseudoClass CONNECTIVITY_BLOCKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("blocked");
  private static final PseudoClass CONNECTIVITY_UNKNOWN_PSEUDO_CLASS = PseudoClass.getPseudoClass("unknown");
  private static final PseudoClass CONNECTIVITY_CONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("connected");
  private static final PseudoClass CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("disconnected");

  private final FafService fafService;
  private final I18n i18n;
  private final ChatService chatService;
  private final TaskService taskService;
  private final ConnectivityService connectivityService;
  private final PlatformService platformService;

  public Label chatConnectionStatusIcon;
  public Label fafConnectionStatusIcon;
  public MenuButton fafConnectionButton;
  public MenuButton chatConnectionButton;
  public ProgressBar taskProgressBar;
  public Pane taskPane;
  public Label taskProgressLabel;
  public Label versionLabel;
  public Label portCheckStatusIcon;
  public Labeled portCheckStatusButton;

  @Inject
  public StatusBarController(FafService fafService, I18n i18n, ChatService chatService, TaskService taskService,
                             ConnectivityService connectivityService, PlatformService platformService) {
    this.fafService = fafService;
    this.i18n = i18n;
    this.chatService = chatService;
    this.taskService = taskService;
    this.connectivityService = connectivityService;
    this.platformService = platformService;
  }

  public void initialize() {
    setCurrentWorkerInStatusBar(null);
    versionLabel.setText(Version.VERSION);

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
    connectivityService.connectivityStateProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> {
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_PUBLIC_PSEUDO_CLASS, false);
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_STUN_PSEUDO_CLASS, false);
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_BLOCKED_PSEUDO_CLASS, false);
        portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_UNKNOWN_PSEUDO_CLASS, false);
        switch (newValue) {
          case PUBLIC:
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_PUBLIC_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.connectivityPublic"));
            break;
          case STUN:
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_STUN_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.connectivityStun"));
            break;
          case BLOCKED:
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_BLOCKED_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.portUnreachable"));
            break;
          case RUNNING:
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_UNKNOWN_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.checkingPort"));
            break;
          case UNKNOWN:
            portCheckStatusIcon.pseudoClassStateChanged(CONNECTIVITY_UNKNOWN_PSEUDO_CLASS, true);
            portCheckStatusButton.setText(i18n.get("statusBar.connectivityUnknown"));
            break;
          default:
            throw new AssertionError("Uncovered value: " + newValue);
        }
      });
    });

    JavaFxUtil.addListener(taskService.getActiveWorkers(), (ListChangeListener<Worker<?>>) c -> {
      ObservableList<Worker<?>> runningWorkers = taskService.getActiveWorkers();
      if (runningWorkers.isEmpty()) {
        setCurrentWorkerInStatusBar(null);
      } else {
        setCurrentWorkerInStatusBar(runningWorkers.iterator().next());
      }
    });
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

  public void onPortCheckRetryClicked() {
    connectivityService.checkConnectivity();
  }

  public void onPortCheckHelpClicked(ActionEvent actionEvent) {
    platformService.showDocument("http://wiki.faforever.com/index.php?title=Connection_issues_and_solutions");
  }
}
