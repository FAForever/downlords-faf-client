package com.faforever.client.chat;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Test;

public class AvatarServiceImplTest extends AbstractPlainJavaFxTest {

  private final AvatarServiceImpl avatarService;

  public AvatarServiceImplTest() {
    avatarService = new AvatarServiceImpl();
  }

  @Test
  public void testLoadAvatar() throws Exception {
    avatarService.loadAvatar(getClass().getResource("/images/tray_icon.png").toURI().toURL().toString());
  }
}
