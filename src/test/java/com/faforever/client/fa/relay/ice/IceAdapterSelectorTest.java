package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.RebuiltEngineExecutable;
import com.faforever.client.fa.TestExecutables;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameLaunchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  private IceAdapterSelector instance;

  @BeforeEach
  public void setUp() {
    instance = new IceAdapterSelector(javaAdapter, nativeAdapter, forgedAllianceLaunchService, forgedAlliancePrefs);
    lenient().when(nativeAdapter.gameArguments()).thenReturn(List.of("/nativenet"));
  }

  private GameLaunchResponse launchOf(String featuredMod) {
    GameLaunchResponse response = mock(GameLaunchResponse.class);
    lenient().when(response.getFeatureMod()).thenReturn(featuredMod);
    return response;
  }

  private void useExe(String productName) throws Exception {
    Path exe = TestExecutables.write(tempDirectory.resolve("ForgedAlliance.exe"), true, productName);
    // On any other mod the selector never reads the exe.
    lenient().when(forgedAllianceLaunchService.getExecutablePath()).thenReturn(exe);
  }

  @Test
  public void testNativeOnFafDevelopWithRebuiltEngineExe() throws Exception {
    useExe(PRODUCT_NAME);

    instance.prepare(launchOf("fafdevelop"));
    instance.start(1, false);

    verify(nativeAdapter).start(1, false);
    verify(javaAdapter, never()).start(anyInt(), anyBoolean());
    assertEquals(List.of("/nativenet"), instance.gameArguments());
  }

  @Test
  public void testTheSettingIsOnByDefault() {
    assertTrue(new ForgedAlliancePrefs().isUseNativeNetwork());
  }

  @Test
  public void testJavaAdapterOnFafDevelopWhenTheSettingIsOff() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setUseNativeNetwork(false);

    instance.prepare(launchOf("fafdevelop"));
    instance.start(1, false);

    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
    // Still takes other players back after their crash.
    assertEquals(List.of("/con", "net_PeerFeatures=1;net_RejoinAccept=1"), instance.gameArguments());
  }

  @Test
  public void testJavaAdapterOnFafDevelopWithOriginalExe() throws Exception {
    useExe("Supreme Commander Forged Alliance");

    instance.prepare(launchOf("fafdevelop"));
    instance.start(1, false);

    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
  }

  @Test
  public void testJavaAdapterOnAnyOtherMod() throws Exception {
    useExe(PRODUCT_NAME);

    instance.prepare(launchOf("faf"));
    instance.start(1, false);

    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
    assertEquals(List.of(), instance.gameArguments());
  }

  @Test
  public void testJavaAdapterBeforeAnyGame() {
    instance.stop();

    verify(javaAdapter).stop();
    verify(nativeAdapter, never()).stop();
  }
}
