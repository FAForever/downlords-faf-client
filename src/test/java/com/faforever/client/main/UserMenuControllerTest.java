package com.faforever.client.main;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.application.Platform;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class UserMenuControllerTest extends AbstractPlainJavaFxTest {

  private UserMenuController instance;

  @Mock
  private UserService userService;

  @Before
  public void setUp() throws Exception {
    instance = loadController("user_menu.fxml");
    instance.userService = userService;
  }

  @Test
  public void testOnLogoutButtonClickedClosesPopup() throws Exception {
    CountDownLatch setRootLatch = new CountDownLatch(1);
    Platform.runLater(() -> {
      getScene().setRoot(instance.userMenuRoot);
      setRootLatch.countDown();
    });
    setRootLatch.await(5, SECONDS);

    assertThat(getStage().isShowing(), is(true));

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      instance.onLogOutButtonClicked();
      latch.countDown();
    });

    assertTrue(latch.await(5, SECONDS));
    assertThat(getStage().isShowing(), is(false));
  }
}
