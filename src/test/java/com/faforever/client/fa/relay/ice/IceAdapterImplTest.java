package com.faforever.client.fa.relay.ice;

import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.game.GameService;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.MessageTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IceAdapterImplTest extends ServiceTest {

  @InjectMocks
  private IceAdapterImpl instance;
  @Spy
  private OperatingSystem operatingSystem = new OsPosix();
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private PlayerService playerService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private GameFullNotifier gameFullNotifier;
  @Mock
  private GameService gameService;
  @Mock
  private ObjectFactory<IceAdapterCallbacks> iceAdapterCallbacksFactory;

  @Mock
  private IceAdapterApi iceAdapterApi;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Spy
  private IceServerMapper iceServerMapper = Mappers.getMapper(IceServerMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(iceServerMapper);
    ReflectionTestUtils.setField(instance, "iceAdapterProxy", iceAdapterApi, null);
  }

  @Test
  public void onIceAdapterStateChanged() throws Exception {
    instance.onIceAdapterStateChanged("Disconnected");
    instance.onIceAdapterStateChanged("Connected");
    verify(iceAdapterApi, times(1)).quit();
  }

  @Test
  public void onGpgGameMessage() throws Exception {
    instance.onGpgGameMessage(new GpgGameOutboundMessage("GameFull", List.of(), MessageTarget.GAME));
    verify(gameFullNotifier).onGameFull();

    GpgGameOutboundMessage message = new GpgGameOutboundMessage("Test", List.of(), MessageTarget.GAME);
    instance.onGpgGameMessage(message);
    verify(fafServerAccessor).sendGpgMessage(message);
  }

  @Test
  public void testSetMatchmaker() throws Exception {
    instance.updateGameTypeFromGameInfo(GameLaunchMessageBuilder.create().defaultValues().gameType(GameType.MATCHMAKER).get());
    instance.setLobbyInitMode();
    verify(iceAdapterApi).setLobbyInitMode("auto");
  }

  @Test
  public void testSetCustom() throws Exception {
    instance.updateGameTypeFromGameInfo(GameLaunchMessageBuilder.create().defaultValues().get());
    instance.setLobbyInitMode();
    verify(iceAdapterApi).setLobbyInitMode("normal");
  }

  @Test
  public void testBuildCommand() throws Exception {
    Path javaExecutablePath = Path.of("some", "path", "java");

    when(operatingSystem.getJavaExecutablePath()).thenReturn(javaExecutablePath);
    PlayerBean currentPlayer = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);
    forgedAlliancePrefs.setShowIceAdapterDebugWindow(true);

    List<String> command = instance.buildCommand(Path.of("."), 0, 0, 4711);

    assertEquals(javaExecutablePath.toAbsolutePath().toString(), command.get(0));
    assertEquals("-Dorg.ice4j.ipv6.DISABLED=true", command.get(1));
    assertEquals("-cp", command.get(2));
    assertTrue(command.get(3).contains("faf-ice-adapter.jar"));
    assertTrue(command.get(3).contains("javafx-"));
    assertEquals("com.faforever.iceadapter.IceAdapter", command.get(4));
    assertEquals("--id", command.get(5));
    assertEquals(String.valueOf(currentPlayer.getId()), command.get(6));
    assertEquals("--game-id", command.get(7));
    assertEquals(String.valueOf(4711), command.get(8));
    assertEquals("--login", command.get(9));
    assertEquals(currentPlayer.getUsername(), command.get(10));
    assertEquals("--rpc-port", command.get(11));
    assertEquals(String.valueOf(0), command.get(12));
    assertEquals("--gpgnet-port", command.get(13));
    assertEquals(String.valueOf(0), command.get(14));
    assertEquals("--debug-window", command.get(15));
    assertEquals("--info-window", command.get(16));
  }

  @Test
  public void testAllowIpv6() throws Exception {
    Path javaExecutablePath = Path.of("some", "path", "java");

    when(operatingSystem.getJavaExecutablePath()).thenReturn(javaExecutablePath);
    forgedAlliancePrefs.setAllowIpv6(true);
    forgedAlliancePrefs.setShowIceAdapterDebugWindow(true);
    PlayerBean currentPlayer = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);

    List<String> command = instance.buildCommand(Path.of("."), 0, 0, 4711);

    assertFalse(command.contains("-Dorg.ice4j.ipv6.DISABLED=true"));
  }

  @Test
  public void testStop() throws Exception {
    instance.stop();
    verify(iceAdapterApi).quit();
  }

  @Test
  public void testDestroy() throws Exception {
    instance.destroy();
    verify(iceAdapterApi).quit();
  }

  @Test
  public void testGameClosey() throws Exception {
    instance.onGameCloseRequested();
    verify(iceAdapterApi).quit();
  }

  @Test
  public void testSetIceAdapters() throws Exception {
    CoturnServer coturnServer = new CoturnServer();
    coturnServer.setCredential("test");
    coturnServer.setCredentialType("token");
    coturnServer.setUsername("0:1234");
    coturnServer.setUrls(Set.of(URI.create("turn://test.coturn.com?transport=tcp")));

    instance.setIceServers(List.of(coturnServer));

    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(iceAdapterApi).setIceServers(captor.capture());

    List<Map<String, Object>> value = captor.getValue();

    assertEquals(1, value.size());

    Map<String, Object> iceMap = value.get(0);

    assertEquals(coturnServer.getUsername(), iceMap.get("username"));
    assertEquals(coturnServer.getCredential(), iceMap.get("credential"));
    assertEquals(List.of("turn://test.coturn.com?transport=tcp"), iceMap.get("urls"));
  }

}
