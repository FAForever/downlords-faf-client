package com.faforever.client.game;

import com.faforever.client.patch.MountPoint;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates the Forged Alliance init file that is required to mount all necessary files and directories.
 */
public class FaInitGenerator {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ClassPathResource INIT_TEMPLATE = new ClassPathResource("/fa/init_template.lua");

  @Resource
  PreferencesService preferencesService;

  /**
   * Generates the Forged Alliance init file.
   *
   * @return the path of the generated init file.
   */
  public Path generateInitFile(List<MountPoint> mountPoints) {
    Path initFile = preferencesService.getFafBinDirectory().resolve(ForgedAlliancePrefs.INIT_FILE_NAME);
    String faPath = preferencesService.getPreferences().getForgedAlliance().getPath().toAbsolutePath().toString().replaceAll("[/\\\\]", "\\\\\\\\");

    logger.debug("Generating init file at {}", initFile);

    List<String> mountPointStrings = mountPoints.stream()
        .map(mountPoint -> String.format("['%s'] = '%s'", mountPoint.getMountPath(), mountPoint.getDirectory().toString().replaceAll("[/\\\\]", "\\\\\\\\")))
        .collect(Collectors.toList());

    CharSequence mountPointsLuaTable = Joiner.on(",\r\n    ").join(mountPointStrings);

    noCatch(() -> {
      Files.createDirectories(initFile.getParent());
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(INIT_TEMPLATE.getInputStream()));
           BufferedWriter writer = Files.newBufferedWriter(initFile, UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.replace("((fa_path))", faPath);
          writer.write(line.replace("--[[ ${mountpointsTable} --]]", mountPointsLuaTable) + "\r\n");
        }
      }
    });
    return initFile;
  }
}
