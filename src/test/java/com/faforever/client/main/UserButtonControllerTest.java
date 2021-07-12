package com.faforever.client.main;

import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserButtonControllerTest extends UITest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ReportDialogController reportDialogController;
  @Mock
  private UserInfoWindowController userInfoWindowController;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;

  private UserButtonController instance;
  private Player player;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new UserButtonController(eventBus, playerService, uiService, userService, preferencesService);
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    when(uiService.loadFxml("theme/user_info_window.fxml")).thenReturn(userInfoWindowController);

    player = PlayerBuilder.create(TEST_USER_NAME).defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(userService.getUsername()).thenReturn(TEST_USER_NAME);

    loadFxml("theme/user_button.fxml", clazz -> instance);
  }

  @Test
  public void testOnLoginSuccess() {
    instance.onLoginSuccessEvent(new LoginSuccessEvent());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(TEST_USER_NAME, instance.userMenuButtonRoot.getText());
  }

  @Test
  public void testOnGetRoot() {
    assertEquals(instance.userMenuButtonRoot, instance.getRoot());
  }

  @Test
  public void testShowProfile() {
    instance.onShowProfile(null);

    verify(userInfoWindowController).setPlayer(player);
    verify(userInfoWindowController).show();
  }

  @Test
  public void testReport() {
    instance.onReport(null);

    verify(reportDialogController).setAutoCompleteWithOnlinePlayers();
    verify(reportDialogController).show();
  }

  @Test
  public void testLogOut() {
    instance.onLogOut(null);

    verify(eventBus).post(any(LogOutRequestEvent.class));
  }

}

