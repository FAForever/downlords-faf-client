package com.faforever.client.map.generator;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadMapGeneratorTaskTest extends AbstractPlainJavaFxTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder downloadDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder sourceDirectory = new TemporaryFolder();

  private DownloadMapGeneratorTask instance;

  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private MapGeneratorService mapGeneratorService;

  @Before
  public void setUp() throws Exception {
    File generatorFile = sourceDirectory.newFile("NeroxisGenMock.jar");

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getMapGenerator().setDownloadUrlFormat(generatorFile.toURI().toURL().toString() + "%1$s");
    instance = new DownloadMapGeneratorTask(mapGeneratorService, clientProperties, i18n, platformService);
  }

  @Test
  public void testCallWithoutVersionThrowsException() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(startsWith("Version hasn't been set."));

    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    instance.setVersion("");//mock version to prevent a subdirectory with the name of the version
    when(mapGeneratorService.getGeneratorExecutablePath()).thenReturn(downloadDirectory.getRoot().toPath());

    instance.call();

    assertThat(Arrays.asList(Objects.requireNonNull(downloadDirectory.getRoot().listFiles())), contains(downloadDirectory.getRoot().toPath().resolve(String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, "")).toFile()));
    verify(platformService).setUnixExecutableAndWritableBits(any());
  }
}
