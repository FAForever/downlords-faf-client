package com.faforever.client.notification;

import com.faforever.client.fx.NodeController;
import com.faforever.client.theme.ThemeService;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for a single persistent notification entry.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PersistentNotificationController extends NodeController<Node> {

  private static final String CSS_STYLE_INFO = "info";
  private static final String CSS_STYLE_WARN = "warn";
  private static final String CSS_STYLE_ERROR = "error";

  private final NotificationService notificationService;
  private final ThemeService themeService;

  public Node notificationRoot;
  public Label messageLabel;
  public Region icon;
  public HBox actionButtonsContainer;
  public ImageView closeImage;

  private PersistentNotification notification;

  @Override
  protected void onInitialize() {
    closeImage.setImage(themeService.getThemeImage("theme/images/small_close.png"));
  }

  /**
   * Sets the notification to display. Populates corresponding UI elements.
   */
  public void setNotification(PersistentNotification notification) {
    this.notification = notification;
    messageLabel.setText(notification.text());
    setImageBasedOnSeverity(notification.severity());
    setActions(notification.actions());
  }

  private void setImageBasedOnSeverity(Severity severity) {
    ObservableList<String> styleClasses = icon.getStyleClass();
    styleClasses.removeAll(CSS_STYLE_INFO, CSS_STYLE_WARN, CSS_STYLE_ERROR);

    switch (severity) {
      case INFO:
        styleClasses.addAll(CSS_STYLE_INFO, "info-icon");
        break;
      case WARN:
        styleClasses.addAll(CSS_STYLE_INFO, "warn-icon");
        styleClasses.add(CSS_STYLE_WARN);
        break;
      case ERROR:
        styleClasses.addAll(CSS_STYLE_INFO, "error-icon");
        styleClasses.add(CSS_STYLE_ERROR);
        break;
      default:
        throw new IllegalStateException("Unhandled severity: " + severity);
    }
  }

  private void setActions(List<Action> actions) {
    if (actions == null) {
      return;
    }

    List<Button> actionButtons = new ArrayList<>();
    for (Action action : actions) {
      Button button = new Button(action.getTitle());
      button.setFocusTraversable(false);
      button.setOnAction(event -> {
        action.call();
        if (action.getType() == Action.Type.OK_DONE) {
          dismiss();
        }
      });

      actionButtons.add(button);
    }

    actionButtonsContainer.getChildren().setAll(actionButtons);
  }

  private void dismiss() {
    notificationService.removeNotification(notification);
  }

  @Override
  public Node getRoot() {
    return notificationRoot;
  }

  public void onCloseButtonClicked() {
    dismiss();
  }
}
