package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.Assert;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;

public class GitFeaturedModsUpdateTask extends CompletableTask<ComparableVersion> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ClassPathResource INIT_TEMPLATE = new ClassPathResource("/fa/init_template.lua");

  @Resource
  I18n i18n;
  @Resource
  PreferencesService preferencesService;
  @Resource
  GitWrapper gitWrapper;
  @Resource
  Environment environment;
  @Resource
  ModService modService;

  private String gameRepositoryUri;
  private Set<String> simMods;
  private String ref;
  private Path repositoryDirectory;

  public GitFeaturedModsUpdateTask() {
    super(Priority.MEDIUM);
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("patchTask.title"));
  }

  @Override
  protected ComparableVersion call() throws Exception {
    logger.info("Updating game files from {}@{}", gameRepositoryUri, ref);

    generateInitFile(repositoryDirectory);

    checkout(repositoryDirectory, gameRepositoryUri, ref);

    logger.info("Downloading missing sim mods");
    downloadMissingSimMods();

    return modService.readModVersion(repositoryDirectory);
  }

  private void checkout(Path gitRepoDir, String gitRepoUrl, String ref) throws IOException {
    Assert.checkNullIllegalState(gitRepoDir, "Parameter 'gitRepoDir' must not be null");
    Assert.checkNullIllegalState(gitRepoUrl, "Parameter 'gitRepoUrl' must not be null");
    Assert.checkNullIllegalState(ref, "Parameter 'ref' must not be null");


    if (Files.notExists(gitRepoDir)) {
      Files.createDirectories(gitRepoDir.getParent());
      gitWrapper.clone(gitRepoUrl, gitRepoDir);
    } else {
      gitWrapper.fetch(gitRepoDir);
    }
    gitWrapper.checkoutRef(gitRepoDir, ref);
  }

  private void generateInitFile(Path gameRepositoryDirectory) {
    Path initFile = preferencesService.getFafBinDirectory().resolve(ForgedAlliancePrefs.INIT_FILE_NAME);
    String faPath = preferencesService.getPreferences().getForgedAlliance().getPath().toAbsolutePath().toString().replaceAll("[/\\\\]", "\\\\\\\\");
    String mountPath = gameRepositoryDirectory.toAbsolutePath().toString().replaceAll("[/\\\\]", "\\\\\\\\");

    logger.debug("Generating init file at {}", initFile);

    noCatch(() -> {
      Files.createDirectories(initFile.getParent());
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(INIT_TEMPLATE.getInputStream()));
           BufferedWriter writer = Files.newBufferedWriter(initFile, UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.replace("((fa_path))", faPath);
          writer.write(line.replace("((mount_dirs))", String.format("'%s'", mountPath)) + "\r\n");
        }
      }
    });
  }

  private void downloadMissingSimMods() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    if (simMods == null || simMods.isEmpty()) {
      return;
    }

    Set<String> uidsOfInstalledMods = modService.getInstalledModUids();
    simMods.stream()
        .filter(uid -> !uidsOfInstalledMods.contains(uid))
        .collect(Collectors.toSet())
        .forEach(uid -> modService.downloadAndInstallMod(uid));
  }

  public void setGameRepositoryUrl(String gameRepositoryUri) {
    this.gameRepositoryUri = gameRepositoryUri;
  }

  public void setSimMods(Set<String> simMods) {
    this.simMods = simMods;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public void setRepositoryDirectory(Path repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
  }
}
