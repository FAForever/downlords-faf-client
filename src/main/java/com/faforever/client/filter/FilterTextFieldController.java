package com.faforever.client.filter;

import javafx.beans.property.Property;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FilterTextFieldController<T> extends AbstractFilterNodeController<String, Property<String>, T> {

  public TextField root;

  @Override
  public boolean hasDefaultValue() {
    return root.getText().isEmpty();
  }

  @Override
  public void resetFilter() {
    root.clear();
  }

  @Override
  public Property<String> getObservable() {
    return root.textProperty();
  }

  @Override
  public TextField getRoot() {
    return root;
  }

  public void setPromptText(String promptText) {
    root.setPromptText(promptText);
  }
}
