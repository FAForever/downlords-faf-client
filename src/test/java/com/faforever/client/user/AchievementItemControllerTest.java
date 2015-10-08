package com.faforever.client.user;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class AchievementItemControllerTest extends AbstractPlainJavaFxTest {

  private AchievementItemController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("achievement_item.fxml");
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.achievementItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
