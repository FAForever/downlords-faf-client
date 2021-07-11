package com.faforever.client.vault.search;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SaveQueryControllerTest extends AbstractPlainJavaFxTest {

  private final ObservableMap<String, String> savedQueries = FXCollections.observableHashMap();
  private SaveQueryController instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new SaveQueryController();
    instance.setQueries(savedQueries);
    instance.setQuery("test");
    instance.setOnCloseButtonClickedListener(() -> {
    });

    loadFxml("theme/vault/search/save_query.fxml", clazz -> instance);
  }

  @Test
  public void testOnSaveButtonClicked() {
    instance.queryName.setText("name");
    instance.onSaveButtonClicked();
    assertTrue(savedQueries.get("name").equals("test"));
  }
}
