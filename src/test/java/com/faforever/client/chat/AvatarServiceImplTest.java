package com.faforever.client.chat;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;

public class AvatarServiceImplTest extends AbstractPlainJavaFxTest {

  private AvatarServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new AvatarServiceImpl();
  }

  @Test
  public void testLoadAvatar() throws Exception {
    instance.loadAvatar(getClass().getResource("/images/tray_icon.png").toURI().toURL().toString());
  }
}
