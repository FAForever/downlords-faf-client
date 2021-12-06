package com.faforever.client.patch;

import com.faforever.client.io.DownloadService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import javafx.collections.FXCollections;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        URI.create("https://mirror3.com"),
        URI.create("https://mirror4.example.com:8080"),
        URI.create("https://mirror5.example.com:8080/"),
        URI.create("https://user:pass@mirror6.example.com:8080/"),
        URI.create("http://mirror7.example.com/"),
        URI.create("ftp://mirror8.example.com")
      )
    ));

    Assertions.assertThat(instance.getMirrorsFor(new URL("http://content.faforever.com/foo/bar.exe"))).isEqualTo(
        Arrays.asList(
            URI.create("https://mirror1.example.com/foo/bar.exe"),
            URI.create("https://mirror2.example.com/foo/bar.exe"),
            URI.create("https://mirror3.com/foo/bar.exe"),
            URI.create("https://mirror4.example.com:8080/foo/bar.exe"),
            URI.create("https://mirror5.example.com:8080/foo/bar.exe"),
            URI.create("https://user:pass@mirror6.example.com:8080/foo/bar.exe"),
            URI.create("http://mirror7.example.com/foo/bar.exe")
        )
    );
  }
}
