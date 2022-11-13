package com.faforever.client.os;

import com.faforever.client.FafClientApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class OsConfiguration {

  @Profile(FafClientApplication.PROFILE_WINDOWS)
  @Bean
  public OperatingSystem windows() {
    return new OsWindows();
  }

  @Profile(FafClientApplication.PROFILE_LINUX)
  @Bean
  public OperatingSystem linux() {
    return new OsPosix();
  }

  @Profile(FafClientApplication.PROFILE_MAC)
  @Bean
  public OperatingSystem macos() {
    return new OsPosix();
  }

  @Bean
  @ConditionalOnMissingBean
  public OperatingSystem runtimeDetection() {
    if (org.bridj.Platform.isWindows()) {
      return new OsWindows();
    } else if (org.bridj.Platform.isLinux()) {
      return new OsPosix();
    } else if (org.bridj.Platform.isMacOSX()) {
      return new OsPosix();
    } else {
      log.warn("Detected unsupported operating system. Feature may not work. Use on your own risk.");
      return new OsUnknown();
    }
  }
}
