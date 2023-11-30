package com.faforever.client.vault;

import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.replay.LiveReplayController;
import com.faforever.client.replay.LocalReplayVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.ReplayController.ReplayContentEnum;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ReplayControllerTest extends PlatformTest {
  @Mock
  private UiService uiService;
  @Mock
  private NavigationHandler navigationHandler;

  @Mock
  public OnlineReplayVaultController onlineReplayVaultController;
  @Mock
  public LocalReplayVaultController localReplayVaultController;
  @Mock
  public LiveReplayController liveReplayController;

  @InjectMocks
  private ReplayController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/vault/vault_entity.fxml", OnlineReplayVaultController.class)).thenReturn(onlineReplayVaultController);
    when(uiService.loadFxml("theme/vault/vault_entity.fxml", LocalReplayVaultController.class)).thenReturn(localReplayVaultController);
    when(uiService.loadFxml("theme/vault/replay/live_replays.fxml")).thenReturn(liveReplayController);
    when(onlineReplayVaultController.getRoot()).thenReturn(new Pane());
    when(localReplayVaultController.getRoot()).thenReturn(new Pane());
    when(liveReplayController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/vault/replay.fxml", clazz -> instance);
  }

  @Test
  public void testOnLiveReplayTabClicked() {
    when(liveReplayController.getRoot()).thenReturn(new Label());
    runOnFxThreadAndWait(() -> instance.localButton.getOnAction().handle(new ActionEvent(instance.liveButton, null)));
    verify(navigationHandler).setLastReplayTab(ReplayContentEnum.LIVE);
  }

  @Test
  public void testOnlineReplayTabIsFirstTab() {
    assertTrue(instance.onlineButton.isSelected());
  }

  @Test
  public void testOnLocalReplayTabClicked() {
    when(localReplayVaultController.getRoot()).thenReturn(new Label());
    runOnFxThreadAndWait(() -> instance.localButton.getOnAction().handle(new ActionEvent(instance.localButton, null)));
    verify(navigationHandler).setLastReplayTab(ReplayContentEnum.LOCAL);
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}