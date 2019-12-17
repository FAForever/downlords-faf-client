package com.faforever.client.game;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.mod.MountInfo;
import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates the Forged Alliance init file that is required to mount all necessary files and directories.
 */

@Lazy
@Component
@RequiredArgsConstructor
public class FaInitGenerator {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ClassPathResource INIT_TEMPLATE = new ClassPathResource("/fa/init_template.lua");

  private final PreferencesService preferencesService;

  /**
   * Generates the Forged Alliance init file.
   *
   * @return the path of the generated init file.
   */
  public Path generateInitFile(List<MountInfo> mountInfos, Set<String> hookDirectories) {
    Path initFile = preferencesService.getFafBinDirectory().resolve(ForgedAlliancePrefs.INIT_FILE_NAME);
    String faPath = preferencesService.getPreferences().getForgedAlliance().getInstallationPath().toAbsolutePath().toString().replaceAll("[/\\\\]", "\\\\\\\\");

    logger.debug("Generating init file at {}", initFile);

    List<String> mountPointStrings = mountInfos.stream()
        .map(this::toMountPointFormat)
        .collect(Collectors.toList());
    CharSequence mountPointsLuaTable = Joiner.on(",\r\n    ").join(mountPointStrings);

    List<String> hookStrings = hookDirectories.stream()
        .sorted()
        .map(s -> "'" + s + "'")
        .collect(Collectors.toList());
    CharSequence hooksLuaTable = Joiner.on(",\r\n    ").join(hookStrings);

    noCatch(() -> {
      Files.createDirectories(initFile.getParent());
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(INIT_TEMPLATE.getInputStream()));
           BufferedWriter writer = Files.newBufferedWriter(initFile, UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.replace("((fa_path))", faPath);
          writer.write(line.replace("--[[ ${mountSpecsTable} --]]", mountPointsLuaTable)
              .replace("--[[ ${hooksTable} --]]", hooksLuaTable) + "\r\n");
        }
      }
    });
    return initFile;
  }

  private String toMountPointFormat(MountInfo mountInfo) {
    String source = Optional.ofNullable(mountInfo.getBaseDir())
        .map(path -> path.resolve(mountInfo.getFile()).toAbsolutePath().toString())
        .orElse(mountInfo.getFile().toAbsolutePath().toString())
        .replaceAll("[/\\\\]", "\\\\\\\\");

    return String.format("{'%s', '%s'}", source, mountInfo.getMountPoint());
  }
}
