package com.faforever.client.preferences;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.URI;

public class MirrorPrefs {
  private final ObservableList<URI> mirrorURLs;

  public MirrorPrefs() {
    mirrorURLs = FXCollections.observableArrayList();
  }

  public ObservableList<URI> getMirrorURLs() {
    return mirrorURLs;
  }
}
