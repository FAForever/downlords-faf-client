package com.faforever.client.chat.avatar;

import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;
import java.nio.file.Paths;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvatarServiceImplTest {

  @Rule
  public TemporaryFolder cacheFolder = new TemporaryFolder();

  @Mock
  private FafService fafService;
  @Mock
  private AssetService assetService;

  private AvatarServiceImpl instance;
  @Before
  public void setUp() throws Exception {
    instance = new AvatarServiceImpl();
    instance.fafService = fafService;
    instance.assetService = assetService;
  }

  @Test
  public void testLoadAvatar() throws Exception {
    URL url = getClass().getResource("/theme/images/close.png").toURI().toURL();
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
