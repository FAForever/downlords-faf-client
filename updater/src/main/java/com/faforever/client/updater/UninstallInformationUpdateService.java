package com.faforever.client.updater;

import com.install4j.api.windows.RegistryRoot;
import com.install4j.api.windows.WinRegistry;
import com.install4j.runtime.installer.config.InstallerConfig;
import com.install4j.runtime.installer.helper.InstallerUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.update4j.Configuration;
import org.update4j.FileMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On Windows, the uninstallation information (including application version and size) are stored under:
 * `HKEY_CURRENT_USER\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\<application-id>`. Every time the client gets
 * updated, this information should be updated as well. That's what this class does.
 */
@Slf4j
public class UninstallInformationUpdateService {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");
  private static final RegistryRoot ROOT = RegistryRoot.HKEY_CURRENT_USER;

  private UninstallInformationUpdateService() {

  }

  @SneakyThrows
  public static void update(Configuration configuration) {
    Path installationDirectory = configuration.getBasePath();

    String applicationId = InstallerUtil.getOldApplicationId(installationDirectory.toFile());
    if (applicationId == null) {
      log.info("Can't update uninstall information since there is no Install4J metadata in: {}", installationDirectory);
      return;
    }

    UninstallInfo newUninstallInfo = createNewUninstallInfo(configuration);
    writeUninstallInfo(newUninstallInfo, applicationId);
  }

  private static UninstallInfo createNewUninstallInfo(Configuration configuration) {
    InstallerConfig installerConfig = InstallerUtil.getOldApplicationConfig(configuration.getBasePath().toFile());

    String version = installerConfig.getApplicationVersion();
    Matcher versionMatcher = VERSION_PATTERN.matcher(version);

    int[] majorMinor = parseVersion(version, versionMatcher);

    return new UninstallInfo(
      getClientExePath(configuration).map(path -> path.toAbsolutePath().toString()).orElse(null),
      installerConfig.getApplicationNameWithVersion(),
      version,
      getInstallationSize(configuration),
      configuration.getBasePath().toAbsolutePath().toString(),
      installerConfig.getPublisherName(),
      installerConfig.getUninstallerPath(),
      installerConfig.getPublisherURL(),
      majorMinor[0],
      majorMinor[1]
    );
  }

  private static Optional<Path> getClientExePath(Configuration configuration) {
    return configuration.getFiles().stream()
      .filter(file -> file.getPath().getParent().equals(configuration.getBasePath()))
      .filter(file -> file.getPath().toString().endsWith(".exe"))
      .findFirst()
      .map(FileMetadata::getPath);
  }

  private static int[] parseVersion(String version, Matcher versionMatcher) {
    int majorVersion;
    int minorVersion;
    if (versionMatcher.find()) {
      majorVersion = Integer.parseInt(versionMatcher.group(1));
      minorVersion = Integer.parseInt(versionMatcher.group(2));
    } else {
      log.warn("Version '{}' did not match pattern: {}", version, versionMatcher.pattern());
      majorVersion = 0;
      minorVersion = 0;
    }
    return new int[]{majorVersion, minorVersion};
  }

  private static int getInstallationSize(Configuration configuration) {
    return configuration.getFiles().stream()
      .mapToLong(FileMetadata::getSize)
      .mapToInt(value -> (int) value / 1024)
      .sum();
  }

  private static void writeUninstallInfo(UninstallInfo info, String applicationId) {
    Arrays.stream(info.getClass().getDeclaredFields())
      .filter(field -> field.isAnnotationPresent(Name.class))
      .filter(field -> getValue(info, field) != null)
      .forEach(field -> {
        field.setAccessible(true);
        setUninstallValue(field.getAnnotation(Name.class).value(), getValue(info, field), applicationId);
      });
  }

  private static void setUninstallValue(String name, Object value, String applicationId) {
    WinRegistry.setValue(ROOT,
      "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + applicationId,
      name, value
    );
  }

  @SneakyThrows
  private static Object getValue(Object object, Field field) {
    return field.get(object);
  }

  @AllArgsConstructor
  @NoArgsConstructor
  private static class UninstallInfo {
    @Name("DisplayIcon")
    String displayIcon;
    @Name("DisplayName")
    String displayName;
    @Name("DisplayVersion")
    String displayVersion;
    @Name("EstimatedSize")
    Integer estimatedSize;
    @Name("InstallLocation")
    String installLocation;
    @Name("Publisher")
    String publisher;
    @Name("UninstallString")
    String uninstallString;
    @Name("URLInfoAbout")
    String urlInfoAbout;
    @Name("VersionMajor")
    Integer versionMajor;
    @Name("VersionMinor")
    Integer versionMinor;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.PARAMETER})
  @interface Name {
    /** Name of the registry value. */
    String value();
  }
}
