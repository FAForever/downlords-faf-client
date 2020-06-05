package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.sun.jna.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckForBetaUpdateTaskTest extends AbstractPlainJavaFxTest {

  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private RestTemplateBuilder restTemplateBuilder;
  @Mock
  private RestTemplate restTemplate;

  private CheckForBetaUpdateTask instance;

  @Before
  public void setUp() throws Exception {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    instance = new CheckForBetaUpdateTask(i18n, preferencesService, restTemplateBuilder);

    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.getGitHubRepo().setApiUrl("http://localhost");
    when(preferencesService.getRemotePreferences()).thenReturn(clientConfiguration);
  }

  @Test
  public void call_noPreRelease() throws Exception {
    GitHubRelease gitHubRelease = new GitHubRelease().setPreRelease(false);

    assertThat(call(gitHubRelease).isEmpty(), is(true));
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

  @Test
  public void call_isPreRelease() throws Exception {
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

  @Test
  public void call_noReleases() throws Exception {
    mockApiResponse(new ResponseEntity<>(null, HttpStatus.OK));

    Optional<UpdateInfo> updateInfo = instance.call();

    assertThat(updateInfo.isEmpty(), is(true));
  }

  @Test
  public void call_unsuccessful() throws Exception {
    mockApiResponse(new ResponseEntity<>(null, HttpStatus.FORBIDDEN));

    Optional<UpdateInfo> updateInfo = instance.call();

    assertThat(updateInfo.isEmpty(), is(true));
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