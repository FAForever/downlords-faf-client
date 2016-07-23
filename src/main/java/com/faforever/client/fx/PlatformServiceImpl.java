package com.faforever.client.fx;

import javafx.application.HostServices;

import java.nio.file.Path;

import static com.github.nocatch.NoCatch.noCatch;
import static org.bridj.Platform.show;

public class PlatformServiceImpl implements PlatformService {

  private final HostServices hostServices;

  public PlatformServiceImpl(HostServices hostServices) {
    this.hostServices = hostServices;
  }

  /**
   * Opens the specified URI in a new browser window or tab.
   */
  @Override
  public void showDocument(String url) {
    hostServices.showDocument(url);
  }

  /**
   * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
   */
  @Override
  public void reveal(Path path) {
    noCatch(() -> show(path.toFile()));
  }
}
