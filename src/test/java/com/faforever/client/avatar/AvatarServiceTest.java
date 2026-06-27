package com.faforever.client.avatar;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.mapstruct.AvatarMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.AvatarAssignment;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AvatarServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;
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
    Avatar avatar = Instancio.of(Avatar.class).set(field(Avatar::url),
                                          getClass().getResource("/theme/images/default_achievement.png")
                                                    .toURI()
                                                    .toURL()).create();
    instance.loadAvatar(avatar);
    verify(assetService).loadAndCacheImage(avatar.url(), Path.of("avatars"));
  }

  @Test
  public void testAvatarIsNull() {
    assertNull(instance.loadAvatar(null));
    verifyNoInteractions(assetService);
  }

  @Test
  public void getAvailableAvatars() {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());

    com.faforever.commons.api.dto.Avatar avatarDto = new com.faforever.commons.api.dto.Avatar();
    avatarDto.setId("5");
    avatarDto.setUrl("https://example.com/avatar.png");
    avatarDto.setTooltip("tooltip");
    AvatarAssignment assignment = new AvatarAssignment();
    assignment.setId("1");
    assignment.setAvatar(avatarDto);
    when(fafApiAccessor.getAll(any())).thenReturn(Flux.just(assignment));

    List<Avatar> result = instance.getAvailableAvatars().join();

    assertThat(result, hasSize(1));
    verify(fafApiAccessor).getAll(argThat(ElideMatchers.hasFilter(qBuilder().string("player.id").eq("1"))));
  }

  @Test
  public void changeAvatar() throws Exception {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    when(fafApiAccessor.patchToOneRelationship(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

    URL url = URI.create("https://example.com").toURL();
    instance.changeAvatar(Instancio.of(Avatar.class).set(field(Avatar::id), 42).set(field(Avatar::url), url).create());

    verify(fafApiAccessor).patchToOneRelationship("player", "1", "currentAvatar", "avatar", "42");
  }

  @Test
  public void changeAvatarToNoAvatarClearsSelection() {
    when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    when(fafApiAccessor.patchToOneRelationship(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

    instance.changeAvatar(new Avatar(null, null, "no avatar"));

    verify(fafApiAccessor).patchToOneRelationship(eq("player"), eq("1"), eq("currentAvatar"), eq("avatar"), eq(null));
  }
}
