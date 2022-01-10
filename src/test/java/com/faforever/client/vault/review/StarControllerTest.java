package com.faforever.client.vault.review;

import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StarControllerTest extends UITest {
  @InjectMocks
  private StarController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/vault/review/star.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.starRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
