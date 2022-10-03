package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.scene.control.CheckBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FilterCheckboxController<T> extends AbstractFilterNodeController<Boolean, BooleanProperty, T> {

  public CheckBox root;

  private boolean bound;

  public void setText(String text) {
    root.setText(text);
  }

  @Override
  public boolean hasDefaultValue() {
    return bound || !root.isSelected();
  }

  @Override
  public void resetFilter() {
    if (!bound) {
      root.setSelected(false);
    }
  }

  @Override
  public BooleanProperty getObservable() {
    return root.selectedProperty();
  }

  @Override
  public void bindBidirectional(Property<?> property) {
    if (property instanceof BooleanProperty booleanProperty) {
      bound = true;
      JavaFxUtil.bindBidirectional(root.selectedProperty(), booleanProperty);
    } else {
      throw new IllegalArgumentException("Property have should instance of class " + BooleanProperty.class.getSimpleName());
    }
  }

  @Override
  public CheckBox getRoot() {
    return root;
  }
}
