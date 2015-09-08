package com.faforever.client.fx;

import javafx.application.HostServices;

public class JavaFxHostService implements HostService {

  private final HostServices hostServices;

  public JavaFxHostService(HostServices hostServices) {
    this.hostServices = hostServices;
  }

  @Override
  public void showDocument(String url) {
    hostServices.showDocument(url);
  }
}
