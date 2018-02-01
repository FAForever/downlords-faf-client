package com.faforever.client.preferences.ui;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.LanguageInfo;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.theme.Theme;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Objects;

import static com.faforever.client.fx.JavaFxUtil.PATH_STRING_CONVERTER;
import static com.faforever.client.theme.UiService.DEFAULT_THEME;

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

  public TextField executableDecoratorField;
  public TextField executionDirectoryField;
  public ToggleGroup colorModeToggleGroup;
  public Toggle customColorsToggle;
  public Toggle randomColorsToggle;
  public Toggle defaultColorsToggle;
  public CheckBox hideFoeCheckBox;
  public TextField gamePortTextField;
  public TextField gameLocationTextField;
  public CheckBox autoDownloadMapsCheckBox;
  public TextField maxMessagesTextField;
  public CheckBox imagePreviewCheckBox;
  public CheckBox enableNotificationsCheckBox;
  public CheckBox enableSoundsCheckBox;
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
  public Region settingsRoot;
  public ComboBox<LanguageInfo> languageComboBox;
  public ComboBox<Theme> themeComboBox;
  public CheckBox rememberLastTabCheckBox;
  public ToggleGroup toastPosition;
  public ComboBox<Screen> toastScreenComboBox;
  public ToggleButton bottomLeftToastButton;
  public ToggleButton topRightToastButton;
  public ToggleButton topLeftToastButton;
  public ToggleButton bottomRightToastButton;
  public PasswordField currentPasswordField;
  public PasswordField newPasswordField;
  public PasswordField confirmPasswordField;
  public ComboBox<TimeInfo> timeComboBox;
  public Label passwordChangeErrorLabel;
  public Label passwordChangeSuccessLabel;
  public ComboBox<UnitDataBaseType> unitDatabaseComboBox;
  private ChangeListener<Theme> themeChangeListener;

  @Inject
  public SettingsController(UserService userService, PreferencesService preferencesService, UiService uiService,
                            I18n i18n, EventBus eventBus, NotificationService notificationService) {
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.notificationService = notificationService;
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
        toastPosition.selectToggle(topRightToastButton);
        break;
      case BOTTOM_RIGHT:
        toastPosition.selectToggle(bottomRightToastButton);
        break;
      case BOTTOM_LEFT:
        toastPosition.selectToggle(bottomLeftToastButton);
        break;
      case TOP_LEFT:
        toastPosition.selectToggle(topLeftToastButton);
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

    maxMessagesTextField.textProperty().bindBidirectional(preferences.getChat().maxMessagesProperty(), new NumberStringConverter(integerNumberFormat));
    imagePreviewCheckBox.selectedProperty().bindBidirectional(preferences.getChat().previewImageUrlsProperty());
    enableNotificationsCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().transientNotificationsEnabledProperty());

    hideFoeCheckBox.selectedProperty().bindBidirectional(preferences.getChat().hideFoeMessagesProperty());

    preferences.getChat().chatColorModeProperty().addListener((observable, oldValue, newValue) -> setSelectedColorMode(newValue));
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

    themeChangeListener = (observable, oldValue, newValue) -> {
      if (observable == themeComboBox.getSelectionModel().selectedItemProperty()) {
        themeComboBox.getSelectionModel().select(newValue);
      }
    };

    preferences.getNotification().toastPositionProperty().addListener((observable, oldValue, newValue) -> setSelectedToastPosition(newValue));
    setSelectedToastPosition(preferences.getNotification().getToastPosition());
    toastPosition.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
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
    configureLanguageSelection(preferences);
    configureThemeSelection(preferences);
    configureRememberLastTab(preferences);
    configureToastScreen(preferences);

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

    enableSoundsCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().soundsEnabledProperty());
    gamePortTextField.textProperty().bindBidirectional(preferences.getForgedAlliance().portProperty(), new NumberStringConverter(integerNumberFormat));
    gameLocationTextField.textProperty().bindBidirectional(preferences.getForgedAlliance().pathProperty(), PATH_STRING_CONVERTER);
    autoDownloadMapsCheckBox.selectedProperty().bindBidirectional(preferences.getForgedAlliance().autoDownloadMapsProperty());

    executableDecoratorField.textProperty().bindBidirectional(preferences.getForgedAlliance().executableDecoratorProperty());
    executionDirectoryField.textProperty().bindBidirectional(preferences.getForgedAlliance().executionDirectoryProperty(), PATH_STRING_CONVERTER);

    passwordChangeErrorLabel.setVisible(false);

    initUnitDatabaseSelection(preferences);
  }

  private void initUnitDatabaseSelection(Preferences preferences) {
    unitDatabaseComboBox.setButtonCell(new StringListCell<>(unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey())));
    unitDatabaseComboBox.setCellFactory(param -> new StringListCell<>(unitDataBaseType -> i18n.get(unitDataBaseType.getI18nKey())));
    unitDatabaseComboBox.setItems(FXCollections.observableArrayList(UnitDataBaseType.values()));
    unitDatabaseComboBox.setFocusTraversable(true);

    ChangeListener<UnitDataBaseType> unitDataBaseTypeChangeListener = (observable, oldValue, newValue) -> unitDatabaseComboBox.getSelectionModel().select(newValue);
    unitDataBaseTypeChangeListener.changed(null, null, preferences.getUnitDataBaseType());
    preferences.unitDataBaseTypeProperty().addListener(unitDataBaseTypeChangeListener);

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

  private void configureRememberLastTab(Preferences preferences) {
    rememberLastTabCheckBox.selectedProperty().bindBidirectional(preferences.rememberLastTabProperty());
  }

  private void configureThemeSelection(Preferences preferences) {
    themeComboBox.setItems(FXCollections.observableArrayList(uiService.getAvailableThemes()));
    themeComboBox.getSelectionModel().selectedItemProperty().addListener(new WeakChangeListener<>(themeChangeListener));

    Theme currentTheme = themeComboBox.getItems().stream()
        .filter(theme -> theme.getDisplayName().equals(preferences.getThemeName()))
        .findFirst().orElse(DEFAULT_THEME);
    themeComboBox.getSelectionModel().select(currentTheme);

    uiService.currentThemeProperty().addListener(
        (observable, oldValue, newValue) -> themeComboBox.getSelectionModel().select(newValue)
    );
  }

  private void configureLanguageSelection(Preferences preferences) {
    languageComboBox.setButtonCell(new StringListCell<>(languageInfo -> i18n.get(languageInfo.getDisplayNameKey())));
    languageComboBox.setCellFactory(param -> new StringListCell<>(languageInfo -> i18n.get(languageInfo.getDisplayNameKey())));
    languageComboBox.setItems(FXCollections.observableArrayList(LanguageInfo.values()));
    LanguageInfo languageInfo = preferences.getLocalization().getLanguage();
    languageComboBox.getSelectionModel().select(languageInfo.ordinal());
  }

  public void onLanguageSelected() {
    LocalizationPrefs localizationPrefs = preferencesService.getPreferences().getLocalization();
    if (Objects.equals(languageComboBox.getValue(), localizationPrefs.getLanguage())) {
      return;
    }
    log.debug("A new language was selected: {}", languageComboBox.getValue());
    localizationPrefs.setLanguage(languageComboBox.getValue());
    preferencesService.storeInBackground();
    notificationService.addNotification(new PersistentNotification(
        i18n.get("settings.languages.restart.message"),
        Severity.WARN,
        Collections.singletonList(new Action(i18n.get("settings.languages.restart"),
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

  public void onSelectExecutionDirectory(ActionEvent event) {
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
}
