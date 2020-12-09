package com.faforever.client.vault.search;

import com.faforever.client.fx.Controller;
import javafx.collections.ObservableMap;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SaveQueryController implements Controller<Parent> {

  public TextField queryName;
  public VBox root;

  private ObservableMap<String, String> queries;
  private String query;
  private Runnable onCloseButtonClickedListener;

  public void onSaveButtonClicked() {
    queries.put(queryName.getText(), query);
    onCloseButtonClickedListener.run();
  }

  public void onCancelButtonClicked() {
    onCloseButtonClickedListener.run();
  }

  public void setQueries(ObservableMap<String, String> queries) {
    this.queries = queries;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }

  @Override
  public Parent getRoot() {
    return root;
  }
}
