package com.faforever.client.preferences.ui;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.ToastPosition;
import com.faforever.client.theme.Theme;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.user.UserService;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.HashMap;

import static com.faforever.client.fx.JavaFxUtil.PATH_STRING_CONVERTER;
import static com.faforever.client.theme.ThemeService.DEFAULT_THEME;

public class SettingsController {

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
  public CheckBox displayRanked1v1ToastCheckBox;
  public CheckBox playPmReceivedSoundCheckBox;
  public Region settingsRoot;
  public ComboBox<String> languageComboBox;
  public ComboBox<Theme> themeComboBox;
  public CheckBox rememberLastTabCheckBox;
  public ComboBox<String> timeComboBox;
  public ToggleGroup toastPosition;
  public ComboBox<Screen> toastScreenComboBox;
  public ToggleButton bottomLeftToastButton;
  public ToggleButton topRightToastButton;
  public ToggleButton topLeftToastButton;
  public ToggleButton bottomRightToastButton;
  public TextField usernameField;
  public PasswordField currentPasswordField;
  public PasswordField newPasswordField;
  public PasswordField confirmPasswordField;
  public Label passwordChangeErrorLabel;
  public Label passwordChangeSuccessLabel;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Resource
  UserService userService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ThemeService themeService;
  @Resource
  I18n i18n;

  private ChangeListener<Theme> themeChangeListener;
  public static String [] languagesAvailable;
  public static final String [] languageCodes={null,"en","de"};
  private String[] options;

  @PostConstruct
  void postConstruct() {
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
    configureTimeSelection(preferences);
    configureLanguageSelection(preferences);
    configureThemeSelection(preferences);
    configureRememberLastTab(preferences);
    configureToastScreen(preferences);

    displayFriendOnlineToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOnlineToastEnabledProperty());
    displayFriendOfflineToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOfflineToastEnabledProperty());
    displayFriendJoinsGameToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendJoinsGameToastEnabledProperty());
    displayFriendPlaysGameToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendPlaysGameToastEnabledProperty());
    displayPmReceivedToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().privateMessageToastEnabledProperty());
    displayRanked1v1ToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().ranked1v1ToastEnabledProperty());
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

    usernameField.textProperty().bind(userService.currentUserProperty());
    passwordChangeErrorLabel.setVisible(false);

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
    themeComboBox.setButtonCell(new StringListCell<>(Theme::getDisplayName));
    themeComboBox.setCellFactory(param -> new StringListCell<>(Theme::getDisplayName));

    toastScreenComboBox.setButtonCell(screenListCell());
    toastScreenComboBox.setCellFactory(param -> screenListCell());
    toastScreenComboBox.setItems(Screen.getScreens());
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
  private void configureTimeSelection(Preferences preferences) {
    options=i18n.get("settings.chat.optionsForTime").split(" ");
    timeComboBox.setItems(FXCollections.observableArrayList(options));
    timeComboBox.setOnAction(new EventHandler<ActionEvent>() {
                                   @Override
                                   public void handle(ActionEvent event) {
                                     newTimneFormatSelected(event);
                                   }
                                 }
    );
    timeComboBox.setDisable(false);
    timeComboBox.setFocusTraversable(true);
    int index;


    index= getIndexNumberOfFormat(preferences.getChat().getMilitaryTime());


    timeComboBox.getSelectionModel().select(index);

  }

  private int getIndexNumberOfFormat(String militaryTime) {
    switch (militaryTime) {
      case ("system"):
        return 0;
      case("yes"):
        return 1;
      case("no"):
        return 2;
    }
    return 0;

  }

  private void newTimneFormatSelected(ActionEvent event) {
    HashMap<String,String> saveCodes= new HashMap<>();
    saveCodes.put(options[0],"system");
    saveCodes.put(options[1],"yes");
    saveCodes.put(options[2],"no");
    logger.info("newTimeFormat is "+timeComboBox.getValue().toString()+" at index "+getIndexNumberOfFormat(timeComboBox.getValue()));
    Preferences preferences= preferencesService.getPreferences();

    String selectedFormat= saveCodes.get(timeComboBox.getValue().toString());
    preferences.getChat().setMilitaryTime(selectedFormat);
    preferencesService.storeInBackground();
    logger.info("saving.....Time Format");
  }

  private void configureThemeSelection(Preferences preferences) {
    themeComboBox.setItems(FXCollections.observableArrayList(themeService.getAvailableThemes()));
    themeComboBox.getSelectionModel().selectedItemProperty().addListener(new WeakChangeListener<>(themeChangeListener));

    Theme currentTheme = themeComboBox.getItems().stream()
        .filter(theme -> theme.getDisplayName().equals(preferences.getThemeName()))
        .findFirst().orElse(DEFAULT_THEME);
    themeComboBox.getSelectionModel().select(currentTheme);

    themeService.currentThemeProperty().addListener(
        (observable, oldValue, newValue) -> themeComboBox.getSelectionModel().select(newValue)
    );
  }

  private void configureLanguageSelection(Preferences preferences) {
    languagesAvailable=i18n.get("settings.languages").split(" ");

    languageComboBox.setItems(FXCollections.observableArrayList(languagesAvailable));

    languageComboBox.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        newLanguageSelected(event);
      }
    }
  );
    languageComboBox.setDisable(false);
    languageComboBox.setFocusTraversable(true);
    int index;


    index= getIndexNumberOfCountryCode(preferences.getLanguagePrefs().getLanguage());


    languageComboBox.getSelectionModel().select(index);


  }
  public int getIndexNumberOfCountryCode(String languageCode)
  {
    if (languageCode!=null) {

      for (int i = 1; i != languageCodes.length; i++) {
        if (languageCodes[i].equals(languageCode)) return i;
      }
    }
    return 0;
  }
  public int getIndexNumberOfCountry(String language)
  {
    for (int i=0; i!= languagesAvailable.length;i++)
    {
      if  (languagesAvailable[i].equals(language)) return i;
    }

    return 0;
  }
  public void newLanguageSelected(ActionEvent actionEvent)
  {
    logger.info("newLanguage is "+languageComboBox.getValue().toString()+" at index "+getIndexNumberOfCountry(languageComboBox.getValue())+" language code is "+languageCodes[getIndexNumberOfCountry(languageComboBox.getValue())]);
    Preferences preferences= preferencesService.getPreferences();

    String selectedLanguage=languageCodes[getIndexNumberOfCountry(languageComboBox.getValue())];
    preferences.getLanguagePrefs().setLanguage(selectedLanguage);
    preferencesService.storeInBackground();
    logger.info("saving.....Language");
    //languageCodes[getIndexNumberOfCountry(languageComboBox.getValue())]
    //TODO: advise the user to restart programm and delete System.out.print

  }
  private void configureToastScreen(Preferences preferences) {
    preferences.getNotification().toastScreenProperty().bind(Bindings.createIntegerBinding(()
        -> Screen.getScreens().indexOf(toastScreenComboBox.getValue()), toastScreenComboBox.valueProperty()));
    toastScreenComboBox.getSelectionModel().select(preferences.getNotification().getToastScreen());
  }

  public Region getRoot() {
    return settingsRoot;
  }

  public void onSelectGameLocation() {
    preferencesService.letUserChooseGameDirectory();
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
