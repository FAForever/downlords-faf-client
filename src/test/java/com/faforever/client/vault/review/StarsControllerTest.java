package com.faforever.client.vault.review;

import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StarsControllerTest extends UITest {
  private StarsController instance;

  @Mock
  private StarController starController;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new StarsController();

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
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
