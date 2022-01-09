package com.faforever.client.fx.contextmenu;

import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;

public class AbstractMenuItemTest extends UITest {

  private AbstractMenuItem<Object> instance;

  @BeforeEach
  public void setUp() {
    instance = new AbstractMenuItem<>() {
      @Override
      protected void onClicked() {

      }

      @Override
      protected String getItemText() {
        return null;
      }
    };
  }
}