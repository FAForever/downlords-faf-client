package com.faforever.client.filter;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FilterTextFieldController<T> extends AbstractFilterNodeController<String, StringProperty, T> {

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
  public StringProperty getObservable() {
    return root.textProperty();
  }

  @Override
  protected String getValue() {
    return root.textProperty().getValue();
  }

  @Override
  public TextField getRoot() {
    return root;
  }

  public void setPromptText(String promptText) {
    root.setPromptText(promptText);
  }
}
