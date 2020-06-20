package com.faforever.client.preferences.ui;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.chat.ChatFormat;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.settings.LanguageItemController;
import com.faforever.client.theme.Theme;
import com.faforever.client.theme.UiService;
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
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
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

  private final NotificationService notificationService;
  private final UserService userService;
  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final ClientUpdateService clientUpdateService;

  public TextField executableDecoratorField;
  public TextField executionDirectoryField;
  public ToggleGroup colorModeToggleGroup;
  public Toggle customColorsToggle;
  public Toggle randomColorsToggle;
  public Toggle defaultColorsToggle;
  public Toggle hideFoeToggle;
  public Toggle forceRelayToggle;
  public TextField gameLocationTextField;
  public Toggle autoDownloadMapsToggle;
  public TextField maxMessagesTextField;
  public Toggle imagePreviewToggle;
  public Toggle enableNotificationsToggle;
  public Toggle enableSoundsToggle;
  public CheckBox displayFriendOnlineToastCheckBox;
  public CheckBox displayFriendOfflineToastCheckBox;
  public CheckBox playFriendOnlineSoundCheckBox;
  public CheckBox playFriendOfflineSoundCheckBox;
  public CheckBox displayFriendJoinsGameToastCheckBox;
  public CheckBox displayFriendPlaysGameToastCheckBox;
  public CheckBox playFriendJoinsGameSoundCheckBox;
  public CheckBox playFriendPlaysGameSoundCheckBox;
  public CheckBox displayPmReceivedToastCheckBox;
  public CheckBox displayLadder1v1ToastCheckBox;
  public CheckBox playPmReceivedSoundCheckBox;
  public CheckBox afterGameReviewCheckBox;
  public Region settingsRoot;
  public ComboBox<Theme> themeComboBox;
  public ToggleGroup toastPositionToggleGroup;
  public ComboBox<Screen> toastScreenComboBox;
  public ToggleButton bottomLeftToastButton;
  public ToggleButton topRightToastButton;
  public ToggleButton topLeftToastButton;
  public ToggleButton bottomRightToastButton;
  public PasswordField currentPasswordField;
  public PasswordField newPasswordField;
  public PasswordField confirmPasswordField;
  public ComboBox<TimeInfo> timeComboBox;
  public ComboBox<ChatFormat> chatComboBox;
  public Label passwordChangeErrorLabel;
  public Label passwordChangeSuccessLabel;
  public ComboBox<UnitDataBaseType> unitDatabaseComboBox;
  public Toggle notifyOnAtMentionOnlyToggle;
  public Pane languagesContainer;
  public TextField backgroundImageLocation;
  public ToggleButton disallowJoinsCheckBox;
  public ToggleButton secondaryVaultLocationToggleButton;
  public Button autoJoinChannelsButton;
  public ToggleButton advancedIceLogToggleButton;
  public ToggleButton prereleaseToggleButton;
  private final InvalidationListener availableLanguagesListener;
  private Popup autojoinChannelsPopUp;
  private ChangeListener<Theme> selectedThemeChangeListener;
  private ChangeListener<Theme> currentThemeChangeListener;
  public ComboBox<NavigationItem> startTabChoiceBox;

  public SettingsController(UserService userService, PreferencesService preferencesService, UiService uiService,
                            I18n i18n, EventBus eventBus, NotificationService notificationService,
                            PlatformService platformService, ClientProperties clientProperties, ClientUpdateService clientUpdateService) {
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.notificationService = notificationService;
    this.platformService = platformService;
    this.clientProperties = clientProperties;
    this.clientUpdateService = clientUpdateService;

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

  private void setSelectedToastPosition(ToastPosition newValue) {
    switch (newValue) {
      case TOP_RIGHT:
        toastPositionToggleGroup.selectToggle(topRightToastButton);
        break;
      case BOTTOM_RIGHT:
        toastPositionToggleGroup.selectToggle(bottomRightToastButton);
        break;
      case BOTTOM_LEFT:
        toastPositionToggleGroup.selectToggle(bottomLeftToastButton);
        break;
      case TOP_LEFT:
        toastPositionToggleGroup.selectToggle(topLeftToastButton);
        break;
    }
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

    Preferences preferences = preferencesService.getPreferences();
    temporarilyDisableUnsupportedSettings(preferences);

    JavaFxUtil.bindBidirectional(maxMessagesTextField.textProperty(), preferences.getChat().maxMessagesProperty(), new NumberStringConverter(integerNumberFormat));
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
      if (newValue == customColorsToggle) {
        preferences.getChat().setChatColorMode(ChatColorMode.CUSTOM);
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

    JavaFxUtil.addListener(preferences.getNotification().toastPositionProperty(), (observable, oldValue, newValue) -> setSelectedToastPosition(newValue));
    setSelectedToastPosition(preferences.getNotification().getToastPosition());
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
    configureTimeSetting(preferences);
    configureChatSetting(preferences);
    configureLanguageSelection();
    configureThemeSelection();
    configureToastScreen(preferences);
    configureStartTab(preferences);

    displayFriendOnlineToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOnlineToastEnabledProperty());
    displayFriendOfflineToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOfflineToastEnabledProperty());
    displayFriendJoinsGameToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendJoinsGameToastEnabledProperty());
    displayFriendPlaysGameToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendPlaysGameToastEnabledProperty());
    displayPmReceivedToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().privateMessageToastEnabledProperty());
    displayLadder1v1ToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().ladder1v1ToastEnabledProperty());
    playFriendOnlineSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOnlineSoundEnabledProperty());
    playFriendOfflineSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOfflineSoundEnabledProperty());
    playFriendJoinsGameSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendJoinsGameSoundEnabledProperty());
    playFriendPlaysGameSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendPlaysGameSoundEnabledProperty());
    playPmReceivedSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().privateMessageSoundEnabledProperty());
    afterGameReviewCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().afterGameReviewEnabledProperty());

    notifyOnAtMentionOnlyToggle.selectedProperty().bindBidirectional(preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty());
    enableSoundsToggle.selectedProperty().bindBidirectional(preferences.getNotification().soundsEnabledProperty());
    forceRelayToggle.selectedProperty().bindBidirectional(preferences.getForgedAlliance().forceRelayProperty());
    gameLocationTextField.textProperty().bindBidirectional(preferences.getForgedAlliance().installationPathProperty(), PATH_STRING_CONVERTER);
    autoDownloadMapsToggle.selectedProperty().bindBidirectional(preferences.getForgedAlliance().autoDownloadMapsProperty());

    executableDecoratorField.textProperty().bindBidirectional(preferences.getForgedAlliance().executableDecoratorProperty());
    executionDirectoryField.textProperty().bindBidirectional(preferences.getForgedAlliance().executionDirectoryProperty(), PATH_STRING_CONVERTER);

    backgroundImageLocation.textProperty().bindBidirectional(preferences.getMainWindow().backgroundImagePathProperty(), PATH_STRING_CONVERTER);

    passwordChangeErrorLabel.setVisible(false);
    addAutoJoinChannelsPopup();

    secondaryVaultLocationToggleButton.setSelected(preferences.getForgedAlliance().getVaultBaseDirectory().equals(preferencesService.getSecondaryVaultLocation()));
    secondaryVaultLocationToggleButton.selectedProperty().addListener(observable -> {
      Path vaultBaseDirectory = secondaryVaultLocationToggleButton.isSelected() ? preferencesService.getSecondaryVaultLocation() : preferencesService.getPrimaryVaultLocation();
      preferences.getForgedAlliance().setVaultBaseDirectory(vaultBaseDirectory);
    });

    advancedIceLogToggleButton.selectedProperty().bindBidirectional(preferences.advancedIceLogEnabledProperty());

    prereleaseToggleButton.selectedProperty().bindBidirectional(preferences.prereleaseCheckEnabledProperty());
    prereleaseToggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null && newValue && (oldValue == null || !oldValue)) {
        clientUpdateService.checkForUpdateInBackground();
      }
    });

    initUnitDatabaseSelection(preferences);
  }

  private void configureStartTab(Preferences preferences) {
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

  private void initUnitDatabaseSelection(Preferences preferences) {
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

  private void configureTimeSetting(Preferences preferences) {
    timeComboBox.setButtonCell(new StringListCell<>(timeInfo -> i18n.get(timeInfo.getDisplayNameKey())));
    timeComboBox.setCellFactory(param -> new StringListCell<>(timeInfo -> i18n.get(timeInfo.getDisplayNameKey())));
    timeComboBox.setItems(FXCollections.observableArrayList(TimeInfo.values()));
    timeComboBox.setDisable(false);
    timeComboBox.setFocusTraversable(true);
    timeComboBox.getSelectionModel().select(preferences.getChat().getTimeFormat());
  }

  public void onTimeFormatSelected() {
    log.debug("A new time format was selected: {}", timeComboBox.getValue());
    Preferences preferences = preferencesService.getPreferences();
    preferences.getChat().setTimeFormat(timeComboBox.getValue());
    preferencesService.storeInBackground();
  }


  private void configureChatSetting(Preferences preferences) {
    chatComboBox.setButtonCell(new StringListCell<>(chatFormat -> i18n.get(chatFormat.getI18nKey())));
    chatComboBox.setCellFactory(param -> new StringListCell<>(chatFormat -> i18n.get(chatFormat.getI18nKey())));
    chatComboBox.setItems(FXCollections.observableArrayList(ChatFormat.values()));
    chatComboBox.getSelectionModel().select(preferences.getChat().getChatFormat());
  }

  public void onChatFormatSelected() {
    log.debug("A new chat format was selected: {}", chatComboBox.getValue());
    Preferences preferences = preferencesService.getPreferences();
    preferences.getChat().setChatFormat(chatComboBox.getValue());
    preferencesService.storeInBackground();
  }

  private StringListCell<Screen> screenListCell() {
    return new StringListCell<>(screen -> i18n.get("settings.screenFormat", Screen.getScreens().indexOf(screen) + 1));
  }

  private void setSelectedColorMode(ChatColorMode newValue) {
    switch (newValue) {
      case DEFAULT:
        colorModeToggleGroup.selectToggle(defaultColorsToggle);
        break;
      case CUSTOM:
        colorModeToggleGroup.selectToggle(customColorsToggle);
        break;
      case RANDOM:
        colorModeToggleGroup.selectToggle(randomColorsToggle);
        break;
    }
  }

  private void configureThemeSelection() {
    themeComboBox.setItems(FXCollections.observableArrayList(uiService.getAvailableThemes()));

    themeComboBox.getSelectionModel().select(uiService.getCurrentTheme());

    themeComboBox.getSelectionModel().selectedItemProperty().addListener(selectedThemeChangeListener);
    JavaFxUtil.addListener(uiService.currentThemeProperty(), new WeakChangeListener<>(currentThemeChangeListener));
  }

  private void configureLanguageSelection() {
    i18n.getAvailableLanguages().addListener(new WeakInvalidationListener(availableLanguagesListener));
    availableLanguagesListener.invalidated(i18n.getAvailableLanguages());
  }

  @VisibleForTesting
  void onLanguageSelected(Locale locale) {
    LocalizationPrefs localizationPrefs = preferencesService.getPreferences().getLocalization();
    if (Objects.equals(locale, localizationPrefs.getLanguage())) {
      return;
    }
    log.debug("A new language was selected: {}", locale);
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

  private void configureToastScreen(Preferences preferences) {
    toastScreenComboBox.getSelectionModel().select(preferences.getNotification().getToastScreen());
    preferences.getNotification().toastScreenProperty().bind(Bindings.createIntegerBinding(()
        -> Screen.getScreens().indexOf(toastScreenComboBox.getValue()), toastScreenComboBox.valueProperty()));
  }

  public Region getRoot() {
    return settingsRoot;
  }

  public void onSelectGameLocation() {
    eventBus.post(new GameDirectoryChooseEvent());
  }

  public void onSelectExecutionDirectory() {
    // TODO implement
  }

  public void onChangePasswordClicked() {
    passwordChangeSuccessLabel.setVisible(false);
    passwordChangeErrorLabel.setVisible(false);

    if (currentPasswordField.getText().isEmpty()) {
      passwordChangeErrorLabel.setVisible(true);
      passwordChangeErrorLabel.setText(i18n.get("settings.account.currentPassword.empty"));
      return;
    }

    if (newPasswordField.getText().isEmpty()) {
      passwordChangeErrorLabel.setVisible(true);
      passwordChangeErrorLabel.setText(i18n.get("settings.account.newPassword.empty"));
      return;
    }

    if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
      passwordChangeErrorLabel.setVisible(true);
      passwordChangeErrorLabel.setText(i18n.get("settings.account.confirmPassword.mismatch"));
      return;
    }

    userService.changePassword(currentPasswordField.getText(), newPasswordField.getText()).getFuture()
        .thenAccept(aVoid -> {
          passwordChangeSuccessLabel.setVisible(true);
          currentPasswordField.setText("");
          newPasswordField.setText("");
          confirmPasswordField.setText("");
        }).exceptionally(throwable -> {
          passwordChangeErrorLabel.setVisible(true);
          passwordChangeErrorLabel.setText(i18n.get("settings.account.changePassword.error", throwable.getCause().getLocalizedMessage()));
          return null;
        }
    );
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

  public void onAutoJoinChannelsButtonClicked(ActionEvent actionEvent) {
    if (autojoinChannelsPopUp.isShowing()) {
      autojoinChannelsPopUp.hide();
      return;
    }

    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = autoJoinChannelsButton.localToScreen(autoJoinChannelsButton.getBoundsInLocal());
    autojoinChannelsPopUp.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  private void addAutoJoinChannelsPopup() {
    autojoinChannelsPopUp = new Popup();
    autojoinChannelsPopUp.setAutoFix(false);
    autojoinChannelsPopUp.setAutoHide(true);

    AutoJoinChannelsController autoJoinChannelsController = uiService.loadFxml("theme/settings/auto_join_channels.fxml");
    autojoinChannelsPopUp.getContent().setAll(autoJoinChannelsController.getRoot());
  }

  public void onSelectBackgroundImage() {
    Platform.runLater(() -> {
      FileChooser directoryChooser = new FileChooser();
      directoryChooser.setTitle(i18n.get("settings.appearance.chooseImage"));
      File result = directoryChooser.showOpenDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      preferencesService.getPreferences().getMainWindow().setBackgroundImagePath(result.toPath());
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
}

