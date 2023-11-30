package com.faforever.client.vault.search;

import com.faforever.client.test.PlatformTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SaveQueryControllerTest extends PlatformTest {

  private final ObservableMap<String, String> savedQueries = FXCollections.observableHashMap();
  @InjectMocks
  private SaveQueryController instance;

  @BeforeEach
  public void setUp() throws Exception {
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
    assertEquals("test", savedQueries.get("name"));
  }
}
