package com.faforever.client.fa.relay.ice;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.GameLaunchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class IceAdapterSelectorTest extends ServiceTest {

  @Mock
  private IceAdapterImpl javaAdapter;
  @Mock
  private NativeIceAdapter nativeAdapter;

  private ForgedAlliancePrefs forgedAlliancePrefs;
  private IceAdapterSelector instance;

  @BeforeEach
  public void setUp() {
    forgedAlliancePrefs = new ForgedAlliancePrefs();
    instance = new IceAdapterSelector(javaAdapter, nativeAdapter, forgedAlliancePrefs);
    lenient().when(nativeAdapter.gameArguments()).thenReturn(List.of("/nativenet"));
  }

  private GameLaunchResponse launchOf(String featuredMod) {
    GameLaunchResponse response = mock(GameLaunchResponse.class);
    // With the setting off the selector never reads the mod.
    lenient().when(response.getFeatureMod()).thenReturn(featuredMod);
    return response;
  }

  @Test
  public void testNativeWhenTurnedOnAndFafDevelop() {
    forgedAlliancePrefs.setNativeNetwork(true);

    instance.prepare(launchOf("fafdevelop"));
    instance.start(1, false);

    verify(nativeAdapter).start(1, false);
    verify(javaAdapter, never()).start(anyInt(), anyBoolean());
    assertEquals(List.of("/nativenet"), instance.gameArguments());
  }

  @Test
  public void testJavaAdapterOnAnyOtherMod() {
    forgedAlliancePrefs.setNativeNetwork(true);

    instance.prepare(launchOf("faf"));
    instance.start(1, false);

    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
  }

  @Test
  public void testJavaAdapterWhenTurnedOff() {
    instance.prepare(launchOf("fafdevelop"));
    instance.start(1, false);

    verify(javaAdapter).start(1, false);
    verify(nativeAdapter, never()).start(anyInt(), anyBoolean());
  }

  @Test
  public void testJavaAdapterBeforeAnyGame() {
    instance.stop();

    verify(javaAdapter).stop();
    verify(nativeAdapter, never()).stop();
  }
}
