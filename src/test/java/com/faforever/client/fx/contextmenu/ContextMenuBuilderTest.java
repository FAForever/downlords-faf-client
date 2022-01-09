package com.faforever.client.fx.contextmenu;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContextMenuBuilderTest extends UITest {

  @Mock
  private ApplicationContext applicationContext;

  ContextMenu instance;
  AbstractMenuItem<Object> simpleItem1;
  AbstractCustomMenuItemController<Object> customItem2;
  AbstractMenuItem<Object> simpleItem3;
  AbstractCustomMenuItemController<Object> customItem4;
  List<MenuItem> items;

  @BeforeEach
  void setUp() {
    simpleItem1 = stubMenuItem();
    customItem2 = stubCustomMenuItem();
    simpleItem3 = stubMenuItem();
    customItem4 = stubCustomMenuItem();

    instance = ContextMenuBuilder.newBuilder(applicationContext)
        .addItem(simpleItem1)           // [0]
        .addSeparator()                 // [1]
        .addCustomItem(customItem2)     // [2]
        .addSeparator()                 // [3]
        .addItem(simpleItem3)           // [4]
        .addCustomItem(customItem4)     // [5]
        .build();
    items = instance.getItems();
  }

  @Test
  public void testCheckFirstSeparatorVisible() {
    assertTrue(items.get(1).isVisible());
    assertTrue(items.get(2).isVisible());

    runOnFxThreadAndWait(() -> items.get(2).setVisible(false));
    assertFalse(items.get(1).isVisible());

    runOnFxThreadAndWait(() -> items.get(2).setVisible(true));
    assertTrue(items.get(1).isVisible());
  }

  @Test
  public void testCheckSecondSeparatorVisible() {
    assertTrue(items.get(3).isVisible());
    assertTrue(items.get(4).isVisible());
    assertTrue(items.get(5).isVisible());

    runOnFxThreadAndWait(() -> items.get(4).setVisible(false));
    assertTrue(items.get(3).isVisible());
    runOnFxThreadAndWait(() -> items.get(5).setVisible(false));
    assertFalse(items.get(3).isVisible());

    runOnFxThreadAndWait(() -> {
      items.get(4).setVisible(true);
      items.get(5).setVisible(true);
    });
    assertTrue(items.get(3).isVisible());
  }

  private AbstractMenuItem<Object> stubMenuItem() {
    return new AbstractMenuItem<>() {
      @Override
      protected void onClicked(Object object) {

      }

      @Override
      protected String getItemText(I18n i18n) {
        return null;
      }
    };
  }

  private AbstractCustomMenuItemController<Object> stubCustomMenuItem() {
    return new AbstractCustomMenuItemController<>() {

      private CustomMenuItem root = new CustomMenuItem();

      @Override
      public void afterSetObject(Object object) {

      }

      @Override
      public CustomMenuItem getRoot() {
        return root;
      }
    };
  }
}