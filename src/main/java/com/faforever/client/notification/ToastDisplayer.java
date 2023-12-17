package com.faforever.client.notification;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.PopupUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Screen;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ToastDisplayer implements InitializingBean {
  private final UiService uiService;
  private final NotificationPrefs notificationPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private TransientNotificationsController transientNotificationsController;
  private Popup transientNotificationsPopup;
  private ObservableValue<AnchorDetails> anchorDetails;

  @Override
  public void afterPropertiesSet() {
    transientNotificationsController = uiService.loadFxml("theme/transient_notifications.fxml");
    transientNotificationsPopup = PopupUtil.createPopup(transientNotificationsController.getRoot());
    transientNotificationsPopup.getScene().getRoot().getStyleClass().add("transient-notification");

    notificationPrefs.transientNotificationsEnabledProperty().subscribe(enabled -> {
      if (enabled) {
        fxApplicationThreadExecutor.execute(() -> transientNotificationsPopup.hide());
      }
    });

    ObjectProperty<ToastPosition> positionProperty = notificationPrefs.toastPositionProperty();
    anchorDetails = notificationPrefs.toastScreenProperty()
                                     .map(this::findScreen)
                                     .map(Screen::getVisualBounds)
                                     .flatMap(bounds -> positionProperty.map(
                                         position -> switch (notificationPrefs.getToastPosition()) {
                                           case ToastPosition.BOTTOM_RIGHT ->
                                               new AnchorDetails(AnchorLocation.CONTENT_BOTTOM_RIGHT,
                                                                 bounds.getMaxX() - 1, bounds.getMaxY() - 1);
                                           case ToastPosition.TOP_RIGHT ->
                                               new AnchorDetails(AnchorLocation.CONTENT_TOP_RIGHT, bounds.getMaxX() - 1,
                                                                 bounds.getMinY());
                                           case ToastPosition.BOTTOM_LEFT ->
                                               new AnchorDetails(AnchorLocation.CONTENT_BOTTOM_LEFT, bounds.getMinX(),
                                                                 bounds.getMaxY() - 1);
                                           case ToastPosition.TOP_LEFT ->
                                               new AnchorDetails(AnchorLocation.CONTENT_TOP_LEFT, bounds.getMinX(),
                                                                 bounds.getMinY());
                                         }));

    anchorDetails.map(AnchorDetails::anchorLocation).subscribe(transientNotificationsPopup::setAnchorLocation);
    transientNotificationsController.getRoot().getChildren().subscribe(this::updateToast);
  }

  public void addNotification(TransientNotification notification) {
    transientNotificationsController.addNotification(notification);
  }

  private Screen findScreen(Number toastScreenIndex) {
    ObservableList<Screen> screens = Screen.getScreens();
    if (toastScreenIndex.intValue() < screens.size()) {
      return screens.get(Math.max(0, toastScreenIndex.intValue()));
    } else {
      return Screen.getPrimary();
    }
  }

  private void updateToast() {
    boolean enabled = notificationPrefs.isTransientNotificationsEnabled();
    if (transientNotificationsController.getRoot().getChildren().isEmpty() || !enabled) {
      transientNotificationsPopup.hide();
      return;
    }

    AnchorDetails anchorDetails = this.anchorDetails.getValue();

    transientNotificationsPopup.show(StageHolder.getStage(), anchorDetails.anchorX(), anchorDetails.anchorY());
  }

  private record AnchorDetails(AnchorLocation anchorLocation, double anchorX, double anchorY) {}
}
