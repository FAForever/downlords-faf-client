package com.faforever.client.vault;

import com.faforever.client.main.event.OpenLiveReplayViewEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.replay.LiveReplayController;
import com.faforever.client.replay.LocalReplayVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ReplayControllerTest extends PlatformTest {
  @Mock
  private EventBus eventBus;
  @Mock
  private UiService uiService;

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

    loadFxml("theme/vault/replay.fxml", clazz -> instance);
  }

  @Test
  public void testOnLiveReplayTabClicked() {
    when(liveReplayController.getRoot()).thenReturn(new Label());
    runOnFxThreadAndWait(() -> instance.getRoot().getSelectionModel().select(instance.liveReplayVaultTab));
    verify(eventBus).post(any(OpenLiveReplayViewEvent.class));
  }

  @Test
  public void testOnlineReplayTabIsFirstTab() {
    assertEquals(instance.onlineReplayVaultTab, instance.getRoot().getSelectionModel().getSelectedItem());
    assertEquals(0, instance.getRoot().getSelectionModel().getSelectedIndex());
  }

  @Test
  public void testOnLocalReplayTabClicked() {
    when(localReplayVaultController.getRoot()).thenReturn(new Label());
    runOnFxThreadAndWait(() -> instance.getRoot().getSelectionModel().select(instance.localReplayVaultTab));
    verify(eventBus).post(any(OpenLocalReplayVaultEvent.class));
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}