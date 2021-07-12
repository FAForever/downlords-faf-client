package com.faforever.client.map.generator;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadMapGeneratorTaskTest extends ServiceTest {

  @TempDir
  public Path tempDirectory;
  public Path downloadDirectory;
  public Path sourceDirectory;

  private DownloadMapGeneratorTask instance;

  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private MapGeneratorService mapGeneratorService;

  private ClientProperties clientProperties;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();
    downloadDirectory = Files.createDirectories(tempDirectory.resolve("download"));
    sourceDirectory = Files.createDirectories(tempDirectory.resolve("source"));

    instance = new DownloadMapGeneratorTask(mapGeneratorService, clientProperties, i18n, platformService);
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    assertEquals("Version hasn't been set.", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Disabled("Cannot delete the temp directory")
  @Test
  public void testCall() throws Exception {
    instance.setVersion("");//mock version to prevent a subdirectory with the name of the version
    when(mapGeneratorService.getGeneratorExecutablePath()).thenReturn(downloadDirectory);

    File generatorFile = Files.createFile(sourceDirectory.resolve("NeroxisGenMock.jar")).toFile();
    clientProperties.getMapGenerator().setDownloadUrlFormat(generatorFile.toURI().toURL() + "%1$s");
    instance.call();

    assertThat(List.of(Objects.requireNonNull(Files.list(downloadDirectory))), contains(downloadDirectory.resolve(String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, "")).toFile()));
    verify(platformService).setUnixExecutableAndWritableBits(any());
  }
}
