package com.faforever.client.headerbar;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserButtonControllerTest extends UITest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private UiService uiService;
  @Mock
  private LoginService loginService;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ReportDialogController reportDialogController;
  @Mock
  private PlayerInfoWindowController playerInfoWindowController;


  @InjectMocks
  private UserButtonController instance;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    when(uiService.loadFxml("theme/user_info_window.fxml")).thenReturn(playerInfoWindowController);
    when(uiService.createShowingProperty(any())).thenReturn(new SimpleBooleanProperty(true));

    player = PlayerBeanBuilder.create().defaultValues().username(TEST_USER_NAME).get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));

    loadFxml("theme/headerbar/user_button.fxml", clazz -> instance);
  }

  @Test
  public void testShowProfile() {
    instance.onShowProfile();

    verify(playerInfoWindowController).setPlayer(player);
    verify(playerInfoWindowController).show();
  }

  @Test
  public void testReport() {
    instance.onReport();

    verify(reportDialogController).setAutoCompleteWithOnlinePlayers();
    verify(reportDialogController).show();
  }

  @Test
  public void testLogOut() {
    instance.onLogOut();

    verify(loginService).logOut();
  }

}

