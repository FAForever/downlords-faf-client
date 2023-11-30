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
import lombok.Getter;
import lombok.Setter;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Setter
@Getter
public class TextFilterController extends FilterNodeController {

  public VBox textBox;
  public TextField textField;
  private String propertyName;
  private boolean exact;

  @Override
  protected void onInitialize() {
    textField = TextFields.createClearableTextField();
    JavaFxUtil.bindManagedToVisible(textField);
    textBox.getChildren().add(textField);
  }

  @Override
  public Optional<List<Condition>> getCondition() {
    QBuilder qBuilder = new QBuilder<>();
    StringProperty property = qBuilder.string(propertyName);
    String value = textField.getText();
    if (!Strings.isNullOrEmpty(value)) {
      if (!textField.getStyleClass().contains("query-filter-selected")) {
        textField.getStyleClass().add("query-filter-selected");
      }
      if (!exact) {
        value = "*" + value + "*";
      }
      return Optional.of(Collections.singletonList(property.eq(value)));
    } else {
      textField.getStyleClass().removeIf(styleClass -> styleClass.equals("query-filter-selected"));
      return Optional.empty();
    }
  }

  @Override
  public void addQueryListener(InvalidationListener queryListener) {
    textField.textProperty().addListener(queryListener);
  }

  @Override
  public void clear() {
    textField.setText("");
  }

  @Override
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
