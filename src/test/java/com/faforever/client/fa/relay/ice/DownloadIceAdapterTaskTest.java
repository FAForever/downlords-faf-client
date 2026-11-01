package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DownloadIceAdapterTaskTest extends PlatformTest {

  @TempDir
  public Path tempDirectory;

  @Mock
  private I18n i18n;

  private ClientProperties clientProperties;
  private DownloadIceAdapterTask instance;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();
    instance = new DownloadIceAdapterTask(clientProperties, i18n);
  }

  @Test
  public void testCallWithoutVersionThrowsException() {
    instance.setTargetFile(tempDirectory.resolve("faf-ice-adapter.jar"));
    assertEquals("Version hasn't been set.",
                 assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCallWithoutTargetFileThrowsException() {
    instance.setVersion("3.3.14");
    assertEquals("Target file hasn't been set.",
                 assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCall() throws Exception {
    Path sourceDirectory = Files.createDirectories(tempDirectory.resolve("source"));
    Path sourceFile = Files.writeString(sourceDirectory.resolve("ice-adapter-3.3.14.jar"), "ice adapter content");
    clientProperties.getIceAdapter().setDownloadUrlFormat(sourceDirectory.toUri() + "ice-adapter-%s.jar");

    Path targetFile = tempDirectory.resolve("download").resolve("faf-ice-adapter-3.3.14.jar");
    instance.setVersion("3.3.14");
    instance.setTargetFile(targetFile);

    instance.call();

    assertEquals(Files.readString(sourceFile), Files.readString(targetFile));
    try (Stream<Path> files = Files.list(targetFile.getParent())) {
      assertEquals(List.of(targetFile), files.toList());
    }
  }
}
