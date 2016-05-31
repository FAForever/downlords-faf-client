package com.faforever.client.main;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.application.Platform;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    getScene().setRoot(instance.userMenuRoot);
    assertThat(getStage().isShowing(), is(true));

    CountDownLatch latch = new CountDownLatch(1);
    getStage().showingProperty().addListener((observable, oldValue, newValue) -> {
      latch.countDown();
    });

    Platform.runLater(() -> instance.onLogOutButtonClicked());

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertThat(getStage().isShowing(), is(false));
  }
}