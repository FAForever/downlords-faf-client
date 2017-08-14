package com.faforever.client.vault.review;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class StarControllerTest extends AbstractPlainJavaFxTest {
  private StarController instance;

  @Before
  public void setUp() throws Exception {
    instance = new StarController();

    loadFxml("theme/vault/review/star.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.starRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
