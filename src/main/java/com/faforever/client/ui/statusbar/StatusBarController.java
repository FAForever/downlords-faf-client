package com.faforever.client.ui.statusbar;

import com.faforever.client.chat.ChatService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.Version;
import com.faforever.client.user.LoginService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class StatusBarController implements Controller<Node> {
  private static final PseudoClass CONNECTIVITY_CONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("connected");
  private static final PseudoClass CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("disconnected");

  private final LoginService loginService;
  private final I18n i18n;
  private final ChatService chatService;
  private final TaskService taskService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Label chatConnectionStatusIcon;
  public Label fafConnectionStatusIcon;
  public MenuButton fafConnectionButton;
  public MenuButton chatConnectionButton;
  public ProgressBar taskProgressBar;
  public Pane taskPane;
  public Label taskProgressLabel;
  public Label versionLabel;

  @Override
  public void initialize() {
    setCurrentWorkerInStatusBar(null);
    versionLabel.setText(Version.getCurrentVersion());

    JavaFxUtil.addListener(loginService.connectionStateProperty(), (SimpleChangeListener<ConnectionState>) newValue -> fxApplicationThreadExecutor.execute(() -> {
      switch (newValue) {
        case DISCONNECTED -> {
          fafConnectionButton.setText(i18n.get("statusBar.fafDisconnected"));
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true);
        }
        case CONNECTING -> {
          fafConnectionButton.setText(i18n.get("statusBar.fafConnecting"));
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
        }
        case CONNECTED -> {
          fafConnectionButton.setText(i18n.get("statusBar.fafConnected"));
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true);
          fafConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
        }
      }
    }));

    JavaFxUtil.addListener(chatService.connectionStateProperty(), (SimpleChangeListener<ConnectionState>) newValue -> fxApplicationThreadExecutor.execute(() -> {
      chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false);
      chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false);
      switch (newValue) {
        case DISCONNECTED -> {
          chatConnectionButton.setText(i18n.get("statusBar.chatDisconnected"));
          chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true);
        }
        case CONNECTING -> chatConnectionButton.setText(i18n.get("statusBar.chatConnecting"));
        case CONNECTED -> {
          chatConnectionButton.setText(i18n.get("statusBar.chatConnected"));
          chatConnectionStatusIcon.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true);
        }
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
  }

  @Override
  public Node getRoot() {
    return null;
  }

  /**
   * @param worker the task to set, {@code null} to unset
   */
  private void setCurrentWorkerInStatusBar(Worker<?> worker) {
    fxApplicationThreadExecutor.execute(() -> {
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
    loginService.reconnectToLobby();
  }

  public void onChatReconnectClicked() {
    chatService.reconnect();
  }
}
