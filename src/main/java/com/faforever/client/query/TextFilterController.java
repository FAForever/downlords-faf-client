package com.faforever.client.query;

import com.faforever.client.fx.JavaFxUtil;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import com.google.common.base.Strings;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Data;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Data
public class TextFilterController implements FilterNodeController {

  public VBox textBox;
  public TextField textField;
  private String propertyName;

  public void initialize() {
    textField = TextFields.createClearableTextField();
    JavaFxUtil.bindManagedToVisible(textField);
    textBox.getChildren().add(textField);
  }

  public Optional<List<Condition>> getCondition() {
    QBuilder qBuilder = new QBuilder<>();
    StringProperty property = qBuilder.string(propertyName);
    String value = textField.getText();
    if (!Strings.isNullOrEmpty(value)) {
      if (!textField.getStyleClass().contains("query-filter-selected")) {
        textField.getStyleClass().add("query-filter-selected");
      }
      return Optional.of(Collections.singletonList(property.eq("*" + value + "*")));
    } else {
      textField.getStyleClass().removeIf(styleClass -> styleClass.equals("query-filter-selected"));
      return Optional.empty();
    }
  }

  public void addQueryListener(InvalidationListener queryListener) {
    textField.textProperty().addListener(queryListener);
  }

  public void clear() {
    textField.setText("");
  }

  public void setTitle(String title) {
    textField.setPromptText(title);
  }

  public void setOnAction(Runnable runnable) {
    textField.setOnAction(event -> runnable.run());
  }

  @Override
  public Node getRoot() {
    return textBox;
  }

}
