package com.faforever.client.fa.rendering;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.RenderingBackend;
import com.faforever.client.test.PlatformTest;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class DownloadRenderingWrapperTaskTest extends PlatformTest {

  private DownloadRenderingWrapperTask instance;

  @Mock
  private I18n i18n;
  @Spy
  private DataPrefs dataPrefs;

  @TempDir
  Path tempDir;

  @BeforeEach
  public void setUp() {
    dataPrefs.setBaseDataDirectory(tempDir);
    instance = new DownloadRenderingWrapperTask(i18n, dataPrefs);
    when(i18n.get(anyString(), any())).thenReturn("Downloading...");
  }

  @Test
  public void testExtractFromTarGz() throws Exception {
    Path archive = createTarGzArchive("dxvk-2.5.3/x32/d3d9.dll", "fake dll content");

    instance.setBackend(RenderingBackend.VULKAN_DXVK);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v2.5.3");

    instance.call();

    Path wrapperDir = tempDir.resolve("bin/rendering/vulkan_dxvk");
    assertTrue(Files.exists(wrapperDir.resolve("d3d9.dll")));
    assertEquals("fake dll content", Files.readString(wrapperDir.resolve("d3d9.dll")));
    assertEquals("v2.5.3", Files.readString(wrapperDir.resolve("version.txt")));
  }

  @Test
  public void testExtractFromZip() throws Exception {
    Path archive = createZipArchive("d3d9.dll", "fake d3d9on12 content");

    instance.setBackend(RenderingBackend.DIRECTX_12);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v1.0.0");

    instance.call();

    Path wrapperDir = tempDir.resolve("bin/rendering/directx_12");
    assertTrue(Files.exists(wrapperDir.resolve("d3d9.dll")));
    assertEquals("fake d3d9on12 content", Files.readString(wrapperDir.resolve("d3d9.dll")));
    assertEquals("v1.0.0", Files.readString(wrapperDir.resolve("version.txt")));
  }

  @Test
  public void testExistingDllIsOverwritten() throws Exception {
    Path wrapperDir = tempDir.resolve("bin/rendering/directx_12");
    Files.createDirectories(wrapperDir);
    Files.writeString(wrapperDir.resolve("d3d9.dll"), "old content");

    Path archive = createZipArchive("d3d9.dll", "new content");

    instance.setBackend(RenderingBackend.DIRECTX_12);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v2.0.0");

    instance.call();

    assertEquals("new content", Files.readString(wrapperDir.resolve("d3d9.dll")));
    assertEquals("v2.0.0", Files.readString(wrapperDir.resolve("version.txt")));
  }

  @Test
  public void testTarGzWithNestedPath() throws Exception {
    Path archive = createTarGzArchive("some-release/x32/d3d9.dll", "nested dll");

    instance.setBackend(RenderingBackend.VULKAN_DXVK);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v3.0.0");

    instance.call();

    Path wrapperDir = tempDir.resolve("bin/rendering/vulkan_dxvk");
    assertTrue(Files.exists(wrapperDir.resolve("d3d9.dll")));
    assertEquals("nested dll", Files.readString(wrapperDir.resolve("d3d9.dll")));
  }

  @Test
  public void testDllNotFoundInArchiveThrows() throws Exception {
    Path archive = createZipArchive("other_file.txt", "not a dll");

    instance.setBackend(RenderingBackend.DIRECTX_12);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v1.0.0");

    assertThrows(IOException.class, () -> instance.call());
  }

  @Test
  public void testTempFileCleanedUpOnSuccess() throws Exception {
    Path archive = createZipArchive("d3d9.dll", "content");

    instance.setBackend(RenderingBackend.DIRECTX_12);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v1.0.0");

    instance.call();

    Path wrapperDir = tempDir.resolve("bin/rendering/directx_12");
    long tempFiles = Files.list(wrapperDir)
        .filter(p -> p.getFileName().toString().startsWith("rendering-wrapper"))
        .count();
    assertEquals(0, tempFiles);
  }

  @Test
  public void testTempFileCleanedUpOnFailure() throws Exception {
    Path archive = createZipArchive("wrong_file.txt", "not a dll");

    instance.setBackend(RenderingBackend.DIRECTX_12);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v1.0.0");

    assertThrows(IOException.class, () -> instance.call());

    Path wrapperDir = tempDir.resolve("bin/rendering/directx_12");
    if (Files.exists(wrapperDir)) {
      long tempFiles = Files.list(wrapperDir)
          .filter(p -> p.getFileName().toString().startsWith("rendering-wrapper"))
          .count();
      assertEquals(0, tempFiles);
    }
  }

  @Test
  public void testZipWithNestedDirectoryEntry() throws Exception {
    Path archive = createZipArchiveWithDirectory("release/d3d9.dll", "nested zip dll");

    instance.setBackend(RenderingBackend.DIRECTX_12);
    instance.setDownloadUrl(archive.toUri().toURL());
    instance.setVersion("v1.0.0");

    // matchesEntryPath matches "release/d3d9.dll" because it ends with "/d3d9.dll"
    instance.call();

    Path wrapperDir = tempDir.resolve("bin/rendering/directx_12");
    assertTrue(Files.exists(wrapperDir.resolve("d3d9.dll")));
    assertEquals("nested zip dll", Files.readString(wrapperDir.resolve("d3d9.dll")));
  }

  private Path createTarGzArchive(String entryPath, String content) throws IOException {
    Path archive = tempDir.resolve("test-archive.tar.gz");
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    try (OutputStream fileOut = Files.newOutputStream(archive);
         GZIPOutputStream gzOut = new GZIPOutputStream(fileOut);
         TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)) {
      TarArchiveEntry entry = new TarArchiveEntry(entryPath);
      entry.setSize(bytes.length);
      tarOut.putArchiveEntry(entry);
      tarOut.write(bytes);
      tarOut.closeArchiveEntry();
    }

    return archive;
  }

  private Path createZipArchive(String entryPath, String content) throws IOException {
    Path archive = tempDir.resolve("test-archive.zip");
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(archive))) {
      zipOut.putNextEntry(new ZipEntry(entryPath));
      zipOut.write(bytes);
      zipOut.closeEntry();
    }

    return archive;
  }

  private Path createZipArchiveWithDirectory(String entryPath, String content) throws IOException {
    Path archive = tempDir.resolve("test-archive-nested.zip");
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(archive))) {
      zipOut.putNextEntry(new ZipEntry("release/"));
      zipOut.closeEntry();
      zipOut.putNextEntry(new ZipEntry(entryPath));
      zipOut.write(bytes);
      zipOut.closeEntry();
    }

    return archive;
  }
}
