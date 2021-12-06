package com.faforever.client.preferences;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.URI;

public class MirrorPrefs {
  private final ListProperty<URI> mirrorURLs;

  public MirrorPrefs() {
    mirrorURLs = new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  public ObservableList<URI> getMirrorURLs() {
    return mirrorURLs.get();
  }
}
