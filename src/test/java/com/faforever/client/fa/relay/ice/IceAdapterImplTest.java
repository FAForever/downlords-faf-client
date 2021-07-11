package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
public class IceAdapterImplTest {

  @Mock
  private IceAdapterImpl instance;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;
  @Mock
  private FafService fafService;
  @Mock
  private PreferencesService preferencesService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new IceAdapterImpl(applicationContext, clientProperties, playerService, eventBus, fafService, preferencesService);
  }

  @Test
  public void onIceAdapterStateChanged() throws Exception {
  }

  @Test
  public void onGpgGameMessage() throws Exception {
  }

  @Test
  public void start() throws Exception {
  }

  @Test
  public void stop() throws Exception {
  }

}
