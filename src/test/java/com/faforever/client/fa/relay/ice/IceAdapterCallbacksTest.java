package com.faforever.client.fa.relay.ice;

import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.MessageTarget;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.verify;

public class IceAdapterCallbacksTest extends ServiceTest {

  @Mock
  private IceAdapterImpl javaAdapter;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @InjectMocks
  private IceAdapterCallbacks instance;

  @Test
  public void theAdapterProcessesMessagesGoToTheJavaAdapter() {
    instance.onGpgNetMessageReceived("GameResult", List.of(1, "victory 10"));
    instance.onConnectionStateChanged("Disconnected");

    verify(javaAdapter).onGpgGameMessage(new GpgGameOutboundMessage("GameResult", List.of(1, "victory 10"),
                                                                    MessageTarget.GAME));
    verify(javaAdapter).onIceAdapterStateChanged("Disconnected");
  }
}
