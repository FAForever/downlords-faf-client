package com.faforever.client.preferences;

import com.faforever.client.chat.ChatColorMode;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.util.converter.NumberStringConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.NumberFormat;

public class SettingsController {

  @FXML
  ComboBox<ChatColorMode> colorMode;
  @FXML
  CheckBox hideFoeCheckBox;
  @FXML
  TextField gamePortTextField;
  @FXML
  TextField gameLocationTextField;
  @FXML
  Button gameLocationButton;
  @FXML
  CheckBox autoDownloadMapsCheckBox;
  @FXML
  ComboBox<String> languageComboBox;
  @FXML
  ComboBox<String> themeComboBox;
  @FXML
  CheckBox rememberLastTabCheckBox;
  @FXML
  Button resetNotificationsButton;
  @FXML
  TextField maxMessagesTextField;
  @FXML
  CheckBox imagePreviewCheckBox;
  @FXML
  CheckBox enableToastsCheckBox;
  @FXML
  ComboBox toastPositionComboBox;
  @FXML
  ComboBox toastScreenComboBox;
  @FXML
  CheckBox enableSoundsCheckBox;
  @FXML
  CheckBox displayFriendOnlineToastCheckBox;
  @FXML
  CheckBox displayFriendOfflineToastCheckBox;
  @FXML
  CheckBox playFriendOnlineSoundCheckBox;
  @FXML
  CheckBox playFriendOfflineSoundCheckBox;
  @FXML
  CheckBox displayFriendJoinsGameToastCheckBox;
  @FXML
  CheckBox displayFriendPlaysGameToastCheckBox;
  @FXML
  CheckBox playFriendJoinsGameSoundCheckBox;
  @FXML
  CheckBox playFriendPlaysGameSoundCheckBox;
  @FXML
  CheckBox displayPmReceivedToastCheckBox;
  @FXML
  CheckBox displayRanked1v1ToastCheckBox;
  @FXML
  CheckBox playPmReceivedSoundCheckBox;
  @FXML
  Region settingsRoot;

  @Resource
  PreferencesService preferencesService;

  @Autowired
  I18n i18n;

  @PostConstruct
  void init() {
    colorMode.getItems().addAll(ChatColorMode.values());
    colorMode.setCellFactory(param -> chatColorModeCell());
    colorMode.setButtonCell(chatColorModeCell());
  }

  @NotNull
  private ListCell<ChatColorMode> chatColorModeCell() {
    return new ListCell<ChatColorMode>() {
      @Override
      protected void updateItem(ChatColorMode item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(i18n.get(item.getI18nKey()));
        }
      }
    };
  }

  @PostConstruct
  void postConstruct() {
    NumberFormat integerNumberFormat = NumberFormat.getIntegerInstance();
    integerNumberFormat.setGroupingUsed(false);

    Preferences preferences = preferencesService.getPreferences();

    languageComboBox.setItems(FXCollections.singletonObservableList("English"));
    themeComboBox.setItems(FXCollections.singletonObservableList("Default"));

    rememberLastTabCheckBox.selectedProperty().bindBidirectional(preferences.rememberLastTabProperty());
    maxMessagesTextField.textProperty().bindBidirectional(preferences.getChat().maxMessagesProperty(), new NumberStringConverter(integerNumberFormat));
    imagePreviewCheckBox.selectedProperty().bindBidirectional(preferences.getChat().previewImageUrlsProperty());
    enableToastsCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().toastsEnabledProperty());

    hideFoeCheckBox.selectedProperty().bindBidirectional(preferences.getChat().hideFoeMessagesProperty());
    colorMode.valueProperty().bindBidirectional(preferences.getChat().chatColorModeProperty());

    displayFriendOnlineToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().displayFriendOnlineToastProperty());
    displayFriendOfflineToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().displayFriendOfflineToastProperty());
    displayFriendJoinsGameToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendJoinsGameToastEnabledProperty());
    displayFriendPlaysGameToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().displayFriendPlaysGameToastProperty());
    displayPmReceivedToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().privateMessageToastEnabledProperty());
    displayRanked1v1ToastCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().displayRanked1v1ToastProperty());
    playFriendOnlineSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOnlineSoundEnabledProperty());
    playFriendOfflineSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendOfflineSoundEnabledProperty());
    playFriendJoinsGameSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendJoinsGameSoundEnabledProperty());
    playFriendPlaysGameSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().friendPlaysGameSoundEnabledProperty());
    playPmReceivedSoundCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().privateMessageSoundEnabledProperty());

    enableSoundsCheckBox.selectedProperty().bindBidirectional(preferences.getNotification().soundsEnabledProperty());
    gamePortTextField.textProperty().bindBidirectional(preferences.getForgedAlliance().portProperty(), new NumberStringConverter(integerNumberFormat));
    gameLocationTextField.textProperty().bindBidirectional(preferences.getForgedAlliance().pathProperty(), JavaFxUtil.PATH_STRING_CONVERTER);
    autoDownloadMapsCheckBox.selectedProperty().bindBidirectional(preferences.getForgedAlliance().autoDownloadMapsProperty());
  }

  public Region getRoot() {
    return settingsRoot;
  }
}
