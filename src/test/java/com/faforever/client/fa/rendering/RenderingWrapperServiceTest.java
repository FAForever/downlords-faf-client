package com.faforever.client.fa.rendering;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.RenderingBackend;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.GitHubAssets;
import com.faforever.client.update.GitHubRelease;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RenderingWrapperServiceTest extends ServiceTest {

  @InjectMocks
  private RenderingWrapperService instance;

  @Mock
  private TaskService taskService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private WebClient defaultWebClient;
  @Mock
  private ObjectFactory<DownloadRenderingWrapperTask> downloadTaskFactory;

  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ClientProperties clientProperties;

  @TempDir
  Path tempDir;

  @BeforeEach
  public void setUp() {
    dataPrefs.setBaseDataDirectory(tempDir);

    ClientProperties.RenderingWrapper renderingWrapper = new ClientProperties.RenderingWrapper();
    renderingWrapper.setDxvkQueryLatestVersionUrl("https://api.github.com/repos/doitsujin/dxvk/releases/latest");
    renderingWrapper.setD3d9on12QueryLatestVersionUrl("https://api.github.com/repos/narzoul/ForceD3D9On12/releases/latest");
    clientProperties.setRenderingWrapper(renderingWrapper);
  }

  @Test
  public void testEnsureWrapperAvailableForDirectX9() {
    assertTrue(instance.ensureWrapperAvailable(RenderingBackend.DIRECTX_9));
    verify(defaultWebClient, never()).get();
  }

  @Test
  public void testEnsureWrapperAvailableWhenAlreadyInstalled() throws Exception {
    Path wrapperDir = instance.getWrapperDirectory(RenderingBackend.VULKAN_DXVK);
    Files.createDirectories(wrapperDir);
    Files.createFile(wrapperDir.resolve("d3d9.dll"));

    assertTrue(instance.ensureWrapperAvailable(RenderingBackend.VULKAN_DXVK));
    verify(defaultWebClient, never()).get();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEnsureWrapperAvailableTriggersDownload() throws Exception {
    GitHubAssets asset = new GitHubAssets();
    asset.setName("dxvk-2.5.3.tar.gz");
    asset.setBrowserDownloadUrl(new URL("https://github.com/doitsujin/dxvk/releases/download/v2.5.3/dxvk-2.5.3.tar.gz"));

    GitHubRelease release = new GitHubRelease();
    release.setTagName("v2.5.3");
    release.setAssets(List.of(asset));

    RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
    RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
    ResponseSpec responseSpec = mock(ResponseSpec.class);

    when(defaultWebClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(GitHubRelease.class)).thenReturn(Mono.just(release));

    DownloadRenderingWrapperTask task = mock(DownloadRenderingWrapperTask.class);
    when(downloadTaskFactory.getObject()).thenReturn(task);
    when(taskService.submitTask(task)).thenReturn(task);
    when(task.getFuture()).thenReturn(CompletableFuture.completedFuture(null));

    assertTrue(instance.ensureWrapperAvailable(RenderingBackend.VULKAN_DXVK));
    verify(taskService).submitTask(task);
    verify(task).setBackend(RenderingBackend.VULKAN_DXVK);
    verify(task).setVersion("v2.5.3");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEnsureWrapperAvailableReturnsFalseOnFailure() {
    RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
    RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
    ResponseSpec responseSpec = mock(ResponseSpec.class);

    when(defaultWebClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.accept(any())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(GitHubRelease.class)).thenReturn(Mono.error(new RuntimeException("Network error")));

    assertFalse(instance.ensureWrapperAvailable(RenderingBackend.DIRECTX_12));
    verify(notificationService).addImmediateErrorNotification(any(), any(), any());
  }
}
