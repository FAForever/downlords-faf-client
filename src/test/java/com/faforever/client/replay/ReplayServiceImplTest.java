package com.faforever.client.replay;

import com.faforever.client.game.FeaturedMod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReplayServiceImplTest {

  @Test
  public void testParseSupComVersion() throws Exception {
    /*
    First 64 bytes of a SCFAReplay file with version 3632. ASCII representation:
    Supreme Commande
    r v1.50.3632....
    Replay v1.9../ma
    ps/SCMP_012/SCMP
    */
    byte[] bytes = new byte[]{
        0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
        0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x36, 0x33, 0x32, 0x00, 0x0D, 0x0A, 0x00,
        0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
        0x70, 0x73, 0x2F, 0x53, 0x43, 0x4D, 0x50, 0x5F, 0x30, 0x31, 0x32, 0x2F, 0x53, 0x43, 0x4D, 0x50
    };

    String version = ReplayServiceImpl.parseSupComVersion(bytes);

    assertEquals("3632", version);
  }

  @Test
  public void testGuessModByFileNameModIsMissing() throws Exception {
    String mod = ReplayServiceImpl.guessModByFileName("110621-2128 Saltrock Colony.SCFAReplay");

    assertEquals(FeaturedMod.DEFAULT_MOD.getString(), mod);
  }

  @Test
  public void testGuessModByFileNameModIsBlackops() throws Exception {
    String mod = ReplayServiceImpl.guessModByFileName("110621-2128 Saltrock Colony.blackops.SCFAReplay");

    assertEquals("blackops", mod);
  }
}
