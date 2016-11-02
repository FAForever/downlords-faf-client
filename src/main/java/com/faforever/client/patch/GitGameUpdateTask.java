package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.google.common.base.Joiner;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.setAttribute;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class GitGameUpdateTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ClassPathResource INIT_TEMPLATE = new ClassPathResource("/fa/init_template.lua");
  private static final long TIMEOUT = 30;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

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
  private String version;
  private Set<String> simMods;

  public GitGameUpdateTask() {
    super(Priority.MEDIUM);
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("patchTask.title"));
  }

  @Override
  protected Void call() throws Exception {
    logger.info("Updating game files to version: {}", Objects.toString(version, "latest"));

    // FIXME derive from repository URI
    Path gameRepositoryDirectory = preferencesService.getFafReposDirectory().resolve("faf");

    copyGameFilesToFafBinDirectory();
    generateInitFile(gameRepositoryDirectory);

    String ref;
    if (version != null) {
      ref = "refs/tags/" + version;
    } else {
      ref = "refs/heads/master";
    }

    checkout(gameRepositoryDirectory, gameRepositoryUri, ref);

    logger.info("Downloading missing sim mods");
    downloadMissingSimMods();
    return null;
  }

  private void checkout(Path gitRepoDir, String gitRepoUri, String ref) throws IOException {
    if (Files.notExists(gitRepoDir)) {
      Files.createDirectories(gitRepoDir.getParent());
      gitWrapper.clone(gitRepoUri, gitRepoDir);
    }

    gitWrapper.clean(gitRepoDir);
    gitWrapper.reset(gitRepoDir);
    gitWrapper.fetch(gitRepoDir);
    gitWrapper.checkoutTag(gitRepoDir, ref);
  }

  private void generateInitFile(Path gameRepositoryDirectory) {
    Path initFile = preferencesService.getFafBinDirectory().resolve("init.lua");
    String faPath = preferencesService.getPreferences().getForgedAlliance().getPath().toAbsolutePath().toString();
    List<String> mountPaths = Collections.singletonList(gameRepositoryDirectory.toAbsolutePath().toString());

    logger.debug("Generating init file at {}", initFile);

    noCatch(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(INIT_TEMPLATE.getInputStream()));
           BufferedWriter writer = Files.newBufferedWriter(initFile, UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.replace("{{fa_path}}", faPath);
          writer.write(line.replace("{{mount_dirs}}", Joiner.on(',').join(mountPaths) + "\r\n"));
        }
      }
    });
  }

  protected void copyGameFilesToFafBinDirectory() throws IOException {
    logger.debug("Copying game files from FA to FAF folder");

    Path fafBinDirectory = preferencesService.getFafBinDirectory();
    createDirectories(fafBinDirectory);

    Path faBinPath = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("bin");

    Files.list(faBinPath)
        .forEach(source -> {
          Path destination = fafBinDirectory.resolve(source.getFileName());

          if (Files.exists(destination)) {
            return;
          }

          logger.debug("Copying file '{}' to '{}'", source, destination);
          noCatch(() -> createDirectories(destination.getParent()));
          noCatch(() -> copy(source, destination, REPLACE_EXISTING));

          if (OperatingSystem.current() == OperatingSystem.WINDOWS) {
            noCatch(() -> setAttribute(destination, "dos:readonly", false));
          }
        });
  }

  public void setVersion(String version) {
    this.version = version;
  }

  private void downloadMissingSimMods() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Set<String> uidsOfRequiredSimMods = simMods;
    if (uidsOfRequiredSimMods.isEmpty()) {
      return;
    }

    Set<String> uidsOfInstalledMods = modService.getInstalledModUids();
    uidsOfRequiredSimMods.stream()
        .filter(uid -> !uidsOfInstalledMods.contains(uid))
        .collect(Collectors.toSet())
        .forEach(uid -> modService.downloadAndInstallMod(uid));
  }

  public void setGameRepositoryUri(String gameRepositoryUri) {
    this.gameRepositoryUri = gameRepositoryUri;
  }

  public void setSimMods(Set<String> simMods) {
    this.simMods = simMods;
  }
}
