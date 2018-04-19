package com.faforever.client.preferences.ui;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.settings.LanguageItemController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SettingsControllerTest extends AbstractPlainJavaFxTest {

  private SettingsController instance;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferenceService;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ClientProperties clientProperties;

  private Preferences preferences;
  private SimpleListProperty<Locale> availableLanguages;

  @Before
  public void setUp() throws Exception {
    preferences = new Preferences();
    when(preferenceService.getPreferences()).thenReturn(preferences);
    when(uiService.currentThemeProperty()).thenReturn(new SimpleObjectProperty<>());

    availableLanguages = new SimpleListProperty<>(FXCollections.observableArrayList());
    when(i18n.getAvailableLanguages()).thenReturn(availableLanguages);

    instance = new SettingsController(userService, preferenceService, uiService, i18n, eventBus, notificationService, platformService, clientProperties);
    loadFxml("theme/settings/settings.fxml", param -> instance);
  }

  @Test
  public void testOnLanguageSelected() {
    preferences.getLocalization().setLanguage(Locale.US);
    instance.onLanguageSelected(Locale.GERMAN);

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnLanguageSelectedThatIsAlreadySet() {
    preferences.getLocalization().setLanguage(Locale.GERMAN);
    instance.onLanguageSelected(Locale.GERMAN);

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnTimeSelected() {
    instance.timeComboBox.setValue(TimeInfo.AUTO);

    instance.onTimeFormatSelected();

    assertThat(preferences.getChat().getTimeFormat(), is(instance.timeComboBox.getValue()));
  }

  @Test
  public void testAvailableLanguagesChange() {
    LanguageItemController languageItemController = mock(LanguageItemController.class);
    when(languageItemController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/settings/language_item.fxml")).thenReturn(languageItemController);

    availableLanguages.setAll(Locale.FRENCH);

    verify(languageItemController).setLocale(Locale.FRENCH);
    verify(languageItemController).setOnSelectedListener(any());
    verify(uiService).loadFxml("theme/settings/language_item.fxml");
  }
}
