package com.faforever.client.vault.search;

import com.faforever.client.test.UITest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class SavedQueriesControllerTest extends UITest {

  private final ObservableMap<String, String> savedQueries = FXCollections.observableHashMap();
  private SavedQueriesController instance;
  @Mock
  private SearchController searchController;
  private TextField queryTextField;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new SavedQueriesController();
    savedQueries.put("name", "test");
    instance.setOnCloseButtonClickedListener(() -> {
    });

    loadFxml("theme/vault/search/saved_queries.fxml", clazz -> instance);
    queryTextField = new TextField();
    instance.setQueries(savedQueries);
    instance.setQueryTextField(queryTextField);
    instance.setSearchController(searchController);
  }

  @Test
  public void testOnSearchButtonClicked() {
    instance.queryListView.getSelectionModel().select(0);
    instance.onSearchButtonClicked();

    assertEquals("test", queryTextField.getText());
    verify(searchController).onSearchButtonClicked();
  }

  @Test
  public void testOnRemoveButtonClicked() {
    instance.queryListView.getSelectionModel().select(0);
    instance.onRemoveQueryButtonClicked();

    assertFalse(savedQueries.containsKey("name"));
    assertTrue(instance.queryListView.getSelectionModel().isEmpty());
    assertFalse(instance.queryListView.getItems().contains("name"));
  }
}
