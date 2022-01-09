package com.faforever.client.fx.contextmenu;

import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractMenuItemTest extends UITest {

  private AbstractMenuItem<Object> instance;

  @BeforeEach
  public void setUp() {
    instance = new AbstractMenuItem<>() {
      @Override
      protected void onClicked(Object object) {

      }

      @Override
      protected String getItemText() {
        return null;
      }
    };
  }
}