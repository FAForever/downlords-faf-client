package com.faforever.client.preferences.ui;

import ch.qos.logback.classic.Level;
import com.faforever.client.api.IceServer;
import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.debugger.DownloadFAFDebuggerTask;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.game.VaultPathHandler;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.DateInfo;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.preferences.NotificationPrefs;
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
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.list.NoSelectionModel;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.LoginService;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.faforever.client.fx.JavaFxUtil.PATH_STRING_CONVERTER;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class SettingsController extends NodeController<Node> {

  private final NotificationService notificationService;
  private final LoginService loginService;
  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final ThemeService themeService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final ClientUpdateService clientUpdateService;
  private final TaskService taskService;
  private final CoturnService coturnService;
  private final VaultPathHandler vaultPathHandler;
  private final Preferences preferences;
  private final ObjectFactory<MoveDirectoryTask> moveDirectoryTaskFactory;
  private final ObjectFactory<DeleteDirectoryTask> deleteDirectoryTaskFactory;
  private final ObjectFactory<DownloadFAFDebuggerTask> downloadFAFDebuggerTaskFactory;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final GamePathHandler gamePathHandler;

  public TextField executableDecoratorField;
  public TextField executionDirectoryField;
  public ToggleGroup colorModeToggleGroup;
  public Toggle randomColorsToggle;
  public Toggle defaultColorsToggle;
  public CheckBox hideFoeToggle;
  public TextField dataLocationTextField;
  public TextField gameLocationTextField;
  public TextField vaultLocationTextField;
  public Label vaultLocationWarningLabel;
  public CheckBox autoDownloadMapsToggle;
  public CheckBox relativePathsToggle;
  public CheckBox useFAFDebuggerToggle;
  public CheckBox allowIpv6Toggle;
  public CheckBox showIceAdapterDebugWindowToggle;
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
  public CheckBox displayMatchFoundNotificationCheckBox;
  public CheckBox playMatchFoundSoundCheckBox;
  public CheckBox playPmReceivedSoundCheckBox;
  public CheckBox afterGameReviewCheckBox;
  public CheckBox disableSteamStartCheckBox;
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
  public ComboBox<Level> logLevelComboBox;
  public CheckBox mapAndModAutoUpdateCheckBox;
  public ListView<IceServer> preferredCoturnListView;

  private final SimpleChangeListener<Theme> selectedThemeChangeListener = this::onThemeChanged;
  private final SimpleChangeListener<Theme> currentThemeChangeListener = newValue -> themeComboBox.getSelectionModel()
                                                                                                  .select(newValue);
  private final SimpleInvalidationListener availableLanguagesListener = this::setAvailableLanguages;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(vaultLocationWarningLabel);
    themeComboBox.setButtonCell(new StringListCell<>(Theme::displayName, fxApplicationThreadExecutor));
    themeComboBox.setCellFactory(param -> new StringListCell<>(Theme::displayName, fxApplicationThreadExecutor));

    toastScreenComboBox.setButtonCell(screenListCell());
    toastScreenComboBox.setCellFactory(param -> screenListCell());
    toastScreenComboBox.setItems(Screen.getScreens());
    NumberFormat integerNumberFormat = NumberFormat.getIntegerInstance();
    integerNumberFormat.setGroupingUsed(false);
    NumberStringConverter numberToStringConverter = new NumberStringConverter(integerNumberFormat);

    temporarilyDisableUnsupportedSettings(preferences);

    JavaFxUtil.bindBidirectional(maxMessagesTextField.textProperty(), preferences.getChat()
        .maxMessagesProperty(), numberToStringConverter);
    imagePreviewToggle.selectedProperty().bindBidirectional(preferences.getChat().previewImageUrlsProperty());
    enableNotificationsToggle.selectedProperty()
        .bindBidirectional(preferences.getNotification().transientNotificationsEnabledProperty());

    hideFoeToggle.selectedProperty().bindBidirectional(preferences.getChat().hideFoeMessagesProperty());

    disallowJoinsCheckBox.selectedProperty().bindBidirectional(preferences.disallowJoinsViaDiscordProperty());
    disableSteamStartCheckBox.selectedProperty()
        .bindBidirectional(preferences.getGeneral().disableSteamStartProperty());

    JavaFxUtil.addListener(preferences.getChat()
        .chatColorModeProperty(), (observable, oldValue, newValue) -> setSelectedColorMode(newValue));
    setSelectedColorMode(preferences.getChat().getChatColorMode());

    colorModeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == defaultColorsToggle) {
        preferences.getChat().setChatColorMode(ChatColorMode.DEFAULT);
      }
      if (newValue == randomColorsToggle) {
        preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      }
    });

    configureTimeSetting();
    configureDateSetting();
    configureLanguageSelection();
    configureThemeSelection();
    configureToastScreen();
    configureStartTab();

    initAutoChannelListView();
    initPreferredCoturnListView();
    initUnitDatabaseSelection();
    initNotifyMeOnAtMention();
    initGameDataCache();
    initMapAndModAutoUpdate();
    initLogLevelComboBox();

    bindNotificationPreferences();
    bindGamePreferences();
    bindGeneralPreferences();
  }

  private void onThemeChanged(Theme newValue) {
    themeService.setTheme(newValue);
    if (themeService.doesThemeNeedRestart(newValue)) {
      notificationService.addNotification(
          new PersistentNotification(i18n.get("theme.needsRestart.message", newValue.displayName()), Severity.WARN,
                                                                     Collections.singletonList(
                                                                         new Action(i18n.get("theme.needsRestart.quit"),
                                                                                    Platform::exit))));
    }
  }

  private void setAvailableLanguages() {
    LocalizationPrefs localization = preferences.getLocalization();
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
  }

  /**
   * Disables preferences that should not be enabled since they are not supported yet.
   */
  private void temporarilyDisableUnsupportedSettings(Preferences preferences) {
    NotificationPrefs notification = preferences.getNotification();
    notification.setFriendPlaysGameToastEnabled(false);
  }

  private void setSelectedToastPosition() {
    switch (preferences.getNotification().getToastPosition()) {
      case TOP_RIGHT -> toastPositionToggleGroup.selectToggle(topRightToastButton);
      case BOTTOM_RIGHT -> toastPositionToggleGroup.selectToggle(bottomRightToastButton);
      case BOTTOM_LEFT -> toastPositionToggleGroup.selectToggle(bottomLeftToastButton);
      case TOP_LEFT -> toastPositionToggleGroup.selectToggle(topLeftToastButton);
    }
  }

  private void bindGeneralPreferences() {
    backgroundImageLocation.textProperty()
        .bindBidirectional(preferences.getMainWindow().backgroundImagePathProperty(), PATH_STRING_CONVERTER);

    advancedIceLogToggle.selectedProperty().bindBidirectional(preferences.advancedIceLogEnabledProperty());

    prereleaseToggle.selectedProperty().bindBidirectional(preferences.preReleaseCheckEnabledProperty());
    prereleaseToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (Boolean.TRUE.equals(newValue) && (!Boolean.TRUE.equals(oldValue))) {
        clientUpdateService.checkForUpdateInBackground();
      }
    });

    logLevelComboBox.getSelectionModel().select(Level.toLevel(preferences.getDeveloper().getLogLevel()));
    preferences.getDeveloper()
        .logLevelProperty()
        .bind(logLevelComboBox.getSelectionModel().selectedItemProperty().asString());
    dataLocationTextField.textProperty()
        .bindBidirectional(preferences.getData().baseDataDirectoryProperty(), PATH_STRING_CONVERTER);

  }

  private void bindGamePreferences() {
    ForgedAlliancePrefs forgedAlliancePrefs = preferences.getForgedAlliance();
    gameLocationTextField.textProperty()
        .bindBidirectional(forgedAlliancePrefs.installationPathProperty(), PATH_STRING_CONVERTER);
    autoDownloadMapsToggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.autoDownloadMapsProperty());
    relativePathsToggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.relativeGamePathsProperty());
    useFAFDebuggerToggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.runFAWithDebuggerProperty());
    allowIpv6Toggle.selectedProperty().bindBidirectional(forgedAlliancePrefs.allowIpv6Property());
    showIceAdapterDebugWindowToggle.selectedProperty()
        .bindBidirectional(forgedAlliancePrefs.showIceAdapterDebugWindow());
    vaultLocationTextField.textProperty()
        .bindBidirectional(forgedAlliancePrefs.vaultBaseDirectoryProperty(), PATH_STRING_CONVERTER);
    JavaFxUtil.addAndTriggerListener(vaultLocationTextField.textProperty(), (observable) ->
        vaultLocationWarningLabel.setVisible(preferencesService.isVaultBasePathInvalidForAscii()));

    useFAFDebuggerToggle.selectedProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue && !oldValue) {
        onUpdateDebuggerClicked();
      }
    }));

    executableDecoratorField.textProperty().bindBidirectional(forgedAlliancePrefs.executableDecoratorProperty());
    executionDirectoryField.textProperty()
        .bindBidirectional(forgedAlliancePrefs.executionDirectoryProperty(), PATH_STRING_CONVERTER);
  }

  private void initPreferredCoturnListView() {
    coturnService.getActiveCoturns()
                 .collectList()
                 .map(FXCollections::observableList)
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(coturnServers -> {
                   preferredCoturnListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                   preferredCoturnListView.setItems(FXCollections.observableList(coturnServers));
                   preferredCoturnListView.setCellFactory(
                       param -> new StringListCell<>(IceServer::region, fxApplicationThreadExecutor));
                   Map<String, IceServer> hostPortCoturnServerMap = coturnServers.stream()
                                                                                 .collect(
                                                                                     Collectors.toMap(IceServer::id,
                                                                                                      Function.identity()));

                   ObservableSet<String> preferredCoturnServers = preferences.getForgedAlliance()
                                                                             .getPreferredCoturnIds();

                   preferredCoturnServers.stream()
                                         .filter(hostPortCoturnServerMap::containsKey)
                                         .map(hostPortCoturnServerMap::get)
                                         .forEach(coturnServer -> preferredCoturnListView.getSelectionModel()
                                                                                         .select(coturnServer));

                   JavaFxUtil.addAndTriggerListener(preferredCoturnListView.getSelectionModel().getSelectedItems(),
                                                    observable -> {
                                                      List<IceServer> selectedCoturns = preferredCoturnListView.getSelectionModel()
                                                                                                               .getSelectedItems();
                                                      preferredCoturnServers.clear();
                                                      selectedCoturns.stream()
                                                                     .map(IceServer::id)
                                                                     .forEach(preferredCoturnServers::add);
                                                    });
                 });
  }

  private void initAutoChannelListView() {
    autoChannelListView.setSelectionModel(new NoSelectionModel<>());
    autoChannelListView.setFocusTraversable(false);
    autoChannelListView.setItems(preferences.getChat().getAutoJoinChannels());
    autoChannelListView.setCellFactory(param -> new RemovableListCell<>(uiService, fxApplicationThreadExecutor));
    JavaFxUtil.addListener(autoChannelListView.getItems(), (InvalidationListener) observable -> autoChannelListView.setVisible(!autoChannelListView.getItems().isEmpty()));
  }

  private void bindNotificationPreferences() {
    NotificationPrefs notificationPrefs = preferences.getNotification();
    displayFriendOnlineToastCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendOnlineToastEnabledProperty());
    displayFriendOfflineToastCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendOfflineToastEnabledProperty());
    displayFriendJoinsGameToastCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendJoinsGameToastEnabledProperty());
    displayFriendPlaysGameToastCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendPlaysGameToastEnabledProperty());
    displayPmReceivedToastCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.privateMessageToastEnabledProperty());
    playFriendOnlineSoundCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendOnlineSoundEnabledProperty());
    playFriendOfflineSoundCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendOfflineSoundEnabledProperty());
    playFriendJoinsGameSoundCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendJoinsGameSoundEnabledProperty());
    playFriendPlaysGameSoundCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.friendPlaysGameSoundEnabledProperty());
    playPmReceivedSoundCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.privateMessageSoundEnabledProperty());
    displayMatchFoundNotificationCheckBox.selectedProperty().setValue(true);
    playMatchFoundSoundCheckBox.selectedProperty()
        .bindBidirectional(notificationPrefs.matchFoundSoundEnabledProperty());
    afterGameReviewCheckBox.selectedProperty().bindBidirectional(notificationPrefs.afterGameReviewEnabledProperty());
    notifyOnAtMentionOnlyToggle.selectedProperty()
        .bindBidirectional(notificationPrefs.notifyOnAtMentionOnlyEnabledProperty());
    enableSoundsToggle.selectedProperty().bindBidirectional(notificationPrefs.soundsEnabledProperty());
  }

  private void initMapAndModAutoUpdate() {
    mapAndModAutoUpdateCheckBox.selectedProperty()
        .bindBidirectional(preferences.mapAndModAutoUpdateProperty());
  }

  private void initLogLevelComboBox() {
    logLevelComboBox.setItems(FXCollections.observableArrayList(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR));
  }

  private void initGameDataCache() {
    gameDataCacheCheckBox.selectedProperty()
        .bindBidirectional(preferences.gameDataCacheActivatedProperty());
    //Binding for CacheLifeTimeInDays does not work because of some java fx bug
    gameDataCacheTimeSpinner.getValueFactory().setValue(preferences.getCacheLifeTimeInDays());
    gameDataCacheTimeSpinner.getValueFactory().valueProperty()
        .addListener((observable, oldValue, newValue) -> preferences
            .setCacheLifeTimeInDays(newValue));
  }

  private void initNotifyMeOnAtMention() {
    String username = loginService.getUsername();
    notifyAtMentionTitle.setText(i18n.get("settings.chat.notifyOnAtMentionOnly", "@" + username));
    notifyAtMentionDescription.setText(i18n.get("settings.chat.notifyOnAtMentionOnly.description", "@" + username));
  }

  private void configureStartTab() {
    WindowPrefs mainWindow = preferences.getMainWindow();
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
    unitDatabaseComboBox.setButtonCell(new StringListCell<>(unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey()), fxApplicationThreadExecutor));
    unitDatabaseComboBox.setCellFactory(param -> new StringListCell<>(unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey()), fxApplicationThreadExecutor));
    unitDatabaseComboBox.setItems(FXCollections.observableArrayList(UnitDataBaseType.values()));
    unitDatabaseComboBox.setFocusTraversable(true);

    ChangeListener<UnitDataBaseType> unitDataBaseTypeChangeListener = (observable, oldValue, newValue) -> unitDatabaseComboBox.getSelectionModel()
        .select(newValue);
    unitDataBaseTypeChangeListener.changed(null, null, preferences.getUnitDataBaseType());
    JavaFxUtil.addListener(preferences.unitDataBaseTypeProperty(), unitDataBaseTypeChangeListener);

    unitDatabaseComboBox.getSelectionModel()
        .selectedItemProperty()
        .addListener((SimpleChangeListener<UnitDataBaseType>) preferences::setUnitDataBaseType);
  }

  private void configureTimeSetting() {
    ChatPrefs chatPrefs = preferences.getChat();
    timeComboBox.setButtonCell(new StringListCell<>(timeInfo -> i18n.get(timeInfo.getDisplayNameKey()), fxApplicationThreadExecutor));
    timeComboBox.setCellFactory(param -> new StringListCell<>(timeInfo -> i18n.get(timeInfo.getDisplayNameKey()), fxApplicationThreadExecutor));
    timeComboBox.setItems(FXCollections.observableArrayList(TimeInfo.values()));
    timeComboBox.setDisable(false);
    timeComboBox.setFocusTraversable(true);
    timeComboBox.getSelectionModel().select(chatPrefs.getTimeFormat());
  }

  public void onTimeFormatSelected() {
    log.trace("A new time format was selected: `{}`", timeComboBox.getValue());
    preferences.getChat().setTimeFormat(timeComboBox.getValue());
  }

  private void configureDateSetting() {
    LocalizationPrefs localizationPrefs = preferences.getLocalization();
    dateComboBox.setButtonCell(new StringListCell<>(dateInfo -> i18n.get(dateInfo.getDisplayNameKey()), fxApplicationThreadExecutor));
    dateComboBox.setCellFactory(param -> new StringListCell<>(dateInfo -> i18n.get(dateInfo.getDisplayNameKey()), fxApplicationThreadExecutor));
    dateComboBox.setItems(FXCollections.observableArrayList(DateInfo.values()));
    dateComboBox.setDisable(false);
    dateComboBox.setFocusTraversable(true);
    dateComboBox.getSelectionModel().select(localizationPrefs.getDateFormat());
  }

  public void onDateFormatSelected() {
    log.trace("A new date format was selected: `{}`", dateComboBox.getValue());
    preferences.getLocalization().setDateFormat(dateComboBox.getValue());
  }

  private StringListCell<Screen> screenListCell() {
    return new StringListCell<>(screen -> i18n.get("settings.screenFormat", Screen.getScreens()
        .indexOf(screen) + 1), fxApplicationThreadExecutor);
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
    themeComboBox.setItems(FXCollections.observableList(themeService.getAvailableThemes()));

    themeComboBox.getSelectionModel().select(themeService.getCurrentTheme());

    themeComboBox.getSelectionModel().selectedItemProperty().addListener(selectedThemeChangeListener);
    JavaFxUtil.addListener(themeService.currentThemeProperty(), new WeakChangeListener<>(currentThemeChangeListener));
  }

  private void configureLanguageSelection() {
    JavaFxUtil.addAndTriggerListener(i18n.getAvailableLanguages(), new WeakInvalidationListener(availableLanguagesListener));
  }

  @VisibleForTesting
  void onLanguageSelected(Locale locale) {
    LocalizationPrefs localizationPrefs = preferences.getLocalization();
    if (Objects.equals(locale, localizationPrefs.getLanguage())) {
      return;
    }
    log.trace("A new language was selected: `{}`", locale);
    localizationPrefs.setLanguage(locale);

    availableLanguagesListener.invalidated(i18n.getAvailableLanguages());

    // FIXME reload application (stage & application context)
    notificationService.addNotification(new PersistentNotification(
        i18n.get(locale, "settings.languages.restart.message"),
        Severity.WARN, List.of(new Action(i18n.get(locale, "settings.languages.restart"), Platform::exit)
        )));
  }

  private void configureToastScreen() {
    JavaFxUtil.addAndTriggerListener(preferences.getNotification()
        .toastPositionProperty(), observable -> setSelectedToastPosition());
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

    NotificationPrefs notificationPrefs = preferences.getNotification();
    toastScreenComboBox.getSelectionModel().select(notificationPrefs.getToastScreen());
    notificationPrefs.toastScreenProperty()
        .bind(toastScreenComboBox.valueProperty().map(value -> Screen.getScreens().indexOf(value)));
  }

  @Override
  public Region getRoot() {
    return settingsRoot;
  }

  public void onSelectGameLocation() {
    gamePathHandler.chooseAndValidateGameDirectory();
  }

  public void onSelectVaultLocation() {
    vaultPathHandler.askForPathAndUpdate();
  }

  public void onSelectDataLocation() {
    platformService.askForPath(i18n.get("settings.data.select"))
                   .thenAccept(possiblePath -> possiblePath.ifPresent(newDataDirectory -> {
      log.info("User changed data directory to: `{}`", newDataDirectory);
      DataPrefs dataPrefs = preferences.getData();

      MoveDirectoryTask moveDirectoryTask = moveDirectoryTaskFactory.getObject();
      moveDirectoryTask.setNewDirectory(newDataDirectory);
      moveDirectoryTask.setOldDirectory(dataPrefs.getBaseDataDirectory());
      moveDirectoryTask.setAfterCopyAction(() -> dataPrefs.setBaseDataDirectory(newDataDirectory));
      taskService.submitTask(moveDirectoryTask);
                   }));
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
    WindowPrefs windowPrefs = preferences.getMainWindow();
    platformService.askForFile(i18n.get("settings.appearance.chooseImage"), windowPrefs.getBackgroundImagePath(),
            new ExtensionFilter(i18n.get("fileChooser.dialog.imageFiles"), "*.png", "*.jpg", "*.jpeg"))
        .ifPresent(windowPrefs::setBackgroundImagePath);
  }

  public void onUseNoBackgroundImage() {
    preferences.getMainWindow().setBackgroundImagePath(null);
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
    preferences.getChat().getAutoJoinChannels().add(channelTextField.getText());
    channelTextField.clear();
  }

  public void onUpdateDebuggerClicked() {
    DownloadFAFDebuggerTask downloadFAFDebuggerTask = downloadFAFDebuggerTaskFactory.getObject();
    taskService.submitTask(downloadFAFDebuggerTask).getFuture().exceptionally(throwable -> {
      useFAFDebuggerToggle.setSelected(false);
      notificationService.addImmediateErrorNotification(throwable, "settings.fa.updateDebugger.failed");
      return null;
    });
  }

  public void onClearCacheClicked() {
    DeleteDirectoryTask deleteDirectoryTask = deleteDirectoryTaskFactory.getObject();
    deleteDirectoryTask.setDirectory(preferences.getData().getCacheDirectory());

    taskService.submitTask(deleteDirectoryTask);
  }
}

