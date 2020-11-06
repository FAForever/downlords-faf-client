package com.faforever.client.preferences;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatFormat;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.Optional;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.preferences.LanguageChannel.FRENCH;
import static com.faforever.client.preferences.LanguageChannel.GERMAN;
import static com.faforever.client.preferences.LanguageChannel.RUSSIAN;

public class ChatPrefs {

  @VisibleForTesting
  public static final ImmutableMap<Locale, LanguageChannel> LOCALE_LANGUAGES_TO_CHANNELS = ImmutableMap.<Locale, LanguageChannel>builder()
      .put(Locale.FRENCH, FRENCH)
      .put(Locale.GERMAN, GERMAN)
      .put(new Locale("ru"), RUSSIAN)
      .put(new Locale("be"), RUSSIAN)
      .build();

  private final DoubleProperty zoom;
  private final BooleanProperty learnedAutoComplete;
  private final BooleanProperty previewImageUrls;
  private final IntegerProperty maxMessages;
  private final ObjectProperty<ChatColorMode> chatColorMode;
  private final IntegerProperty channelTabScrollPaneWidth;
  private final MapProperty<String, Color> userToColor;
  private final BooleanProperty hideFoeMessages;
  private final BooleanProperty playerListShown;
  private final ObjectProperty<TimeInfo> timeFormat;
  private final ObjectProperty<DateInfo> dateFormat;
  private final ObjectProperty<ChatFormat> chatFormat;
  private final ListProperty<String> autoJoinChannels;
  /**
   * Time in minutes a player has to be inactive to be considered idle.
   */
  private final IntegerProperty idleThreshold;

  public ChatPrefs() {
    timeFormat = new SimpleObjectProperty<>(TimeInfo.AUTO);
    dateFormat = new SimpleObjectProperty<>(DateInfo.AUTO);
    maxMessages = new SimpleIntegerProperty(500);
    zoom = new SimpleDoubleProperty(1);
    learnedAutoComplete = new SimpleBooleanProperty(false);
    previewImageUrls = new SimpleBooleanProperty(true);
    hideFoeMessages = new SimpleBooleanProperty(true);
    channelTabScrollPaneWidth = new SimpleIntegerProperty(250);
    userToColor = new SimpleMapProperty<>(FXCollections.observableHashMap());
    chatColorMode = new SimpleObjectProperty<>(CUSTOM);
    idleThreshold = new SimpleIntegerProperty(10);
    chatFormat = new SimpleObjectProperty<>(ChatFormat.COMPACT);
    autoJoinChannels = new SimpleListProperty<>(FXCollections.observableArrayList());
    playerListShown = new SimpleBooleanProperty(true);

    Locale localeLanguage = new Locale(Locale.getDefault().getLanguage());
    Optional.ofNullable(LOCALE_LANGUAGES_TO_CHANNELS.get(localeLanguage))
        .ifPresent(channel -> autoJoinChannels.get().add(channel.getChannelName()));
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

  public DateInfo getDateFormat() {
    return dateFormat.get();
  }

  public void setDateFormat(DateInfo date) {
    this.dateFormat.set(date);
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

  public ObservableList<String> getAutoJoinChannels() {
    return autoJoinChannels.get();
  }

  public boolean isPlayerListShown() {
    return playerListShown.get();
  }

  public void setPlayerListShown(boolean playerListShown) {
    this.playerListShown.set(playerListShown);
  }

  public BooleanProperty playerListShownProperty() {
    return playerListShown;
  }
}
