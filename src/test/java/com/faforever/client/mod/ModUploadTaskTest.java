package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.faforever.client.mod.ModUploadTask.MOD_UPLOAD_COMPLETE_API_POST;
import static com.faforever.client.mod.ModUploadTask.MOD_UPLOAD_START_API_GET;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModUploadTaskTest extends PlatformTest {

  @TempDir
  Path tempDirectory;

  @Mock
  FafApiAccessor fafApiAccessor;

  @Mock
  WebClient defaultWebClient;

  @Mock
  I18n i18n;

  @Spy
  DataPrefs dataPrefs;

  @Captor
  ArgumentCaptor<ModUploadMetadata> metadataCaptor;

  ModUploadTask underTest;

  UUID requestId;
  URI signedUri;

  @Mock
  WebClient.RequestBodyUriSpec requestBodySpec;
  @Mock
  WebClient.RequestHeadersSpec<?> headersSpec;
  @Mock
  WebClient.ResponseSpec responseSpec;

  @BeforeEach
  void setUp() throws Exception {
    underTest = new ModUploadTask(fafApiAccessor, i18n, dataPrefs, defaultWebClient);

    dataPrefs.setBaseDataDirectory(tempDirectory);
    Files.createDirectories(dataPrefs.getCacheDirectory());

    lenient().when(i18n.get(any())).thenReturn("");

    requestId = UUID.randomUUID();
    signedUri = new URI("https://example.com/upload");
  }

  @Test
  void testModPathNull() {
    assertThrows(NullPointerException.class, () -> underTest.call());
  }

  @Test
  void testProgressListenerNull() {
    underTest.setModPath(Path.of("."));
    assertThrows(NullPointerException.class, () -> underTest.call());
  }

  @Test
  void testCall() throws Exception {
    stubWebClient();

    when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    when(fafApiAccessor.postJson(eq(MOD_UPLOAD_COMPLETE_API_POST), metadataCaptor.capture())).thenReturn(Mono.empty());

    Path modFolder = Files.createDirectory(tempDirectory.resolve("my-mod"));
    Files.writeString(modFolder.resolve("hello.txt"), "world");

    underTest.setModPath(modFolder);
    underTest.call();

    verify(fafApiAccessor).getApiObject(MOD_UPLOAD_START_API_GET, UploadUrlResponse.class);
    verify(defaultWebClient).put();
    verify(fafApiAccessor).postJson(eq(MOD_UPLOAD_COMPLETE_API_POST), metadataCaptor.capture());

    assert metadataCaptor.getValue().requestId().equals(requestId);
    assert Files.list(dataPrefs.getCacheDirectory()).toList().isEmpty();
  }

  @Test
  void testCallUploadFails() throws Exception {
    stubWebClient();

    when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.error(new IllegalStateException("Simulated S3 failure")));

    Path modFolder = Files.createDirectory(tempDirectory.resolve("my-mod"));
    Files.writeString(modFolder.resolve("hello.txt"), "world");

    underTest.setModPath(modFolder);

    assertThrows(IllegalStateException.class, () -> underTest.call());
  }

  void stubWebClient() {
    when(fafApiAccessor.getApiObject(MOD_UPLOAD_START_API_GET, UploadUrlResponse.class)).thenReturn(
        Mono.just(new UploadUrlResponse(signedUri, requestId)));

    when(defaultWebClient.put()).thenReturn(requestBodySpec);
    when(requestBodySpec.uri(signedUri)).thenReturn(requestBodySpec);
    when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(MediaType.valueOf("application/zip"))).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(BodyInserter.class))).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
  }
}
