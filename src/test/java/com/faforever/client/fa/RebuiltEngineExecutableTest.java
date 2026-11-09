package com.faforever.client.fa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RebuiltEngineExecutableTest {

  private static final String PRODUCT_NAME = RebuiltEngineExecutable.PRODUCT_NAME;

  @TempDir
  public Path tempDirectory;

  @Test
  public void testRebuiltEngine64Bit() throws Exception {
    assertTrue(RebuiltEngineExecutable.isRebuiltEngine(exe(true, PRODUCT_NAME)));
  }

  @Test
  public void testRebuiltEngine32Bit() throws Exception {
    assertTrue(RebuiltEngineExecutable.isRebuiltEngine(exe(false, PRODUCT_NAME)));
  }

  @Test
  public void testOtherProduct() throws Exception {
    Path executable = exe(false, "Supreme Commander Forged Alliance");

    assertEquals(Optional.of("Supreme Commander Forged Alliance"), RebuiltEngineExecutable.readProductName(executable));
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(executable));
  }

  @Test
  public void testNoVersionResource() throws Exception {
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(exe(true, null)));
  }

  @Test
  public void testVarFileInfoBeforeStringFileInfo() throws Exception {
    Path executable = TestExecutables.writeVarFileInfoFirst(tempDirectory.resolve("ForgedAlliance.exe"), PRODUCT_NAME);

    assertTrue(RebuiltEngineExecutable.isRebuiltEngine(executable));
  }

  @Test
  public void testNoProductName() throws Exception {
    Path executable = TestExecutables.writeWithoutProductName(tempDirectory.resolve("ForgedAlliance.exe"));

    assertEquals(Optional.empty(), RebuiltEngineExecutable.readProductName(executable));
  }

  @Test
  public void testProductNameValueLengthWithoutTerminator() throws Exception {
    Path executable = TestExecutables.writeProductNameValueLength(tempDirectory.resolve("ForgedAlliance.exe"), PRODUCT_NAME, 6);

    assertTrue(RebuiltEngineExecutable.isRebuiltEngine(executable));
  }

  @Test
  @Timeout(10)
  public void testZeroLengthEntryEndsTheWalk() throws Exception {
    Path executable = TestExecutables.writeZeroLengthEntry(tempDirectory.resolve("ForgedAlliance.exe"), PRODUCT_NAME);

    assertEquals(Optional.empty(), RebuiltEngineExecutable.readProductName(executable));
  }

  @Test
  public void testVersionInfoEndingInsideTheProductName() throws Exception {
    // The product name is followed by its terminating zero and two bytes of padding; 6 bytes less also drops its
    // last character.
    Path executable = TestExecutables.writeVersionInfoCut(tempDirectory.resolve("ForgedAlliance.exe"), PRODUCT_NAME, 6);

    assertEquals(Optional.of(PRODUCT_NAME.substring(0, PRODUCT_NAME.length() - 1)), RebuiltEngineExecutable.readProductName(executable));
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(executable));
  }

  @Test
  public void testResourceDirectoryInsideSection() throws Exception {
    Path executable = TestExecutables.writeResourceDirectoryInsideSection(tempDirectory.resolve("ForgedAlliance.exe"), PRODUCT_NAME, 32);

    assertEquals(Optional.of(PRODUCT_NAME), RebuiltEngineExecutable.readProductName(executable));
  }

  @Test
  public void testResourcesWithoutVersion() throws Exception {
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(TestExecutables.writeIconOnly(tempDirectory.resolve("ForgedAlliance.exe"))));
  }

  @Test
  public void testResourceOutsideSections() throws Exception {
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(TestExecutables.writeResourceOutsideSections(tempDirectory.resolve("ForgedAlliance.exe"))));
  }

  @Test
  public void testNoPeSignature() throws Exception {
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(TestExecutables.writeWithoutPeSignature(tempDirectory.resolve("ForgedAlliance.exe"))));
  }

  @Test
  public void testNotAnExecutable() throws Exception {
    Path file = Files.writeString(tempDirectory.resolve("ForgedAlliance.exe"), "not an executable");

    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(file));
  }

  @Test
  public void testNoDosHeader() throws Exception {
    Path file = Files.writeString(tempDirectory.resolve("ForgedAlliance.exe"), "x".repeat(256));

    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(file));
  }

  @Test
  public void testMissingFile() {
    assertFalse(RebuiltEngineExecutable.isRebuiltEngine(tempDirectory.resolve("ForgedAlliance.exe")));
  }

  private Path exe(boolean pe32Plus, String productName) throws Exception {
    return TestExecutables.write(tempDirectory.resolve("ForgedAlliance.exe"), pe32Plus, productName);
  }
}
