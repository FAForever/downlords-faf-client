package com.faforever.client.update;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.google.common.io.CharStreams;
import com.sun.jna.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
public class CheckForReleaseUpdateTaskTest extends AbstractPlainJavaFxTest {

  private CheckForReleaseUpdateTask instance;

  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private RestTemplateBuilder restTemplateBuilder;
  @Mock
  private RestTemplate restTemplate;
  private ClientConfiguration clientConfiguration;

  @Before
  public void setUp() throws Exception {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    instance = new CheckForReleaseUpdateTask(i18n, preferencesService, restTemplateBuilder);

    clientConfiguration = new ClientConfiguration();
    clientConfiguration.getGitHubRepo().setApiUrl("http://localhost");
    when(preferencesService.getRemotePreferences()).thenReturn(clientConfiguration);
  }

  /**
   * There is no newer version on the server.
   */
  @Test
  public void testGetUpdateIsCurrent() throws Exception {
    clientConfiguration.setLatestRelease(new ReleaseInfo(
        new ComparableVersion("2.0.0"),
        new ComparableVersion("2.0.0"),
        getClass().getResource("/update4j.xml"),
        null,
        null,
        null,
        false,
        "",
        null
    ));

    GitHubRelease gitHubRelease = new GitHubRelease()
        .setTagName("v2.0.0")
        .setPreRelease(true)
        .setAssets(Arrays.asList(
            new GitHubAsset().setName("foo.jar"),
            new GitHubAsset()
                .setName(getUpdate4jXmlName())
                .setBrowserDownloadUrl(getClass().getResource("/update4j.xml")),
            new GitHubAsset().setName("something.xml")
        ));


    assertThat(call(gitHubRelease).isPresent(), is(true));
  }

  private String getUpdate4jXmlName() {
    if (Platform.isWindows()) {
      return "update4j-win.xml";
    }
    if (Platform.isLinux()) {
      return "update4j-unix.xml";
    }
    return "update4j-mac.xml";
  }

  private Optional<UpdateInfo> call(GitHubRelease gitHubRelease) throws Exception {
    ResponseEntity<List<GitHubRelease>> response = new ResponseEntity<>(Collections.singletonList(gitHubRelease), HttpStatus.OK);
    mockApiResponse(response);

    return instance.call();
  }

  private void mockApiResponse(ResponseEntity<List<GitHubRelease>> response) {
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
        .thenReturn(response);
  }
}
