package com.faforever.client.headerbar;

import com.faforever.client.api.TokenRetriever;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserButtonControllerTest extends PlatformTest {
  private static final String TEST_USER_NAME = "junit";

  @Mock
  private UiService uiService;
  @Mock
  private LoginService loginService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReportDialogController reportDialogController;
  @Mock
  private PlayerInfoWindowController playerInfoWindowController;
  @Mock
  private TokenRetriever tokenRetriever;


  @InjectMocks
  private UserButtonController instance;
  private PlayerInfo player;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    lenient().when(uiService.loadFxml("theme/user_info_window.fxml")).thenReturn(playerInfoWindowController);

    player = PlayerInfoBuilder.create().defaultValues().username(TEST_USER_NAME).get();
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));

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
  public void testCopyAccessToken() {
    when(tokenRetriever.getAccessToken()).thenReturn(Mono.just("someToken"));

    instance.onCopyAccessToken();

    verify(tokenRetriever).getAccessToken();
  }

  @Test
  public void testLogOut() {
    instance.onLogOut();

    verify(loginService).logOut();
  }

}

