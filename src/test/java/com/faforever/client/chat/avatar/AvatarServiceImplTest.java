package com.faforever.client.chat.avatar;

import com.faforever.client.remote.FafService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class AvatarServiceImplTest extends AbstractPlainJavaFxTest {

  private AvatarServiceImpl instance;

  @Mock
  private FafService fafService;

  @Before
  public void setUp() throws Exception {
    instance = new AvatarServiceImpl();
    instance.fafService = fafService;
  }

  @Test
  public void testLoadAvatar() throws Exception {
    instance.loadAvatar(getClass().getResource("/theme/images/tray_icon.png").toURI().toURL().toString());
  }

  @Test
  public void getAvailableAvatars() throws Exception {
    instance.getAvailableAvatars();
    verify(fafService).getAvailableAvatars();

  }

  @Test
  public void changeAvatar() throws Exception {
    AvatarBean avatar = new AvatarBean(null, "");
    instance.changeAvatar(avatar);
    verify(fafService).selectAvatar(avatar);
  }
}
