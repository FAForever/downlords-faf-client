package com.faforever.client.vault.review;

import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class StarsControllerTest extends PlatformTest {
  @InjectMocks
  private StarsController instance;

  @Mock
  private UiService uiService;
  @Mock
  private StarController starController;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/vault/review/star.fxml")).thenReturn(starController);
    when(starController.fillProperty()).thenReturn(new SimpleFloatProperty());
    when(starController.getRoot()).thenAnswer(invocation -> new Pane());

    loadFxml("theme/vault/review/stars.fxml", param -> {
      if (param == StarController.class) {
        return starController;
      }
      return instance;
    });
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.starsRoot));
  }
}
