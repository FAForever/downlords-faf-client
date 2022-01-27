package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;

public class CopyLabelMenuItemTest extends UITest {

  @Mock
  private I18n i18n;

  private CopyLabelMenuItem instance;

  @BeforeEach
  public void setUp() {
    instance = new CopyLabelMenuItem(i18n);
  }

  @Test
  public void testCopyLabel() {
    final boolean[] isEqual = new boolean[1];
    runOnFxThreadAndWait(() -> {
      instance.setObject(new Label("value"));
      instance.onClicked();
      isEqual[0] = Clipboard.getSystemClipboard().getString().equals("value");
    });
    assertTrue(isEqual[0]);
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(new Label("value"));
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfValueIsEmpty() {
    instance.setObject(new Label(""));
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoValue() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}