package com.faforever.client.query;

import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import lombok.Data;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Data
public class ToggleFilterController implements FilterNodeController {

  public GridPane toggleFilter;
  public Label title;
  public CheckBox checkBox;
  private String propertyName;
  private String value;

  public Optional<List<Condition>> getCondition() {
    QBuilder qBuilder = new QBuilder<>();
    StringProperty property = qBuilder.string(propertyName);
    if (checkBox.isSelected()) {
      return Optional.of(Collections.singletonList(property.in(value)));
    } else {
      return Optional.empty();
    }
  }

  public void addQueryListener(InvalidationListener queryListener) {
    checkBox.selectedProperty().addListener(queryListener);
  }

  public void clear() {
    checkBox.setSelected(false);
  }

  public void setTitle(String title) {
    this.title.setText(title + ":");
  }

  @Override
  public Node getRoot() {
    return toggleFilter;
  }

}
