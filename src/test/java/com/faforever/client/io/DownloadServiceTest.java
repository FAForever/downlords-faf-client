package com.faforever.client.io;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.io.ByteCountListener;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadServiceTest extends ServiceTest {

  @Mock
  private PreferencesService preferencesService;

  private DownloadService instance;
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new DownloadService(preferencesService);
  }

  @Test
  public void testGetMirrorURL() throws Exception {
    URI uri = URI.create("https://mirror.example.com");

    assertEquals(
        instance.getMirrorURL(uri, new URL("http://content.faforever.com/foo/bar.exe")),
        Optional.of(new URL("https://mirror.example.com/foo/bar.exe"))
    );

    assertEquals(
        instance.getMirrorURL(uri, new URL("ftp://content.faforever.com/foo/bar.exe")),
        Optional.empty()
    );
  }

  @Test
  public void testMirrorUrlList() throws Exception {
    preferences.getMirror().getMirrorURLs().setAll(
        URI.create("https://mirror1.example.com"),
        URI.create("https://mirror2.example.com/"),
        URI.create("https://mirror3.example.com/subdirectory"),
        URI.create("https://mirror4.com"),
        URI.create("https://mirror5.example.com:8080"),
        URI.create("https://mirror6.example.com:8080/"),
        URI.create("https://user:pass@mirror7.example.com:8080/"),
        URI.create("http://mirror8.example.com/"),
        URI.create("ftp://mirror9.example.com")
    );

    Assertions.assertThat(instance.getMirrorsFor(new URL("http://content.faforever.com/foo/bar.exe"))).isEqualTo(
        List.of(
            URI.create("https://mirror1.example.com/foo/bar.exe").toURL(),
            URI.create("https://mirror2.example.com/foo/bar.exe").toURL(),
            URI.create("https://mirror3.example.com/subdirectory/foo/bar.exe").toURL(),
            URI.create("https://mirror4.com/foo/bar.exe").toURL(),
            URI.create("https://mirror5.example.com:8080/foo/bar.exe").toURL(),
            URI.create("https://mirror6.example.com:8080/foo/bar.exe").toURL(),
            URI.create("https://user:pass@mirror7.example.com:8080/foo/bar.exe").toURL(),
            URI.create("http://mirror8.example.com/foo/bar.exe").toURL()
        )
    );
  }

  @Test
  public void testDownloadWithNoMirrors() throws Exception {
    DownloadService downloadServiceSpy = Mockito.spy(instance);

    URL url = new URL("https://example.com/foo");
    Path targetFile = Paths.get("/some/target");
    ByteCountListener progressListener = (processed, total) -> {};
    String md5sum = "asdf";
    downloadServiceSpy.downloadFileWithMirrors(url, targetFile, progressListener, md5sum);

    verify(downloadServiceSpy, times(1)).downloadFile(url, targetFile, progressListener, md5sum);
  }

  @Test
  public void testDownloadWithMirrors() throws Exception {
    preferences.getMirror().getMirrorURLs().setAll(
        URI.create("https://mirror1.example.com/"),
        URI.create("https://mirror2.example.com/"),
        URI.create("https://mirror3.com/foo")
    );

    DownloadService downloadServiceSpy = Mockito.spy(instance);
    doThrow(IOException.class).when(downloadServiceSpy).downloadFile(any(URL.class), any(Path.class), any(), anyString());
    doThrow(FileNotFoundException.class).when(downloadServiceSpy).downloadFile(any(URL.class), any(Path.class), any(), anyString());
    doThrow(ChecksumMismatchException.class).when(downloadServiceSpy).downloadFile(any(URL.class), any(Path.class), any(), anyString());

    URL url = new URL("https://example.com/bar");
    Path targetFile = Paths.get("/some/target");
    ByteCountListener progressListener = (processed, total) -> {};
    String md5sum = "asdf";
    downloadServiceSpy.downloadFileWithMirrors(url, targetFile, progressListener, md5sum);

    verify(downloadServiceSpy).downloadFile(new URL("https://mirror1.example.com/bar"), targetFile, progressListener, md5sum);
    verify(downloadServiceSpy).downloadFile(new URL("https://mirror2.example.com/bar"), targetFile, progressListener, md5sum);
    verify(downloadServiceSpy).downloadFile(new URL("https://mirror1.example.com/foo/bar"), targetFile, progressListener, md5sum);
    verify(downloadServiceSpy).downloadFile(new URL("https://example.com/bar"), targetFile, progressListener, md5sum);
  }

  @Test
  public void testDownloadFile() throws Exception {
    URL url = mock(URL.class);
    URLConnection urlConnection = mock(URLConnection.class);

    byte[] DATA = "some content".getBytes();
    when(url.openStream()).thenReturn(new ByteArrayInputStream(DATA));
    when(urlConnection.getContentLength()).thenReturn(DATA.length);
    when(url.openConnection()).thenReturn(urlConnection);

    Path temp = Files.createTempFile("download", ".dat");
    instance.downloadFile(url, temp, (processed, total) -> {}, "00000000000000000000000000000000");

    byte[] data = Files.readAllBytes(temp);
    assertEquals(data, DATA);
  }

  @Test
  public void testDownloadFileBadChecksum() throws Exception {
    URL url = mock(URL.class);
    URLConnection urlConnection = mock(URLConnection.class);

    byte[] DATA = "some content".getBytes();
    when(url.openStream()).thenReturn(new ByteArrayInputStream(DATA));
    when(urlConnection.getContentLength()).thenReturn(DATA.length);
    when(url.openConnection()).thenReturn(urlConnection);

    Path temp = Files.createTempFile("download", ".dat");
    assertThrows(ChecksumMismatchException.class, () -> instance.downloadFile(url, temp, (processed, total) -> {
    }, "00000000000000000000000000000000"));
  }
}
