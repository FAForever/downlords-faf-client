package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import javafx.scene.input.Clipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CopyUsernameMenuItemTest extends UITest {

  @Mock
  private I18n i18n;

  private CopyUsernameMenuItem instance;

  @BeforeEach
  public void setup() {
    instance = new CopyUsernameMenuItem(i18n);
  }

  @Test
  public void testCopyUsername() {
    final boolean[] isEqual = new boolean[1];
    runOnFxThreadAndWait(() -> {
      instance.setObject("username");
      instance.onClicked();
      isEqual[0] = Clipboard.getSystemClipboard().getString().equals("username");
    });
    assertTrue(isEqual[0]);
  }

  @Test
  public void testVisibleItem() {
    instance.setObject("username");
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUsernameIsEmpty() {
    instance.setObject("");
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfUsernameIsNull() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}