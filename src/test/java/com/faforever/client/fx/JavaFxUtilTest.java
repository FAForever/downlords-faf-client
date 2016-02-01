package com.faforever.client.fx;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.control.TextField;
import org.junit.Test;

public class JavaFxUtilTest extends AbstractPlainJavaFxTest {

  @Test
  public void testMakeAutoCompletable() throws Exception {
    TextField textField = new TextField();
    textField.setText("A");
  }
}
