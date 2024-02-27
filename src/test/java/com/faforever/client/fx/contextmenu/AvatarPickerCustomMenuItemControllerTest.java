package com.faforever.client.fx.contextmenu;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.PlatformTest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AvatarPickerCustomMenuItemControllerTest extends PlatformTest {

  @Mock
  private AvatarService avatarService;
  @Mock
  private I18n i18n;
  @InjectMocks
  private AvatarPickerCustomMenuItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/chat/avatar_picker_menu_item.fxml", clazz -> instance);
  }

  @Test
  public void testVisibleItemIfThereAvailableAvatars() throws Exception {
    List<AvatarBean> avatars = Instancio.createList(AvatarBean.class);
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(avatars));

    runOnFxThreadAndWait(() -> instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get()));

    assertTrue(instance.getRoot().isVisible());
    assertTrue(instance.avatarComboBox.isVisible());
    assertEquals(avatars.size() + 1, instance.avatarComboBox.getItems().size()); // 3 avatars and 1 no avatar
  }

  @Test
  public void testInvisibleItemIfPlayerIsNotSelf() {
    runOnFxThreadAndWait(() -> instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get()));

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testInvisibleItemIfNoAvailableAvatars() {
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    runOnFxThreadAndWait(() -> instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get()));

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testSelectAvatar() throws Exception {
    AvatarBean avatar = Instancio.create(AvatarBean.class);
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(avatar)));

    runOnFxThreadAndWait(() -> instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get()));
    runOnFxThreadAndWait(() -> instance.avatarComboBox.getSelectionModel().select(1)); // 0 index - no avatar

    verify(avatarService).changeAvatar(avatar);
  }

  @Test
  public void testSelectNoAvatar() throws Exception {
    AvatarBean avatar = Instancio.create(AvatarBean.class);
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(avatar)));
    when(i18n.get("chat.userContext.noAvatar")).thenReturn("no avatar");

    runOnFxThreadAndWait(() -> instance.setObject(PlayerBeanBuilder.create().defaultValues().avatar(avatar).socialStatus(SocialStatus.SELF).get()));
    assertEquals(avatar, instance.avatarComboBox.getSelectionModel().getSelectedItem());

    runOnFxThreadAndWait(() -> instance.avatarComboBox.getSelectionModel().select(0)); // 0 index - no avatar
    assertEquals("no avatar", instance.avatarComboBox.getSelectionModel().getSelectedItem().description());
  }
}