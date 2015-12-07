package com.faforever.client;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import org.testfx.util.WaitForAsyncUtils;

public class MainTest extends AbstractPlainJavaFxTest {

  private Main instance;

  @Before
  public void setUp() throws Exception {
    instance = new Main();
  }

  @Test
  public void testStart() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> instance.start(new Stage()));
    instance.context.close();
  }

  @Test
  public void testStop() throws Exception {
    instance.stop();
  }
}
