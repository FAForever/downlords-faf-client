package com.faforever.client.ui.statusbar;

import com.faforever.client.chat.ChatService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.google.common.base.Strings;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;

import static javafx.application.Platform.runLater;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StatusBarController implements Controller<Node> {
  private static final PseudoClass CONNECTIVITY_CONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("connected");
  private static final PseudoClass CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("disconnected");

  private final FafService fafService;
  private final I18n i18n;
  private final ChatService chatService;
  private final TaskService taskService;

  public Label chatConnectionStatusIcon;
  public Label fafConnectionStatusIcon;
  public MenuButton fafConnectionButton;
  public MenuButton chatConnectionButton;
  public ProgressBar taskProgressBar;
  public Pane taskPane;
  public Label taskProgressLabel;

  @Inject
  public StatusBarController(FafService fafService, I18n i18n, ChatService chatService, TaskService taskService) {
    this.fafService = fafService;
    this.i18n = i18n;
    this.chatService = chatService;
    this.taskService = taskService;
  }

  public void initialize() {
    setCurrentWorkerInStatusBar(null);

    fafService.connectionStateProperty().addListener((observable, oldValue, newValue) -> runLater(() -> {
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

    chatService.connectionStateProperty().addListener((observable, oldValue, newValue) -> runLater(() -> {
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

    taskService.getActiveWorkers().addListener((Observable observable) -> {
      Collection<Worker<?>> runningWorkers = taskService.getActiveWorkers();
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
}
