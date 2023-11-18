package com.faforever.client.avatar;

import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.mapstruct.AvatarMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AvatarServiceTest extends ServiceTest {

  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private AssetService assetService;
  @Mock
  private PlayerService playerService;
  @Spy
  private AvatarMapper avatarMapper = Mappers.getMapper(AvatarMapper.class);

  @InjectMocks
  private AvatarService instance;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(avatarMapper);
  }

  @Test
  public void testLoadAvatar() throws Exception {
    AvatarBean avatarBean = AvatarBeanBuilder.create().url(getClass().getResource("/theme/images/default_achievement.png").toURI().toURL()).get();
    instance.loadAvatar(avatarBean);
    verify(assetService).loadAndCacheImage(avatarBean.getUrl(), Path.of("avatars"));
  }

  @Test
  public void testAvatarIsNull() {
    assertNull(instance.loadAvatar(null));
    verifyNoInteractions(assetService);
  }

  @Test
  public void getAvailableAvatars() {
    when(fafServerAccessor.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(List.of()));
    instance.getAvailableAvatars();
    verify(fafServerAccessor).getAvailableAvatars();
  }

  @Test
  public void changeAvatar() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().get());

    URL url = new URL("https://example.com");
    instance.changeAvatar(AvatarBeanBuilder.create().url(url).description("Description").get());

    verify(fafServerAccessor).selectAvatar(url);
  }
}
