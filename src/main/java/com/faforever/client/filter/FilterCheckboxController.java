package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.BooleanProperty;
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

  public void bindBidirectional(BooleanProperty property) {
    // property.isBound() only changes when property.bind(...) method is called
    bound = true;
    JavaFxUtil.bindBidirectional(root.selectedProperty(), property);
  }

  @Override
  public BooleanProperty getObservable() {
    return root.selectedProperty();
  }

  @Override
  protected Boolean getValue() {
    return root.selectedProperty().getValue();
  }

  @Override
  public CheckBox getRoot() {
    return root;
  }
}
