package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.RebuiltEngineExecutable;
import com.faforever.client.fa.TestExecutables;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.MessageTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Sinks;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class IceAdapterSelectorTest extends ServiceTest {

  private static final String PRODUCT_NAME = RebuiltEngineExecutable.PRODUCT_NAME;

  @TempDir
  public Path tempDirectory;

  @Mock
  private IceAdapterImpl javaAdapter;
  @Mock
  private NativeIceAdapter nativeAdapter;
  @Mock
  private ForgedAllianceLaunchService forgedAllianceLaunchService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  // No auto-cancel: after a command the adapter can't handle, the selector subscribes again, as the lobby events allow.
  private final Sinks.Many<JoinGameGpgCommand> joinGame = Sinks.many().multicast().onBackpressureBuffer(256, false);
  private final Sinks.Many<HostGameGpgCommand> hostGame = Sinks.many().multicast().onBackpressureBuffer(256, false);
  private final Sinks.Many<ConnectToPeerGpgCommand> connectToPeer = Sinks.many().multicast().onBackpressureBuffer(256, false);
  private final Sinks.Many<DisconnectFromPeerGpgCommand> disconnectFromPeer = Sinks.many().multicast().onBackpressureBuffer(256, false);
  private final Sinks.Many<IceMsgGpgCommand> iceMsg = Sinks.many().multicast().onBackpressureBuffer(256, false);

  private IceAdapterSelector instance;

  @BeforeEach
  public void setUp() {
    lenient().when(fafServerAccessor.getEvents(JoinGameGpgCommand.class)).thenReturn(joinGame.asFlux());
    lenient().when(fafServerAccessor.getEvents(HostGameGpgCommand.class)).thenReturn(hostGame.asFlux());
    lenient().when(fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)).thenReturn(connectToPeer.asFlux());
    lenient().when(fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)).thenReturn(disconnectFromPeer.asFlux());
    lenient().when(fafServerAccessor.getEvents(IceMsgGpgCommand.class)).thenReturn(iceMsg.asFlux());
    instance = new IceAdapterSelector(javaAdapter, nativeAdapter, forgedAllianceLaunchService, forgedAlliancePrefs,
                                      fafServerAccessor);
    instance.afterPropertiesSet();
    lenient().when(nativeAdapter.gameArguments()).thenReturn(List.of("/nativenet"));
    lenient().when(javaAdapter.gameArguments()).thenReturn(List.of("/java"));
  }

  private GameLaunchResponse launchOf(String featuredMod) {
    GameLaunchResponse response = mock(GameLaunchResponse.class);
    lenient().when(response.getFeatureMod()).thenReturn(featuredMod);
    return response;
  }

  private void useExe(String productName) throws Exception {
    Path exe = TestExecutables.write(tempDirectory.resolve("ForgedAlliance.exe"), true, productName);
    // With the setting off the selector never reads the exe.
    lenient().when(forgedAllianceLaunchService.getExecutablePath()).thenReturn(exe);
  }

  @Test
  public void testNativeWithTheRebuiltEngineExeOnAnyMod() throws Exception {
    useExe(PRODUCT_NAME);

    for (String featuredMod : List.of("faf", "fafdevelop", "fafbeta", "ladder1v1")) {
      GameLaunchResponse launch = launchOf(featuredMod);
      instance.prepare(launch);
      instance.start(1, false);
      assertEquals(List.of("/nativenet"), instance.gameArguments());
      instance.stop();
      verify(nativeAdapter).prepare(launch);
    }

    verify(javaAdapter, never()).prepare(any());
    verify(javaAdapter, never()).start(anyInt(), anyBoolean());
  }

  @Test
  public void testTheSettingIsOnByDefault() {
    assertTrue(new ForgedAlliancePrefs().isUseNativeNetwork());
  }

  @Test
  public void testJavaAdapterWithTheRebuiltEngineWhenTheSettingIsOff() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setUseNativeNetwork(false);
    GameLaunchResponse launch = launchOf("faf");

    instance.prepare(launch);
    instance.start(1, false);

    verify(javaAdapter).prepare(launch);
    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
    // The Java adapter gives its own arguments, the rebuilt engine's included.
    assertEquals(List.of("/java"), instance.gameArguments());
  }

  @Test
  public void testJavaAdapterWithTheOriginalExe() throws Exception {
    useExe("Supreme Commander Forged Alliance");

    instance.prepare(launchOf("fafdevelop"));
    instance.start(1, false);

    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
    assertEquals(List.of("/java"), instance.gameArguments());
  }

  @Test
  public void testEveryOtherCallGoesToTheChosenAdapter() throws Exception {
    useExe(PRODUCT_NAME);
    instance.prepare(launchOf("faf"));
    GpgGameOutboundMessage message = new GpgGameOutboundMessage("GameState", List.of("Idle"), MessageTarget.GAME);
    List<CoturnServer> servers = List.of(new CoturnServer());
    Object record = new Object();

    instance.onIceAdapterStateChanged("Connected");
    instance.onGpgGameMessage(message);
    instance.setIceServers(servers);
    instance.hostGame("map");
    instance.joinGame("host", 3);
    instance.connectToPeer("other", 9, true);
    instance.disconnectFromPeer(9);
    instance.iceMsg(9, record);

    verify(nativeAdapter).onIceAdapterStateChanged("Connected");
    verify(nativeAdapter).onGpgGameMessage(message);
    verify(nativeAdapter).setIceServers(servers);
    verify(nativeAdapter).hostGame("map");
    verify(nativeAdapter).joinGame("host", 3);
    verify(nativeAdapter).connectToPeer("other", 9, true);
    verify(nativeAdapter).disconnectFromPeer(9);
    verify(nativeAdapter).iceMsg(9, record);
    verifyNoInteractions(javaAdapter);
  }

  @Test
  public void testEveryCallBeforePrepareIsRefused() {
    GpgGameOutboundMessage message = new GpgGameOutboundMessage("GameState", List.of("Idle"), MessageTarget.GAME);

    assertThrows(IllegalStateException.class, () -> instance.gameArguments());
    assertThrows(IllegalStateException.class, () -> instance.start(1, false));
    assertThrows(IllegalStateException.class, () -> instance.onIceAdapterStateChanged("Connected"));
    assertThrows(IllegalStateException.class, () -> instance.onGpgGameMessage(message));
    assertThrows(IllegalStateException.class, () -> instance.setIceServers(List.of()));
    assertThrows(IllegalStateException.class, () -> instance.hostGame("map"));
    assertThrows(IllegalStateException.class, () -> instance.joinGame("host", 3));
    assertThrows(IllegalStateException.class, () -> instance.connectToPeer("other", 9, true));
    assertThrows(IllegalStateException.class, () -> instance.disconnectFromPeer(9));
    assertThrows(IllegalStateException.class, () -> instance.iceMsg(9, "{}"));
    verifyNoInteractions(javaAdapter, nativeAdapter);
  }

  @Test
  public void testASecondPrepareIsRefusedAndKeepsTheFirstGamesAdapter() throws Exception {
    useExe(PRODUCT_NAME);
    instance.prepare(launchOf("faf"));
    forgedAlliancePrefs.setUseNativeNetwork(false);

    assertThrows(IllegalStateException.class, () -> instance.prepare(launchOf("faf")));

    assertEquals(List.of("/nativenet"), instance.gameArguments());
    verify(javaAdapter, never()).prepare(any());
  }

  @Test
  public void testStopEndsTheGameAndTheNextGameChoosesAgain() throws Exception {
    useExe(PRODUCT_NAME);
    instance.prepare(launchOf("faf"));
    instance.stop();
    forgedAlliancePrefs.setUseNativeNetwork(false);

    instance.prepare(launchOf("faf"));
    instance.start(2, false);

    verify(nativeAdapter).stop();
    verify(javaAdapter).start(2, false);
    instance.stop();
    verify(javaAdapter).stop();
    assertThrows(IllegalStateException.class, () -> instance.gameArguments());
  }

  @Test
  public void testAnAdapterThatFailsToStopStillEndsTheGame() throws Exception {
    useExe(PRODUCT_NAME);
    instance.prepare(launchOf("faf"));
    doThrow(new IllegalStateException("broken")).when(nativeAdapter).stop();

    assertThrows(IllegalStateException.class, () -> instance.stop());

    instance.prepare(launchOf("faf"));
    verify(nativeAdapter, times(2)).prepare(any());
  }

  @Test
  public void testAnAdapterThatFailsToPrepareIsNotSelected() throws Exception {
    useExe(PRODUCT_NAME);
    doThrow(new IllegalStateException("broken")).when(nativeAdapter).prepare(any());

    assertThrows(IllegalStateException.class, () -> instance.prepare(launchOf("faf")));

    assertThrows(IllegalStateException.class, () -> instance.gameArguments());
  }

  @Test
  public void testStopWithoutAGameDoesNothing() {
    instance.stop();

    verifyNoInteractions(javaAdapter, nativeAdapter);
  }

  @Test
  public void testTheServersCommandsGoToTheChosenAdapter() throws Exception {
    useExe(PRODUCT_NAME);
    instance.prepare(launchOf("faf"));

    joinGame.tryEmitNext(joinGameCommand("host", 3));
    hostGame.tryEmitNext(hostGameCommand("map"));
    connectToPeer.tryEmitNext(connectToPeerCommand("other", 9, true));
    disconnectFromPeer.tryEmitNext(disconnectFromPeerCommand(9));
    iceMsg.tryEmitNext(iceMsgCommand(9, "{}"));

    verify(nativeAdapter).joinGame("host", 3);
    verify(nativeAdapter).hostGame("map");
    verify(nativeAdapter).connectToPeer("other", 9, true);
    verify(nativeAdapter).disconnectFromPeer(9);
    verify(nativeAdapter).iceMsg(9, "{}");
    verifyNoInteractions(javaAdapter);
  }

  @Test
  public void testAServerCommandWithoutAGameIsDroppedAndLaterOnesArrive() throws Exception {
    connectToPeer.tryEmitNext(connectToPeerCommand("early", 8, true));
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setUseNativeNetwork(false);
    instance.prepare(launchOf("faf"));

    connectToPeer.tryEmitNext(connectToPeerCommand("other", 9, false));

    verify(javaAdapter).connectToPeer("other", 9, false);
    verify(javaAdapter, never()).connectToPeer("early", 8, true);
  }

  @Test
  public void testAServerCommandTheAdapterRefusesIsDroppedAndTheNextOneArrives() throws Exception {
    useExe(PRODUCT_NAME);
    instance.prepare(launchOf("faf"));
    Object broken = new Object();
    doThrow(new IllegalArgumentException("Unable to write an ICE message")).when(nativeAdapter).iceMsg(9, broken);

    iceMsg.tryEmitNext(iceMsgCommand(9, broken));
    iceMsg.tryEmitNext(iceMsgCommand(9, "{}"));

    verify(nativeAdapter).iceMsg(9, "{}");
  }

  private static JoinGameGpgCommand joinGameCommand(String username, int peerUid) {
    JoinGameGpgCommand command = mock(JoinGameGpgCommand.class);
    when(command.getUsername()).thenReturn(username);
    when(command.getPeerUid()).thenReturn(peerUid);
    return command;
  }

  private static HostGameGpgCommand hostGameCommand(String map) {
    HostGameGpgCommand command = mock(HostGameGpgCommand.class);
    when(command.getMap()).thenReturn(map);
    return command;
  }

  private static ConnectToPeerGpgCommand connectToPeerCommand(String username, int peerUid, boolean offer) {
    ConnectToPeerGpgCommand command = mock(ConnectToPeerGpgCommand.class);
    lenient().when(command.getUsername()).thenReturn(username);
    lenient().when(command.getPeerUid()).thenReturn(peerUid);
    lenient().when(command.isOffer()).thenReturn(offer);
    return command;
  }

  private static DisconnectFromPeerGpgCommand disconnectFromPeerCommand(int uid) {
    DisconnectFromPeerGpgCommand command = mock(DisconnectFromPeerGpgCommand.class);
    when(command.getUid()).thenReturn(uid);
    return command;
  }

  private static IceMsgGpgCommand iceMsgCommand(int sender, Object record) {
    IceMsgGpgCommand command = mock(IceMsgGpgCommand.class);
    when(command.getSender()).thenReturn(sender);
    when(command.getRecord()).thenReturn(record);
    return command;
  }
}
