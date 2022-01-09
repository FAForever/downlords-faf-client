package com.faforever.client.fx.contextmenu;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ContextMenuBuilderTest extends UITest {

  @Mock
  private ApplicationContext applicationContext;

  ContextMenu instance;
  AbstractMenuItem<Object> simpleItem1;
  AbstractCustomMenuItemController<Object> customItem2;
  AbstractCustomMenuItemController<Object> customItem4;
  AbstractMenuItem<Object> simpleItem4;
  List<MenuItem> items;

  @SuppressWarnings("unchecked")
  @BeforeEach
  public void setUp() {
    simpleItem1 = stubMenuItem();
    customItem2 = stubCustomMenuItem();
    customItem4 = stubCustomMenuItem();
    simpleItem4 = stubMenuItem();

    when(applicationContext.getBean(any(Class.class))).thenReturn(simpleItem1, simpleItem4);

    instance = ContextMenuBuilder.newBuilder(applicationContext)
        .addItem(stubMenuItem().getClass())           // [0]
        .addSeparator()                               // [1]
        .addCustomItem(customItem2)                   // [2]
        .addSeparator()                               // [3]
        .addItem(stubMenuItem().getClass())           // [4]
        .addCustomItem(customItem4)                   // [5]
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
      protected void onClicked() {

      }

      @Override
      protected String getItemText() {
        return null;
      }
    };
  }

  private AbstractCustomMenuItemController<Object> stubCustomMenuItem() {
    return new AbstractCustomMenuItemController<>() {

      private final CustomMenuItem root = new CustomMenuItem();

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