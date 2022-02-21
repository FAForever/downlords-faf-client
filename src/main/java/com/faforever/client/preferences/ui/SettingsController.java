package com.faforever.client.preferences.ui;

import ch.qos.logback.classic.Level;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatFormat;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.debugger.DownloadFAFDebuggerTask;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CancelAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.DateInfo;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.preferences.tasks.DeleteDirectoryTask;
import com.faforever.client.preferences.tasks.MoveDirectoryTask;
import com.faforever.client.settings.LanguageItemController;
import com.faforever.client.task.TaskService;
import com.faforever.client.theme.Theme;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoSelectionModelListView;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.faforever.client.fx.JavaFxUtil.PATH_STRING_CONVERTER;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class SettingsController implements Controller<Node> {

  private final ApplicationContext applicationContext;
  private final NotificationService notificationService;
  private final UserService userService;
  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final ClientUpdateService clientUpdateService;
  private final GameService gameService;
  private final TaskService taskService;
  private final InvalidationListener availableLanguagesListener;

  public TextField executableDecoratorField;
  public TextField executionDirectoryField;
  public ToggleGroup colorModeToggleGroup;
  public Toggle randomColorsToggle;
  public Toggle defaultColorsToggle;
  public CheckBox hideFoeToggle;
  public CheckBox forceRelayToggle;
  public TextField dataLocationTextField;
  public TextField gameLocationTextField;
  public TextField vaultLocationTextField;
  public CheckBox autoDownloadMapsToggle;
  public CheckBox useFAFDebuggerToggle;
  public TextField maxMessagesTextField;
  public CheckBox imagePreviewToggle;
  public CheckBox enableNotificationsToggle;
  public CheckBox enableSoundsToggle;
  public CheckBox displayFriendOnlineToastCheckBox;
  public CheckBox displayFriendOfflineToastCheckBox;
  public CheckBox playFriendOnlineSoundCheckBox;
  public CheckBox playFriendOfflineSoundCheckBox;
  public CheckBox displayFriendJoinsGameToastCheckBox;
  public CheckBox displayFriendPlaysGameToastCheckBox;
  public CheckBox playFriendJoinsGameSoundCheckBox;
  public CheckBox playFriendPlaysGameSoundCheckBox;
  public CheckBox displayPmReceivedToastCheckBox;
  public CheckBox playPmReceivedSoundCheckBox;
  public CheckBox afterGameReviewCheckBox;
  public Region settingsRoot;
  public ComboBox<Theme> themeComboBox;
  public ToggleGroup toastPositionToggleGroup;
  public ComboBox<Screen> toastScreenComboBox;
  public Toggle bottomLeftToastButton;
  public Toggle topRightToastButton;
  public Toggle topLeftToastButton;
  public ToggleButton bottomRightToastButton;
  public ComboBox<TimeInfo> timeComboBox;
  public ComboBox<DateInfo> dateComboBox;
  public ComboBox<ChatFormat> chatComboBox;
  public ComboBox<UnitDataBaseType> unitDatabaseComboBox;
  public CheckBox notifyOnAtMentionOnlyToggle;
  public Pane languagesContainer;
  public TextField backgroundImageLocation;
  public CheckBox disallowJoinsCheckBox;
  public CheckBox advancedIceLogToggle;
  public CheckBox prereleaseToggle;
  public Region settingsHeader;
  public ComboBox<NavigationItem> startTabChoiceBox;
  public Label notifyAtMentionTitle;
  public Label notifyAtMentionDescription;
  public TextField channelTextField;
  public Button addChannelButton;
  public ListView<String> autoChannelListView;
  public Button clearCacheButton;
  public CheckBox gameDataCacheCheckBox;
  public Spinner<Integer> gameDataCacheTimeSpinner;
  public CheckBox allowReplayWhileInGameCheckBox;
  public Button allowReplayWhileInGameButton;
  public ComboBox<Level> logLevelComboBox;
  public CheckBox mapAndModAutoUpdateCheckBox;
  public TextField mirrorURITextField;
  public ListView<URI> mirrorURLsListView;

  private ChangeListener<Theme> selectedThemeChangeListener;
  private ChangeListener<Theme> currentThemeChangeListener;

  public SettingsController(ApplicationContext applicationContext, UserService userService, PreferencesService preferencesService, UiService uiService,
                            I18n i18n, EventBus eventBus, NotificationService notificationService,
                            PlatformService platformService, ClientProperties clientProperties,
                            ClientUpdateService clientUpdateService, GameService gameService, TaskService taskService) {
    this.applicationContext = applicationContext;
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.notificationService = notificationService;
    this.platformService = platformService;
    this.clientProperties = clientProperties;
    this.clientUpdateService = clientUpdateService;
    this.gameService = gameService;
    this.taskService = taskService;

    availableLanguagesListener = observable -> {
      LocalizationPrefs localization = preferencesService.getPreferences().getLocalization();
      Locale currentLocale = localization.getLanguage();
      List<Node> nodes = i18n.getAvailableLanguages().stream()
          .map(locale -> {
            LanguageItemController controller = uiService.loadFxml("theme/settings/language_item.fxml");
            controller.setLocale(locale);
            controller.setOnSelectedListener(this::onLanguageSelected);
            controller.setSelected(locale.equals(currentLocale));
            return controller.getRoot();
          })
          .collect(Collectors.toList());
      languagesContainer.getChildren().setAll(nodes);
    };
  }

  public void initialize() {
    eventBus.register(this);
    themeComboBox.setButtonCell(new StringListCell<>(Theme::getDisplayName));
    themeComboBox.setCellFactory(param -> new StringListCell<>(Theme::getDisplayName));

    toastScreenComboBox.setButtonCell(screenListCell());
    toastScreenComboBox.setCellFactory(param -> screenListCell());
    toastScreenComboBox.setItems(Screen.getScreens());
    NumberFormat integerNumberFormat = NumberFormat.getIntegerInstance();
    integerNumberFormat.setGroupingUsed(false);
    NumberStringConverter numberToStringConverter = new NumberStringConverter(integerNumberFormat);

    Preferences preferences = preferencesService.getPreferences();
    temporarilyDisableUnsupportedSettings(preferences);

    JavaFxUtil.bindBidirectional(maxMessagesTextField.textProperty(), preferences.getChat().maxMessagesProperty(), numberToStringConverter);
    imagePreviewToggle.selectedProperty().bindBidirectional(preferences.getChat().previewImageUrlsProperty());
    enableNotificationsToggle.selectedProperty().bindBidirectional(preferences.getNotification().transientNotificationsEnabledProperty());

    hideFoeToggle.selectedProperty().bindBidirectional(preferences.getChat().hideFoeMessagesProperty());

    disallowJoinsCheckBox.selectedProperty().bindBidirectional(preferences.disallowJoinsViaDiscordProperty());

    JavaFxUtil.addListener(preferences.getChat().chatColorModeProperty(), (observable, oldValue, newValue) -> setSelectedColorMode(newValue));
    setSelectedColorMode(preferences.getChat().getChatColorMode());

    colorModeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == defaultColorsToggle) {
        preferences.getChat().setChatColorMode(ChatColorMode.DEFAULT);
      }
      if (newValue == randomColorsToggle) {
        preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      }
    });

    currentThemeChangeListener = (observable, oldValue, newValue) -> themeComboBox.getSelectionModel().select(newValue);
    selectedThemeChangeListener = (observable, oldValue, newValue) -> {
      uiService.setTheme(newValue);
      if (oldValue != null && uiService.doesThemeNeedRestart(newValue)) {
        notificationService.addNotification(new PersistentNotification(i18n.get("theme.needsRestart.message", newValue.getDisplayName()), Severity.WARN,
            Collections.singletonList(new Action(i18n.get("theme.needsRestart.quit"), event -> Platform.exit()))));
        // FIXME reload application (stage & application context) https://github.com/FAForever/downlords-faf-client/issues/1794
      }
    };

    configureTimeSetting();
    configureDateSetting();
    configureChatSetting();
    configureLanguageSelection();
    configureThemeSelection();
    configureToastScreen();
    configureStartTab();

    initAutoChannelListView();
    initMirrorUrlsListView();
    initUnitDatabaseSelection();
    initAllowReplaysWhileInGame();
    initNotifyMeOnAtMention();
    initGameDataCache();
    initMapAndModAutoUpdate();
    initLogLevelComboBox();

    bindNotificationPreferences();
    bindGamePreferences();
    bindGeneralPreferences();
  }

  /**
   * Disables preferences that should not be enabled since they are not supported yet.
   */
  private void temporarilyDisableUnsupportedSettings(Preferences preferences) {
    NotificationsPrefs notification = preferences.getNotification();
    notification.setFriendOnlineSoundEnabled(false);
    notification.setFriendOfflineSoundEnabled(false);
    notification.setFriendOfflineSoundEnabled(false);
    notification.setFriendPlaysGameSoundEnabled(false);
    notification.setFriendPlaysGameToastEnabled(false);
  }

  private void setSelectedToastPosition() {
    switch (preferencesService.getPreferences().getNotification().getToastPosition()) {
      case TOP_RIGHT -> toastPositionToggleGroup.selectToggle(topRightToastButton);
      case BOTTOM_RIGHT -> toastPositionToggleGroup.selectToggle(bottomRightToastButton);
      case BOTTOM_LEFT -> toastPositionToggleGroup.selectToggle(bottomLeftToastButton);
      case TOP_LEFT -> toastPositionToggleGroup.selectToggle(topLeftToastButton);
    }
  }

  private void bindGeneralPreferences() {
    Preferences preferences = preferencesService.getPreferences();
    backgroundImageLocation.textProperty().bindBidirectional(preferences.getMainWindow().backgroundImagePathProperty(), PATH_STRING_CONVERTER);

    advancedIceLogToggle.selectedProperty().bindBidirectional(preferences.advancedIceLogEnabledProperty());

    prereleaseToggle.selectedProperty().bindBidirectional(preferences.preReleaseCheckEnabledProperty());
    prereleaseToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (Boolean.TRUE.equals(newValue) && (!Boolean.TRUE.equals(oldValue))) {
        clientUpdateService.checkForUpdateInBackground();
      }
    });

    logLevelComboBox.getSelectionModel().select(Level.toLevel(preferences.getDeveloper().getLogLevel()));
    preferences.getDeveloper().logLevelProperty().bind(logLevelComboBox.getSelectionModel().selectedItemProperty().asString());
    dataLocationTextField.textProperty().bindBidirectional(preferences.getData().baseDataDirectoryProperty(), PATH_STRING_CONVERTER);

  }

  private void bindGamePreferences() {
    ForgedAlliancePrefs forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();
    forceRelayToggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.forceRelayProperty());
    gameLocationTextField.textProperty().bindBidirectional(forgedAlliancePrefs.installationPathProperty(), PATH_STRING_CONVERTER);
    autoDownloadMapsToggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.autoDownloadMapsProperty());
    useFAFDebuggerToggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.runFAWithDebuggerProperty());
    vaultLocationTextField.textProperty().bindBidirectional(forgedAlliancePrefs.vaultBaseDirectoryProperty(), PATH_STRING_CONVERTER);

    useFAFDebuggerToggle.selectedProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue && !oldValue) {
        onUpdateDebuggerClicked();
      }
    }));

    executableDecoratorField.textProperty().bindBidirectional(forgedAlliancePrefs.executableDecoratorProperty());
    executionDirectoryField.textProperty().bindBidirectional(forgedAlliancePrefs.executionDirectoryProperty(), PATH_STRING_CONVERTER);
  }

  private void initMirrorUrlsListView() {
    mirrorURLsListView.setSelectionModel(new NoSelectionModelListView<>());
    mirrorURLsListView.setFocusTraversable(false);
    mirrorURLsListView.setItems(preferencesService.getPreferences().getMirror().getMirrorURLs());
    mirrorURLsListView.setCellFactory(param -> uiService.<RemovableListCellController<URI>>loadFxml("theme/settings/removable_cell.fxml"));
    JavaFxUtil.addListener(mirrorURLsListView.getItems(), (ListChangeListener<URI>) c -> {
      preferencesService.storeInBackground();
      mirrorURLsListView.setVisible(!mirrorURLsListView.getItems().isEmpty());
    });
  }

  private void initAutoChannelListView() {
    autoChannelListView.setSelectionModel(new NoSelectionModelListView<>());
    autoChannelListView.setFocusTraversable(false);
    autoChannelListView.setItems(preferencesService.getPreferences().getChat().getAutoJoinChannels());
    autoChannelListView.setCellFactory(param -> uiService.<RemovableListCellController<String>>loadFxml("theme/settings/removable_cell.fxml"));
    JavaFxUtil.addListener(autoChannelListView.getItems(), (ListChangeListener<String>) c -> {
      preferencesService.storeInBackground();
      autoChannelListView.setVisible(!autoChannelListView.getItems().isEmpty());
    });
  }

  private void bindNotificationPreferences() {
    NotificationsPrefs notificationsPrefs = preferencesService.getPreferences().getNotification();
    displayFriendOnlineToastCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendOnlineToastEnabledProperty());
    displayFriendOfflineToastCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendOfflineToastEnabledProperty());
    displayFriendJoinsGameToastCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendJoinsGameToastEnabledProperty());
    displayFriendPlaysGameToastCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendPlaysGameToastEnabledProperty());
    displayPmReceivedToastCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.privateMessageToastEnabledProperty());
    playFriendOnlineSoundCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendOnlineSoundEnabledProperty());
    playFriendOfflineSoundCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendOfflineSoundEnabledProperty());
    playFriendJoinsGameSoundCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendJoinsGameSoundEnabledProperty());
    playFriendPlaysGameSoundCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.friendPlaysGameSoundEnabledProperty());
    playPmReceivedSoundCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.privateMessageSoundEnabledProperty());
    afterGameReviewCheckBox.selectedProperty().bindBidirectional(notificationsPrefs.afterGameReviewEnabledProperty());
    notifyOnAtMentionOnlyToggle.selectedProperty().bindBidirectional(notificationsPrefs.notifyOnAtMentionOnlyEnabledProperty());
    enableSoundsToggle.selectedProperty().bindBidirectional(notificationsPrefs.soundsEnabledProperty());
  }

  private void initMapAndModAutoUpdate() {
    mapAndModAutoUpdateCheckBox.selectedProperty()
        .bindBidirectional(preferencesService.getPreferences().mapAndModAutoUpdateProperty());
  }

  private void initLogLevelComboBox() {
    logLevelComboBox.setItems(FXCollections.observableArrayList(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR));
  }

  private void initGameDataCache() {
    gameDataCacheCheckBox.selectedProperty().bindBidirectional(preferencesService.getPreferences().gameDataCacheActivatedProperty());
    //Binding for CacheLifeTimeInDays does not work because of some java fx bug
    gameDataCacheTimeSpinner.getValueFactory().setValue(preferencesService.getPreferences().getCacheLifeTimeInDays());
    gameDataCacheTimeSpinner.getValueFactory().valueProperty()
        .addListener((observable, oldValue, newValue) -> preferencesService.getPreferences().setCacheLifeTimeInDays(newValue));
  }

  private void initNotifyMeOnAtMention() {
    String username = userService.getUsername();
    notifyAtMentionTitle.setText(i18n.get("settings.chat.notifyOnAtMentionOnly", "@" + username));
    notifyAtMentionDescription.setText(i18n.get("settings.chat.notifyOnAtMentionOnly.description", "@" + username));
  }

  private void initAllowReplaysWhileInGame() {
    ForgedAlliancePrefs forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();
    allowReplayWhileInGameCheckBox.setSelected(forgedAlliancePrefs.isAllowReplaysWhileInGame());
    JavaFxUtil.bindBidirectional(allowReplayWhileInGameCheckBox.selectedProperty(), forgedAlliancePrefs.allowReplaysWhileInGameProperty());
    try {
      gameService.isGamePrefsPatchedToAllowMultiInstances()
          .thenAccept(isPatched -> allowReplayWhileInGameButton.setDisable(isPatched));
    } catch (IOException e) {
      log.warn("Failed evaluating if game.prefs file is patched for multiple instances", e);
      allowReplayWhileInGameButton.setDisable(true);
    }
  }

  private void configureStartTab() {
    WindowPrefs mainWindow = preferencesService.getPreferences().getMainWindow();
    startTabChoiceBox.setItems(FXCollections.observableArrayList(NavigationItem.values()));
    startTabChoiceBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(NavigationItem navigationItem) {
        return i18n.get(navigationItem.getI18nKey());
      }

      @Override
      public NavigationItem fromString(String s) {
        throw new UnsupportedOperationException("Not needed");
      }
    });
    startTabChoiceBox.getSelectionModel().select(mainWindow.getNavigationItem());
    mainWindow.navigationItemProperty().bind(startTabChoiceBox.getSelectionModel().selectedItemProperty());
  }

  private void initUnitDatabaseSelection() {
    Preferences preferences = preferencesService.getPreferences();
    unitDatabaseComboBox.setButtonCell(new StringListCell<>(unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey())));
    unitDatabaseComboBox.setCellFactory(param -> new StringListCell<>(unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey())));
    unitDatabaseComboBox.setItems(FXCollections.observableArrayList(UnitDataBaseType.values()));
    unitDatabaseComboBox.setFocusTraversable(true);

    ChangeListener<UnitDataBaseType> unitDataBaseTypeChangeListener = (observable, oldValue, newValue) -> unitDatabaseComboBox.getSelectionModel().select(newValue);
    unitDataBaseTypeChangeListener.changed(null, null, preferences.getUnitDataBaseType());
    JavaFxUtil.addListener(preferences.unitDataBaseTypeProperty(), unitDataBaseTypeChangeListener);

    unitDatabaseComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      preferences.setUnitDataBaseType(newValue);
      preferencesService.storeInBackground();
    });
  }

  private void configureTimeSetting() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    timeComboBox.setButtonCell(new StringListCell<>(timeInfo -> i18n.get(timeInfo.getDisplayNameKey())));
    timeComboBox.setCellFactory(param -> new StringListCell<>(timeInfo -> i18n.get(timeInfo.getDisplayNameKey())));
    timeComboBox.setItems(FXCollections.observableArrayList(TimeInfo.values()));
    timeComboBox.setDisable(false);
    timeComboBox.setFocusTraversable(true);
    timeComboBox.getSelectionModel().select(chatPrefs.getTimeFormat());
  }

  public void onTimeFormatSelected() {
    log.trace("A new time format was selected: `{}`", timeComboBox.getValue());
    Preferences preferences = preferencesService.getPreferences();
    preferences.getChat().setTimeFormat(timeComboBox.getValue());
    preferencesService.storeInBackground();
  }

  private void configureDateSetting() {
    LocalizationPrefs localizationPrefs = preferencesService.getPreferences().getLocalization();
    dateComboBox.setButtonCell(new StringListCell<>(dateInfo -> i18n.get(dateInfo.getDisplayNameKey())));
    dateComboBox.setCellFactory(param -> new StringListCell<>(dateInfo -> i18n.get(dateInfo.getDisplayNameKey())));
    dateComboBox.setItems(FXCollections.observableArrayList(DateInfo.values()));
    dateComboBox.setDisable(false);
    dateComboBox.setFocusTraversable(true);
    dateComboBox.getSelectionModel().select(localizationPrefs.getDateFormat());
  }

  public void onDateFormatSelected() {
    log.trace("A new date format was selected: `{}`", dateComboBox.getValue());
    Preferences preferences = preferencesService.getPreferences();
    preferences.getLocalization().setDateFormat(dateComboBox.getValue());
    preferencesService.storeInBackground();
  }


  private void configureChatSetting() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    chatComboBox.setButtonCell(new StringListCell<>(chatFormat -> i18n.get(chatFormat.getI18nKey())));
    chatComboBox.setCellFactory(param -> new StringListCell<>(chatFormat -> i18n.get(chatFormat.getI18nKey())));
    chatComboBox.setItems(FXCollections.observableArrayList(ChatFormat.values()));
    chatComboBox.getSelectionModel().select(chatPrefs.getChatFormat());
  }

  public void onChatFormatSelected() {
    log.trace("A new chat format was selected: `{}`", chatComboBox.getValue());
    Preferences preferences = preferencesService.getPreferences();
    preferences.getChat().setChatFormat(chatComboBox.getValue());
    preferencesService.storeInBackground();
  }

  private StringListCell<Screen> screenListCell() {
    return new StringListCell<>(screen -> i18n.get("settings.screenFormat", Screen.getScreens().indexOf(screen) + 1));
  }

  private void setSelectedColorMode(ChatColorMode newValue) {
    if (newValue != null) {
      switch (newValue) {
        case DEFAULT -> colorModeToggleGroup.selectToggle(defaultColorsToggle);
        case RANDOM -> colorModeToggleGroup.selectToggle(randomColorsToggle);
      }
    } else {
      colorModeToggleGroup.selectToggle(defaultColorsToggle);
    }
  }

  private void configureThemeSelection() {
    themeComboBox.setItems(FXCollections.observableArrayList(uiService.getAvailableThemes()));

    themeComboBox.getSelectionModel().select(uiService.getCurrentTheme());

    themeComboBox.getSelectionModel().selectedItemProperty().addListener(selectedThemeChangeListener);
    JavaFxUtil.addListener(uiService.currentThemeProperty(), new WeakChangeListener<>(currentThemeChangeListener));
  }

  private void configureLanguageSelection() {
    JavaFxUtil.addAndTriggerListener(i18n.getAvailableLanguages(), new WeakInvalidationListener(availableLanguagesListener));
  }

  @VisibleForTesting
  void onLanguageSelected(Locale locale) {
    LocalizationPrefs localizationPrefs = preferencesService.getPreferences().getLocalization();
    if (Objects.equals(locale, localizationPrefs.getLanguage())) {
      return;
    }
    log.trace("A new language was selected: `{}`", locale);
    localizationPrefs.setLanguage(locale);
    preferencesService.storeInBackground();

    availableLanguagesListener.invalidated(i18n.getAvailableLanguages());

    notificationService.addNotification(new PersistentNotification(
        i18n.get(locale, "settings.languages.restart.message"),
        Severity.WARN,
        Collections.singletonList(new Action(i18n.get(locale, "settings.languages.restart"),
            event -> {
              Platform.exit();
              // FIXME reload application (stage & application context)
            })
        )));
  }

  private void configureToastScreen() {
    Preferences preferences = preferencesService.getPreferences();
    JavaFxUtil.addAndTriggerListener(preferences.getNotification().toastPositionProperty(), observable -> setSelectedToastPosition());
    toastPositionToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == topLeftToastButton) {
        preferences.getNotification().setToastPosition(ToastPosition.TOP_LEFT);
      }
      if (newValue == topRightToastButton) {
        preferences.getNotification().setToastPosition(ToastPosition.TOP_RIGHT);
      }
      if (newValue == bottomLeftToastButton) {
        preferences.getNotification().setToastPosition(ToastPosition.BOTTOM_LEFT);
      }
      if (newValue == bottomRightToastButton) {
        preferences.getNotification().setToastPosition(ToastPosition.BOTTOM_RIGHT);
      }
    });

    NotificationsPrefs notificationsPrefs = preferences.getNotification();
    toastScreenComboBox.getSelectionModel().select(notificationsPrefs.getToastScreen());
    notificationsPrefs.toastScreenProperty().bind(Bindings.createIntegerBinding(()
        -> Screen.getScreens().indexOf(toastScreenComboBox.getValue()), toastScreenComboBox.valueProperty()));
  }

  public Region getRoot() {
    return settingsRoot;
  }

  public void onSelectGameLocation() {
    eventBus.post(new GameDirectoryChooseEvent());
  }

  public void onSelectVaultLocation() {
    platformService.askForPath(i18n.get("settings.vault.select")).ifPresent(newVaultDirectory -> {
      log.info("User changed vault directory to: `{}`", newVaultDirectory);

      ForgedAlliancePrefs forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();

      MoveDirectoryTask moveDirectoryTask = applicationContext.getBean(MoveDirectoryTask.class);
      moveDirectoryTask.setOldDirectory(forgedAlliancePrefs.getVaultBaseDirectory());
      moveDirectoryTask.setNewDirectory(newVaultDirectory);
      moveDirectoryTask.setAfterCopyAction(() -> {
        forgedAlliancePrefs.setVaultBaseDirectory(newVaultDirectory);
        preferencesService.storeInBackground();
      });
      notificationService.addNotification(new ImmediateNotification(i18n.get("settings.vault.change"), i18n.get("settings.vault.change.message"), Severity.INFO,
          List.of(
              new Action(i18n.get("no"), event -> {
                moveDirectoryTask.setPreserveOldDirectory(false);
                taskService.submitTask(moveDirectoryTask);
              }),
              new Action(i18n.get("yes"), event -> {
                moveDirectoryTask.setPreserveOldDirectory(true);
                taskService.submitTask(moveDirectoryTask);
              }),
              new CancelAction(i18n)
          )));
    });
  }

  public void onSelectDataLocation() {
    platformService.askForPath(i18n.get("settings.data.select")).ifPresent(newDataDirectory -> {
      log.info("User changed data directory to: `{}`", newDataDirectory);
      DataPrefs dataPrefs = preferencesService.getPreferences().getData();

      MoveDirectoryTask moveDirectoryTask = applicationContext.getBean(MoveDirectoryTask.class);
      moveDirectoryTask.setNewDirectory(newDataDirectory);
      moveDirectoryTask.setOldDirectory(dataPrefs.getBaseDataDirectory());
      moveDirectoryTask.setAfterCopyAction(() -> {
        dataPrefs.setBaseDataDirectory(newDataDirectory);
        preferencesService.storeInBackground();
      });
      taskService.submitTask(moveDirectoryTask);
    });
  }

  public void onSelectExecutionDirectory() {
    // TODO implement
  }

  public void onPreviewToastButtonClicked() {
    notificationService.addNotification(new TransientNotification(
        i18n.get("settings.notifications.toastPreview.title"),
        i18n.get("settings.notifications.toastPreview.text")
    ));
  }

  public void onHelpUsButtonClicked() {
    platformService.showDocument(clientProperties.getTranslationProjectUrl());
  }

  public void onSelectBackgroundImage() {
    platformService.askForPath(i18n.get("settings.appearance.chooseImage")).ifPresent(newImagePath -> {
      preferencesService.getPreferences().getMainWindow().setBackgroundImagePath(newImagePath);
      preferencesService.storeInBackground();
    });
  }

  public void onUseNoBackgroundImage(ActionEvent actionEvent) {
    preferencesService.getPreferences().getMainWindow().setBackgroundImagePath(null);
    preferencesService.storeInBackground();
  }

  public void openDiscordFeedbackChannel() {
    platformService.showDocument(clientProperties.getDiscord().getDiscordPrereleaseFeedbackChannelUrl());
  }

  public void openWebsite() {
    platformService.showDocument(clientProperties.getWebsite().getBaseUrl());
  }

  public void onAddAutoChannel() {
    if (channelTextField.getText().isBlank() || autoChannelListView.getItems().contains(channelTextField.getText())) {
      return;
    }
    if (!channelTextField.getText().startsWith("#")) {
      channelTextField.setText("#" + channelTextField.getText());
    }
    preferencesService.getPreferences().getChat().getAutoJoinChannels().add(channelTextField.getText());
    preferencesService.storeInBackground();
    channelTextField.clear();
  }

  public void onPatchGamePrefsForMultipleInstances() {
    try {
      gameService.patchGamePrefsForMultiInstances()
          .thenRun(() -> JavaFxUtil.runLater(() -> allowReplayWhileInGameButton.setDisable(true)))
          .exceptionally(throwable -> {
            log.error("Game.prefs patch failed", throwable);
            notificationService.addImmediateErrorNotification(throwable, "settings.fa.patchGamePrefsFailed");
            return null;
          });
    } catch (Exception e) {
      log.error("Game.prefs patch failed", e);
      notificationService.addImmediateErrorNotification(e, "settings.fa.patchGamePrefsFailed");
    }
  }

  public void onUpdateDebuggerClicked() {
    DownloadFAFDebuggerTask downloadFAFDebuggerTask = applicationContext.getBean(DownloadFAFDebuggerTask.class);
    taskService.submitTask(downloadFAFDebuggerTask).getFuture().exceptionally(throwable -> {
      useFAFDebuggerToggle.setSelected(false);
      notificationService.addImmediateErrorNotification(throwable, "settings.fa.updateDebugger.failed");
      return null;
    });
  }

  public void onAddMirrorURL() {
    String text = mirrorURITextField.getText();
    if (text.isBlank()) {
      return;
    }
    if (!text.endsWith("/")) {
      text = text + "/";
    }

    try {
      URI uri = new URL(text).toURI();

      if (mirrorURLsListView.getItems().contains(uri)) {
        return;
      }
      preferencesService.getPreferences().getMirror().getMirrorURLs().add(uri);
      preferencesService.storeInBackground();
      mirrorURITextField.clear();
    } catch (URISyntaxException | MalformedURLException e) {
      log.warn("Failed to add invalid URL: {}", text, e);
      notificationService.addImmediateWarnNotification("settings.data.mirrorURLs.add.error", e.getMessage());
    }
  }

  public void onClearCacheClicked() {
    DeleteDirectoryTask deleteDirectoryTask = applicationContext.getBean(DeleteDirectoryTask.class);
    deleteDirectoryTask.setDirectory(preferencesService.getPreferences().getData().getCacheDirectory());

    taskService.submitTask(deleteDirectoryTask);
  }
}

