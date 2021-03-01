package com.faforever.client.vault.search;

import com.faforever.client.fx.Controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Data
public class SavedQueriesController implements Controller<Parent> {

  public ListView<String> queryListView;
  public VBox root;
  public TextField queryTextField;
  public SearchController searchController;

  private ObservableMap<String, String> queries;
  private Runnable onCloseButtonClickedListener;

  public void onSearchButtonClicked() {
    queryTextField.setText(queries.get(queryListView.getSelectionModel().getSelectedItems().get(0)));
    searchController.onSearchButtonClicked();
    onCloseButtonClickedListener.run();
  }

  public void onRemoveQueryButtonClicked() {
    queries.remove(queryListView.getSelectionModel().getSelectedItems().get(0));
    queryListView.getItems().remove(queryListView.getSelectionModel().getSelectedItems().get(0));
    queryListView.getSelectionModel().clearSelection();
  }

  public void setQueries(ObservableMap<String, String> queries) {
    this.queries = queries;
    queryListView.setItems(FXCollections.observableArrayList(queries.keySet()));
  }

  public void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    queryListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
  }
}
