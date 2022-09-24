package com.faforever.client.tournament.game;

import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.commons.lobby.IsReadyRequest;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TournamentGameServiceTest extends ServiceTest {
  @InjectMocks
  private TournamentGameService instance;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private GameService gameService;
  @Mock
  private IsReadyForGameController isReadyForGameController;
  @Mock
  private NotificationService notificationService;
  @Captor
  private ArgumentCaptor<Consumer<IsReadyRequest>> eventListenerCapture;
  @Captor
  private ArgumentCaptor<Runnable> isReadyCallbackCapture;

  @Test
  public void testReceivingIsReadyMessageAndClickingReady() throws Exception {
    instance.afterPropertiesSet();
    verify(fafServerAccessor).addEventListener(eq(IsReadyRequest.class), eventListenerCapture.capture());

    doReturn(isReadyForGameController).when(uiService).loadFxml("theme/tournaments/is_ready_for_game.fxml");
    doReturn(new Region()).when(isReadyForGameController).getRoot();

    eventListenerCapture.getValue().accept(new IsReadyRequest("name", "faf", 1, "abc"));

    verify(uiService).bringMainStageToFront();
    verify(notificationService).addNotification(any(ImmediateNotification.class));
    verify(isReadyForGameController).setTimeout(1);
    verify(isReadyForGameController).setReadyCallback(isReadyCallbackCapture.capture());

    isReadyCallbackCapture.getValue().run();

    verify(fafServerAccessor).sendIsReady("abc");
    verify(gameService).startListeningToTournamentGame("faf");
  }
}