package com.faforever.client.io;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.io.ByteCountListener;
import javafx.collections.FXCollections;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadServiceTest extends ServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PreferencesService preferencesService;

  private DownloadService instance;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

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
    when(preferencesService.getPreferences().getMirror().getMirrorURLs()).thenReturn(
        FXCollections.observableArrayList(Arrays.asList(
            URI.create("https://mirror1.example.com"),
            URI.create("https://mirror2.example.com/"),
            URI.create("https://mirror3.example.com/subdirectory"),
            URI.create("https://mirror4.com"),
            URI.create("https://mirror5.example.com:8080"),
            URI.create("https://mirror6.example.com:8080/"),
            URI.create("https://user:pass@mirror7.example.com:8080/"),
            URI.create("http://mirror8.example.com/"),
            URI.create("ftp://mirror9.example.com")
        )
    ));

    Assertions.assertThat(instance.getMirrorsFor(new URL("http://content.faforever.com/foo/bar.exe"))).isEqualTo(
        Arrays.asList(
            URI.create("https://mirror1.example.com/foo/bar.exe"),
            URI.create("https://mirror2.example.com/foo/bar.exe"),
            URI.create("https://mirror3.example.com/subdirectory/foo/bar.exe"),
            URI.create("https://mirror4.com/foo/bar.exe"),
            URI.create("https://mirror5.example.com:8080/foo/bar.exe"),
            URI.create("https://mirror6.example.com:8080/foo/bar.exe"),
            URI.create("https://user:pass@mirror7.example.com:8080/foo/bar.exe"),
            URI.create("http://mirror8.example.com/foo/bar.exe")
        )
    );
  }

  @Test
  public void testDownloadWithNoMirrors() throws Exception {
    when(preferencesService.getPreferences().getMirror().getMirrorURLs()).thenReturn(FXCollections.observableArrayList());

    DownloadService downloadServiceSpy = Mockito.spy(instance);

    URL url = new URL("https://example.com/foo");
    Path targetFile = Paths.get("/some/target");
    ByteCountListener progressListener = (a, b) -> {return;};
    String md5sum = "asdf";
    downloadServiceSpy.downloadFileWithMirrors(url, targetFile, progressListener, md5sum);

    verify(downloadServiceSpy, times(1)).downloadFile(url, targetFile, progressListener, md5sum);
  }

  @Test
  public void testDownloadWithMirrors() throws Exception {
    when(preferencesService.getPreferences().getMirror().getMirrorURLs()).thenReturn(FXCollections.observableArrayList(
        Arrays.asList(
            URI.create("https://mirror1.example.com/"),
            URI.create("https://mirror2.example.com/"),
            URI.create("https://mirror3.com/foo")
        )
    ));

    DownloadService downloadServiceSpy = Mockito.spy(instance);
    doThrow(IOException.class).when(downloadServiceSpy).downloadFile(any(URL.class), any(Path.class), any(), anyString());
    doThrow(FileNotFoundException.class).when(downloadServiceSpy).downloadFile(any(URL.class), any(Path.class), any(), anyString());
    doThrow(ChecksumMismatchException.class).when(downloadServiceSpy).downloadFile(any(URL.class), any(Path.class), any(), anyString());

    URL url = new URL("https://example.com/bar");
    Path targetFile = Paths.get("/some/target");
    ByteCountListener progressListener = (a, b) -> {return;};
    String md5sum = "asdf";
    downloadServiceSpy.downloadFileWithMirrors(url, targetFile, progressListener, md5sum);

    verify(downloadServiceSpy).downloadFile(new URL("https://mirror1.example.com/bar"), targetFile, progressListener, md5sum);
    verify(downloadServiceSpy).downloadFile(new URL("https://mirror2.example.com/bar"), targetFile, progressListener, md5sum);
    verify(downloadServiceSpy).downloadFile(new URL("https://mirror1.example.com/foo/bar"), targetFile, progressListener, md5sum);
    verify(downloadServiceSpy).downloadFile(new URL("https://example.com/bar"), targetFile, progressListener, md5sum);
  }
}
