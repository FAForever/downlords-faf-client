package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ChatPrefs {

  private final DoubleProperty zoom;
  private final BooleanProperty learnedAutoComplete;
  private final BooleanProperty previewImageUrls;
  private final IntegerProperty maxMessages;

  public ChatPrefs() {
    maxMessages = new SimpleIntegerProperty(500);
    zoom = new SimpleDoubleProperty(1);
    learnedAutoComplete = new SimpleBooleanProperty(false);
    previewImageUrls = new SimpleBooleanProperty(true);
  }

  public boolean getPreviewImageUrls() {
    return previewImageUrls.get();
  }

  public void setPreviewImageUrls(boolean previewImageUrls) {
    this.previewImageUrls.set(previewImageUrls);
  }

  public BooleanProperty previewImageUrlsProperty() {
    return previewImageUrls;
  }

  public Double getZoom() {
    return zoom.getValue();
  }

  public void setZoom(Double zoom) {
    this.zoom.set(zoom);
  }

  public DoubleProperty zoomProperty() {
    return zoom;
  }

  public boolean getLearnedAutoComplete() {
    return learnedAutoComplete.get();
  }

  public void setLearnedAutoComplete(boolean learnedAutoComplete) {
    this.learnedAutoComplete.set(learnedAutoComplete);
  }

  public BooleanProperty learnedAutoCompleteProperty() {
    return learnedAutoComplete;
  }

  public int getMaxMessages() {
    return maxMessages.get();
  }

  public void setMaxMessages(int maxMessages) {
    this.maxMessages.set(maxMessages);
  }

  public IntegerProperty maxMessagesProperty() {
    return maxMessages;
  }
}
