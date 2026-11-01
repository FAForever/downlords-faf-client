package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IceServiceTest extends ServiceTest {

  @TempDir
  public Path tempDirectory;

  @Mock
  private TaskService taskService;
  @Mock
  private ObjectFactory<DownloadIceAdapterTask> downloadIceAdapterTaskFactory;
  @Mock
  private DownloadIceAdapterTask downloadIceAdapterTask;

  private MockWebServer mockGitHub;
  private IceService instance;

  @BeforeEach
  public void setUp() throws Exception {
    mockGitHub = new MockWebServer();
    mockGitHub.start();

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getIceAdapter()
                    .setQueryLatestVersionUrl(
                        String.format("http://localhost:%s/releases/latest", mockGitHub.getPort()));

    DataPrefs dataPrefs = new DataPrefs();
    dataPrefs.setBaseDataDirectory(tempDirectory);

    instance = new IceService(clientProperties, dataPrefs, taskService, WebClient.builder().build(),
                              downloadIceAdapterTaskFactory);
  }

  @AfterEach
  public void tearDown() throws Exception {
    mockGitHub.shutdown();
  }

  private void prepareLatestReleaseResponse(String tagName) {
    mockGitHub.enqueue(new MockResponse().setBody(String.format("{\"tag_name\":\"%s\"}", tagName))
                                         .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));
  }

  private void prepareErrorResponse() {
    mockGitHub.enqueue(new MockResponse().setResponseCode(403));
  }

  private void prepareDownloadTask() {
    when(downloadIceAdapterTaskFactory.getObject()).thenReturn(downloadIceAdapterTask);
    when(downloadIceAdapterTask.getMono()).thenReturn(Mono.empty());
    when(taskService.submitTask(downloadIceAdapterTask)).thenReturn(downloadIceAdapterTask);
  }

  private Path createLocalIceAdapterJar(String version) throws Exception {
    Path iceAdapterJar = instance.getIceAdapterJarPath(version);
    Files.createDirectories(iceAdapterJar.getParent());
    return Files.createFile(iceAdapterJar);
  }

  @Test
  public void testGetIceAdapterJarPath() {
    assertEquals(tempDirectory.resolve(DataPrefs.ICE_ADAPTER_SUB_DIRECTORY).resolve("faf-ice-adapter-3.3.14.jar"),
                 instance.getIceAdapterJarPath("3.3.14"));
  }

  @Test
  public void testQueryLatestVersion() {
    prepareLatestReleaseResponse("3.3.14");

    StepVerifier.create(instance.queryLatestVersion()).expectNext("3.3.14").verifyComplete();
  }

  @Test
  public void testGetNewestIceAdapterDownloadsLatestVersion() {
    prepareLatestReleaseResponse("3.3.14");
    prepareDownloadTask();

    StepVerifier.create(instance.getNewestIceAdapter())
                .expectNext(instance.getIceAdapterJarPath("3.3.14"))
                .verifyComplete();

    verify(downloadIceAdapterTask).setVersion("3.3.14");
    verify(downloadIceAdapterTask).setTargetFile(instance.getIceAdapterJarPath("3.3.14"));
  }

  @Test
  public void testGetNewestIceAdapterUsesExistingLatestVersion() throws Exception {
    prepareLatestReleaseResponse("3.3.14");
    Path iceAdapterJar = createLocalIceAdapterJar("3.3.14");

    StepVerifier.create(instance.getNewestIceAdapter()).expectNext(iceAdapterJar).verifyComplete();

    verify(downloadIceAdapterTaskFactory, never()).getObject();
    verify(taskService, never()).submitTask(any());
  }

  @Test
  public void testGetNewestIceAdapterFallsBackToNewestLocalJar() throws Exception {
    prepareErrorResponse();
    createLocalIceAdapterJar("3.3.9");
    Path newestJar = createLocalIceAdapterJar("3.3.14");

    StepVerifier.create(instance.getNewestIceAdapter()).expectNext(newestJar).verifyComplete();

    verify(taskService, never()).submitTask(any());
  }

  @Test
  public void testGetNewestIceAdapterFailsWithoutLocalJar() {
    prepareErrorResponse();

    StepVerifier.create(instance.getNewestIceAdapter()).verifyError();
  }

  @Test
  public void testFindNewestLocalIceAdapterJarWithoutDirectory() {
    assertEquals(Optional.empty(), instance.findNewestLocalIceAdapterJar());
  }

  @Test
  public void testFindNewestLocalIceAdapterJarIgnoresOtherFiles() throws Exception {
    Path iceAdapterJar = createLocalIceAdapterJar("3.3.14");
    Files.createFile(iceAdapterJar.resolveSibling("ice-adapter12345.tmp"));

    assertEquals(Optional.of(iceAdapterJar), instance.findNewestLocalIceAdapterJar());
  }
}
