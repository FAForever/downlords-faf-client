package com.faforever.client.preferences;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatFormat;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;

public class ChatPrefs {

  private final DoubleProperty zoom;
  private final BooleanProperty learnedAutoComplete;
  private final BooleanProperty previewImageUrls;
  private final IntegerProperty maxMessages;
  private final ObjectProperty<ChatColorMode> chatColorMode;
  private final IntegerProperty channelTabScrollPaneWidth;
  private final MapProperty<String, Color> userToColor;
  private final BooleanProperty hideFoeMessages;
  private final ObjectProperty<TimeInfo> timeFormat;
  private final ObjectProperty<ChatFormat> chatFormat;

  /**
   * Time in minutes a player has to be inactive to be considered idle.
   */
  private final IntegerProperty idleThreshold;

  public ChatPrefs() {
    timeFormat = new SimpleObjectProperty<>(TimeInfo.AUTO);
    maxMessages = new SimpleIntegerProperty(500);
    zoom = new SimpleDoubleProperty(1);
    learnedAutoComplete = new SimpleBooleanProperty(false);
    previewImageUrls = new SimpleBooleanProperty(true);
    hideFoeMessages = new SimpleBooleanProperty(true);
    channelTabScrollPaneWidth = new SimpleIntegerProperty(250);
    userToColor = new SimpleMapProperty<>(FXCollections.observableHashMap());
    chatColorMode = new SimpleObjectProperty<>(CUSTOM);
    idleThreshold = new SimpleIntegerProperty(10);
    chatFormat = new SimpleObjectProperty<>(ChatFormat.EXTENDED);
  }

  public ChatColorMode getChatColorMode() {
    return chatColorMode.get();
  }

  public void setChatColorMode(ChatColorMode chatColorMode) {
    this.chatColorMode.set(chatColorMode);
  }

  public TimeInfo getTimeFormat() {
    return timeFormat.get();
  }

  public void setTimeFormat(TimeInfo time) {
    this.timeFormat.set(time);
  }

  public ChatFormat getChatFormat() {
    return this.chatFormat.get();
  }

  public void setChatFormat(ChatFormat chatFormat) {
    this.chatFormat.setValue(chatFormat);
  }

  public ObjectProperty<ChatFormat> chatFormatProperty() {
    return chatFormat;
  }

  public ObjectProperty<ChatColorMode> chatColorModeProperty() {
    return chatColorMode;
  }

  public ObservableMap<String, Color> getUserToColor() {
    return userToColor.get();
  }

  public void setUserToColor(ObservableMap<String, Color> userToColor) {
    this.userToColor.set(userToColor);
  }

  public MapProperty<String, Color> userToColorProperty() {
    return userToColor;
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

  public void setZoom(double zoom) {
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

  public int getChannelTabScrollPaneWidth() {
    return channelTabScrollPaneWidth.get();
  }

  public void setChannelTabScrollPaneWidth(int channelTabScrollPaneWidth) {
    this.channelTabScrollPaneWidth.set(channelTabScrollPaneWidth);
  }

  public IntegerProperty channelTabScrollPaneWidthProperty() {
    return channelTabScrollPaneWidth;
  }


  public boolean getHideFoeMessages() {
    return hideFoeMessages.get();
  }

  public void setHideFoeMessages(boolean hideFoeMessages) {
    this.hideFoeMessages.set(hideFoeMessages);
  }

  public BooleanProperty hideFoeMessagesProperty() {
    return hideFoeMessages;
  }

  public int getIdleThreshold() {
    return idleThreshold.get();
  }

  public void setIdleThreshold(int idleThreshold) {
    this.idleThreshold.set(idleThreshold);
  }

  public IntegerProperty idleThresholdProperty() {
    return idleThreshold;
  }
}
