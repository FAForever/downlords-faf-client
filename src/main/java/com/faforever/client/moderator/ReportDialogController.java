package com.faforever.client.moderator;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.report.ReportService;
import com.faforever.client.reporting.ReportingService;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReportDialogController implements Controller<Node> {

  private final I18n i18n;
  private final ReportService reportService;
  public Pane reportDialogRoot;
  public Button dismissButton;
  public Button reportButton;
  public Label userLabel;

  public Label successLabel;
  public Label errorLabel;
  public TextField reportDescription;
  public TextField game;
  public TextField timeCode;
  private JFXDialogLayout dialogLayout;
  private ObjectProperty<Player> player;
  private final NotificationService notificationService;
  private final ReportingService reportingService;

  private Runnable closeListener;

  public ReportDialogController(I18n i18n, ReportService reportService, NotificationService notificationService, ReportingService reportingService) {
    this.i18n = i18n;
    this.reportService = reportService;
    this.notificationService = notificationService;
    this.reportingService = reportingService;

    dialogLayout = new JFXDialogLayout();
    player = new SimpleObjectProperty<>();
  }

  public void initialize() {
    dialogLayout.setBody(reportDialogRoot);

    userLabel.textProperty().bind(Bindings.createStringBinding(() -> i18n.get("report.user.text", player.get().getUsername()), player));
  }

  public Region getRoot() {
    return reportDialogRoot;
  }

  public ReportDialogController setPlayer(Player player) {
    this.player.set(player);
    return this;
  }

  public void onReportButtonClicked() {
    if (!isValid()) {
      return;
    }
    reportButton.setDisable(true);
    try {
      reportService.report(player.get(), game.getText(), reportDescription.getText(), timeCode.getText());
      successLabel.setVisible(true);
    } catch (Exception e) {
      notificationService.addNotification(
          new ImmediateErrorNotification(i18n.get("errorTitle"), i18n.get("report.createProblem"), e, i18n, reportingService)
      );
      reportButton.setDisable(false);
    }

  }

  private boolean isValid() {
    List<String> errors = new ArrayList<>();
    if (StringUtils.isBlank(reportDescription.getText())) {
      errors.add(i18n.get("report.noDescription"));
    }
    if (!StringUtils.isNumeric(game.getText())) {
      errors.add(i18n.get("report.wrongGameId"));
    }
    if (!errors.isEmpty()) {
      errorLabel.setText(String.join(", ", errors));
      errorLabel.setVisible(true);
      return false;
    } else {
      errorLabel.setVisible(false);
      return true;
    }
  }

  public ReportDialogController setCloseListener(Runnable closeListener) {
    this.closeListener = closeListener;
    return this;
  }

  public void dismiss() {
    closeListener.run();
  }

  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }

  public JFXDialogLayout getDialogLayout() {
    return dialogLayout;
  }
}
