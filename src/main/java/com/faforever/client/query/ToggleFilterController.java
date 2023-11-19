package com.faforever.client.query;

import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.properties.concrete.StringProperty;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Getter
@Setter
public class ToggleFilterController extends FilterNodeController {

  public GridPane toggleFilter;
  public Label title;
  public CheckBox checkBox;
  private String propertyName;
  private String value;

  @Override
  public Optional<List<Condition>> getCondition() {
    QBuilder qBuilder = new QBuilder<>();
    StringProperty property = qBuilder.string(propertyName);
    if (checkBox.isSelected()) {
      return Optional.of(Collections.singletonList(property.eq(value)));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void addQueryListener(InvalidationListener queryListener) {
    checkBox.selectedProperty().addListener(queryListener);
  }

  @Override
  public void clear() {
    checkBox.setSelected(false);
  }

  @Override
  public void setTitle(String title) {
    this.title.setText(title + ":");
  }

  @Override
  public Node getRoot() {
    return toggleFilter;
  }

}
