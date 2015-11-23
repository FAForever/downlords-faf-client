package com.faforever.client.util;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.Test;

import java.util.Arrays;

public class JavaFxUtilTest extends AbstractPlainJavaFxTest {

  @Test
  public void testMakeAutoCompletable() throws Exception {
    TextField textField = new TextField();

    JavaFxUtil.makeAutoCompletable(textField, s -> Arrays.asList(
        new CustomMenuItem(new Label("Apple")),
        new CustomMenuItem(new Label("Pie")),
        new CustomMenuItem(new Label("Banana")),
        new CustomMenuItem(new Label("Horse"))), null);

    textField.setText("A");
  }
}