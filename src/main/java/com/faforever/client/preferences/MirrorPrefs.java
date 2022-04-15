package com.faforever.client.preferences;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.net.URI;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class MirrorPrefs {
  ObservableList<URI> mirrorURLs;

  public MirrorPrefs() {
    mirrorURLs = FXCollections.observableArrayList();
  }

  public ObservableList<URI> getMirrorURLs() {
    return mirrorURLs;
  }
}
