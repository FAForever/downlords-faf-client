package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import javafx.scene.input.Clipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;

public class CopyUsernameMenuItemTest extends UITest {

  @Mock
  private I18n i18n;

  private CopyUsernameMenuItem instance;

  @BeforeEach
  public void setUp() {
    instance = new CopyUsernameMenuItem(i18n);
  }

  @Test
  public void testCopyUsername() {
    final boolean[] isEqual = new boolean[1];
    runOnFxThreadAndWait(() -> {
      instance.setObject("value");
      instance.onClicked();
      isEqual[0] = Clipboard.getSystemClipboard().getString().equals("value");
    });
    assertTrue(isEqual[0]);
  }

  @Test
  public void testVisibleItem() {
    instance.setObject("value");
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItem() {
    instance.setObject("");
    assertFalse(instance.isVisible());
  }
}