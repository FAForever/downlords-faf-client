package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.test.PlatformTest;
import com.faforever.commons.api.dto.GroupPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class BroadcastMessageMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private ModeratorService moderatorService;

  private BroadcastMessageMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new BroadcastMessageMenuItem(i18n, moderatorService);
  }

  @Test
  public void testVisibleItem() {
    when(moderatorService.getPermissions()).thenReturn(Set.of(GroupPermission.ROLE_WRITE_MESSAGE));

    runOnFxThreadAndWait(() -> instance.setObject(any()));

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItem() {
    when(moderatorService.getPermissions()).thenReturn(Collections.emptySet());

    runOnFxThreadAndWait(() -> instance.setObject(null));

    assertFalse(instance.isVisible());
  }
}