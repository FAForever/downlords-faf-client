package com.faforever.client.fa;

import org.junit.jupiter.api.Test;
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
  public void testNotAnExecutable() throws Exception {
    Path file = Files.writeString(tempDirectory.resolve("ForgedAlliance.exe"), "not an executable");

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
