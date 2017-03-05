package com.faforever.client.update;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.google.common.io.CharStreams;
import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;

  //TODO: switch to constructor injection, superclass CompletableTask<UpdateInfo> has no default constructor
  @Inject
  ClientProperties clientProperties;
  @Inject
  I18n i18n;

  private ComparableVersion currentVersion;

  public CheckForUpdateTask() {
    super(Priority.LOW);
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @Override
  protected UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    logger.info("Checking for client update");

    String releasesUrl = clientProperties.getGitHub().getReleasesUrl();
    int connectionTimeout = clientProperties.getGitHub().getTimeout();

    URL url = new URL(releasesUrl);
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.setConnectTimeout(connectionTimeout);

    try (Reader reader = new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)) {
      Type type = new TypeToken<List<GitHubRelease>>() {
      }.getType();

      StringBuilder to = new StringBuilder();
      CharStreams.copy(reader, to);

      List<GitHubRelease> releases = gson.fromJson(to.toString(), type);
      GitHubRelease gitHubRelease = releases.get(0);

      // Strip the "v" prefix
      String strippedVersion = gitHubRelease.getName().substring(1);
      ComparableVersion latestVersion = new ComparableVersion(strippedVersion);

      logger.info("Current version is {}, newest version is {}", currentVersion, gitHubRelease.getName());

      if (!(latestVersion.compareTo(currentVersion) > 0)) {
        return null;
      }

      GitHubAsset gitHubAsset = gitHubRelease.getAssets()[0];
      return new UpdateInfo(
          gitHubRelease.getName(),
          gitHubAsset.getName(),
          gitHubAsset.getBrowserDownloadUrl(),
          gitHubAsset.getSize(),
          gitHubRelease.getHtmlUrl());
    }
  }

  public void setCurrentVersion(ComparableVersion currentVersion) {
    this.currentVersion = currentVersion;
  }
}
