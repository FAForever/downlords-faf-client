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

public class FafReExecutableTest {

  @TempDir
  public Path tempDirectory;

  @Test
  public void testFafRe64Bit() throws Exception {
    assertTrue(FafReExecutable.isFafRe(exe(true, "faf-re")));
  }

  @Test
  public void testFafRe32Bit() throws Exception {
    assertTrue(FafReExecutable.isFafRe(exe(false, "faf-re")));
  }

  @Test
  public void testOtherProduct() throws Exception {
    Path executable = exe(false, "Supreme Commander Forged Alliance");

    assertEquals(Optional.of("Supreme Commander Forged Alliance"), FafReExecutable.readProductName(executable));
    assertFalse(FafReExecutable.isFafRe(executable));
  }

  @Test
  public void testNoVersionResource() throws Exception {
    assertFalse(FafReExecutable.isFafRe(exe(true, null)));
  }

  @Test
  public void testVarFileInfoBeforeStringFileInfo() throws Exception {
    Path executable = TestExecutables.writeVarFileInfoFirst(tempDirectory.resolve("ForgedAlliance.exe"), "faf-re");

    assertTrue(FafReExecutable.isFafRe(executable));
  }

  @Test
  public void testNoProductName() throws Exception {
    Path executable = TestExecutables.writeWithoutProductName(tempDirectory.resolve("ForgedAlliance.exe"));

    assertEquals(Optional.empty(), FafReExecutable.readProductName(executable));
  }

  @Test
  public void testProductNameValueLengthWithoutTerminator() throws Exception {
    Path executable = TestExecutables.writeProductNameValueLength(tempDirectory.resolve("ForgedAlliance.exe"), "faf-re", 6);

    assertTrue(FafReExecutable.isFafRe(executable));
  }

  @Test
  @Timeout(10)
  public void testZeroLengthEntryEndsTheWalk() throws Exception {
    Path executable = TestExecutables.writeZeroLengthEntry(tempDirectory.resolve("ForgedAlliance.exe"), "faf-re");

    assertEquals(Optional.empty(), FafReExecutable.readProductName(executable));
  }

  @Test
  public void testVersionInfoEndingInsideTheProductName() throws Exception {
    // "faf-re" is followed by its terminating zero and two bytes of padding; 6 bytes less also drops the "e".
    Path executable = TestExecutables.writeVersionInfoCut(tempDirectory.resolve("ForgedAlliance.exe"), "faf-re", 6);

    assertEquals(Optional.of("faf-r"), FafReExecutable.readProductName(executable));
    assertFalse(FafReExecutable.isFafRe(executable));
  }

  @Test
  public void testResourcesWithoutVersion() throws Exception {
    assertFalse(FafReExecutable.isFafRe(TestExecutables.writeIconOnly(tempDirectory.resolve("ForgedAlliance.exe"))));
  }

  @Test
  public void testResourceOutsideSections() throws Exception {
    assertFalse(FafReExecutable.isFafRe(TestExecutables.writeResourceOutsideSections(tempDirectory.resolve("ForgedAlliance.exe"))));
  }

  @Test
  public void testNoPeSignature() throws Exception {
    assertFalse(FafReExecutable.isFafRe(TestExecutables.writeWithoutPeSignature(tempDirectory.resolve("ForgedAlliance.exe"))));
  }

  @Test
  public void testNotAnExecutable() throws Exception {
    Path file = Files.writeString(tempDirectory.resolve("ForgedAlliance.exe"), "not an executable");

    assertFalse(FafReExecutable.isFafRe(file));
  }

  @Test
  public void testNoDosHeader() throws Exception {
    Path file = Files.writeString(tempDirectory.resolve("ForgedAlliance.exe"), "x".repeat(256));

    assertFalse(FafReExecutable.isFafRe(file));
  }

  @Test
  public void testMissingFile() {
    assertFalse(FafReExecutable.isFafRe(tempDirectory.resolve("ForgedAlliance.exe")));
  }

  private Path exe(boolean pe32Plus, String productName) throws Exception {
    return TestExecutables.write(tempDirectory.resolve("ForgedAlliance.exe"), pe32Plus, productName);
  }
}
