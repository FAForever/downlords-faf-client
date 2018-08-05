package com.faforever.client.reporting;

import com.bugsnag.Severity;
import com.faforever.client.fx.Controller;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.NumberValidator;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ReportingDialogController implements Controller<Node> {
  private final ReportingService reportingService;
  public JFXTextField logLinesTextField;
  public VBox root;
  public JFXCheckBox contactMeCheckBox;
  public TextArea feedbackText;
  public JFXComboBox<Severity> severityChoiceBox;
  private Runnable closeListener;
  private Throwable throwable;

  @Inject
  public ReportingDialogController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  @Override
  public Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    logLinesTextField.setValidators(new NumberValidator());
    severityChoiceBox.setConverter(new StringConverter<Severity>() {
      @Override
      public String toString(Severity object) {
        return object.name();
      }

      @Override
      public Severity fromString(String string) {
        return Severity.valueOf(string);
      }
    });
    severityChoiceBox.setItems(FXCollections.observableArrayList(Severity.values()));
    severityChoiceBox.getSelectionModel().select(Severity.ERROR);
  }

  public void cancel() {
    closeListener.run();
  }

  public void submit() {
    reportingService.userFilledOutReportForm(
        Integer.parseInt(logLinesTextField.getText()),
        contactMeCheckBox.isSelected(),
        feedbackText.getText(),
        severityChoiceBox.getSelectionModel().getSelectedItem(),
        throwable);
    closeListener.run();
  }

  public ReportingDialogController setCloseListener(Runnable close) {
    closeListener = close;
    return this;
  }

  public ReportingDialogController setDefaultLogLines(int defaultLogLines) {
    logLinesTextField.setText(String.valueOf(defaultLogLines));
    return this;
  }

  public ReportingDialogController setThrowable(Throwable throwable) {
    this.throwable = throwable;
    return this;
  }
}
