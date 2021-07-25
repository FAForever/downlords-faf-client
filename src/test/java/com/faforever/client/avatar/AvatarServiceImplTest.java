package com.faforever.client.avatar;

import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.nio.file.Paths;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AvatarServiceImplTest extends ServiceTest {

  @Mock
  private FafService fafService;
  @Mock
  private AssetService assetService;

  private AvatarServiceImpl instance;
  @BeforeEach
  public void setUp() throws Exception {
    instance = new AvatarServiceImpl(fafService, assetService);
  }

  @Test
  public void testLoadAvatar() throws Exception {
    URL url = getClass().getResource("/theme/images/default_achievement.png").toURI().toURL();
    instance.loadAvatar(url.toString());
    verify(assetService).loadAndCacheImage(url, Paths.get("avatars"), null);
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
