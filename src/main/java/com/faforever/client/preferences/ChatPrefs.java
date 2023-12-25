package com.faforever.client.preferences;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatFormat;
import com.faforever.client.chat.ChatUserCategory;
import com.google.common.annotations.VisibleForTesting;
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
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.preferences.LanguageChannel.FRENCH;
import static com.faforever.client.preferences.LanguageChannel.GERMAN;
import static com.faforever.client.preferences.LanguageChannel.RUSSIAN;


public class ChatPrefs {

  @VisibleForTesting
  public static final Map<Locale, LanguageChannel> LOCALE_LANGUAGES_TO_CHANNELS = Map.of(Locale.FRENCH, FRENCH,
                                                                                         Locale.GERMAN, GERMAN,
                                                                                         Locale.of("ru"), RUSSIAN,
                                                                                         Locale.of("be"), RUSSIAN);

  private final DoubleProperty zoom = new SimpleDoubleProperty(1);
  private final BooleanProperty previewImageUrls = new SimpleBooleanProperty(true);
  private final IntegerProperty maxMessages = new SimpleIntegerProperty(500);
  private final ObjectProperty<ChatColorMode> chatColorMode = new SimpleObjectProperty<>(DEFAULT);
  private final MapProperty<String, Color> userToColor = new SimpleMapProperty<>(FXCollections.observableHashMap());
  private final MapProperty<ChatUserCategory, Color> groupToColor = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final BooleanProperty hideFoeMessages = new SimpleBooleanProperty(true);
  private final BooleanProperty playerListShown = new SimpleBooleanProperty(true);
  private final ObjectProperty<TimeInfo> timeFormat = new SimpleObjectProperty<>(TimeInfo.AUTO);
  private final ObjectProperty<ChatFormat> chatFormat = new SimpleObjectProperty<>(ChatFormat.COMPACT);
  private final ObservableList<String> autoJoinChannels = FXCollections.observableArrayList();
  private final BooleanProperty showMapName = new SimpleBooleanProperty(false);
  private final BooleanProperty showMapPreview = new SimpleBooleanProperty(false);
  private final ObservableMap<String, ObservableSet<ChatUserCategory>> channelNameToHiddenCategories = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());

  public ChatPrefs() {
    Optional.ofNullable(LOCALE_LANGUAGES_TO_CHANNELS.get(Locale.of(Locale.getDefault().getLanguage())))
            .ifPresent(channel -> autoJoinChannels.add(channel.getChannelName()));
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

  public ObjectProperty<TimeInfo> timeFormatProperty() {
    return timeFormat;
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

  public ObservableMap<ChatUserCategory, Color> getGroupToColor() {
    return groupToColor.get();
  }

  public void setGroupToColor(ObservableMap<ChatUserCategory, Color> groupToColor) {
    this.groupToColor.set(groupToColor);
  }

  public MapProperty<ChatUserCategory, Color> groupToColorProperty() {
    return groupToColor;
  }

  public boolean isPreviewImageUrls() {
    return previewImageUrls.get();
  }

  public void setPreviewImageUrls(boolean previewImageUrls) {
    this.previewImageUrls.set(previewImageUrls);
  }

  public BooleanProperty previewImageUrlsProperty() {
    return previewImageUrls;
  }

  public double getZoom() {
    return zoom.get();
  }

  public void setZoom(double zoom) {
    this.zoom.set(zoom);
  }

  public DoubleProperty zoomProperty() {
    return zoom;
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

  public boolean isHideFoeMessages() {
    return hideFoeMessages.get();
  }

  public void setHideFoeMessages(boolean hideFoeMessages) {
    this.hideFoeMessages.set(hideFoeMessages);
  }

  public BooleanProperty hideFoeMessagesProperty() {
    return hideFoeMessages;
  }

  public ObservableList<String> getAutoJoinChannels() {
    return autoJoinChannels;
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

  public boolean isShowMapName() {
    return showMapName.get();
  }

  public boolean isShowMapPreview() {
    return showMapPreview.get();
  }

  public BooleanProperty showMapNameProperty() {
    return showMapName;
  }

  public BooleanProperty showMapPreviewProperty() {
    return showMapPreview;
  }

  public void setShowMapPreview(boolean showMapPreview) {
    this.showMapPreview.set(showMapPreview);
  }

  public void setShowMapName(boolean showMapName) {
    this.showMapName.set(showMapName);
  }

  public ObservableMap<String, ObservableSet<ChatUserCategory>> getChannelNameToHiddenCategories() {
    return channelNameToHiddenCategories;
  }

  public void setChannelNameToHiddenCategories(Map<String, ObservableSet<ChatUserCategory>> channelNameToHiddenCategories) {
    this.channelNameToHiddenCategories.clear();
    this.channelNameToHiddenCategories.putAll(channelNameToHiddenCategories);
  }
}
