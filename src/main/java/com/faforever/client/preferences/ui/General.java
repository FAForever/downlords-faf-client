package com.faforever.client.preferences.ui;

import com.faforever.client.fx.StringListCell;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.Theme;
import com.faforever.client.theme.ThemeService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.faforever.client.theme.ThemeServiceImpl.DEFAULT_THEME;

public class General {
  @FXML
  ComboBox<String> languageComboBox;
  @FXML
  ComboBox<Theme> themeComboBox;
  @FXML
  CheckBox rememberLastTabCheckBox;

  @Resource
  ThemeService themeService;
  @Resource
  PreferencesService preferencesService;

  private ChangeListener<Theme> themeChangeListener;

  @FXML
  void initialize() {
    themeComboBox.setButtonCell(new StringListCell<>(Theme::getDisplayName));
    themeComboBox.setCellFactory(param -> new StringListCell<>(Theme::getDisplayName));
  }

  @PostConstruct
  void postConstruct() {

    themeChangeListener = new ChangeListener<Theme>() {
      @Override
      public void changed(ObservableValue<? extends Theme> observable, Theme oldValue, Theme newValue) {
        if (observable == themeComboBox.getSelectionModel().selectedItemProperty()) {
          themeComboBox.getSelectionModel().select(newValue);
        }
      }
    };

    Preferences preferences = preferencesService.getPreferences();
    configureLanguageSelection();
    configureThemeSelection(preferences);
    configureRememberLastTab(preferences);
  }

  private void configureRememberLastTab(Preferences preferences) {
    rememberLastTabCheckBox.selectedProperty().bindBidirectional(preferences.rememberLastTabProperty());
  }

  private void configureThemeSelection(Preferences preferences) {
    themeComboBox.setItems(FXCollections.observableArrayList(themeService.getAvailableThemes()));
    themeComboBox.getSelectionModel().selectedItemProperty().addListener(new WeakChangeListener<Theme>(themeChangeListener));

    Theme currentTheme = themeComboBox.getItems().stream()
        .filter(theme -> theme.getDisplayName().equals(preferences.getThemeName()))
        .findFirst().orElse(DEFAULT_THEME);
    themeComboBox.getSelectionModel().select(currentTheme);

    themeService.currentThemeProperty().addListener(
        (observable, oldValue, newValue) -> themeComboBox.getSelectionModel().select(newValue)
    );
  }

  private void configureLanguageSelection() {
    languageComboBox.setItems(FXCollections.singletonObservableList("English"));
    languageComboBox.getSelectionModel().select(0);
  }
}
