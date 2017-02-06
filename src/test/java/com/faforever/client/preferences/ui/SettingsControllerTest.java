package com.faforever.client.preferences.ui;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.LanguageInfo;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    preferences = new Preferences();
    when(preferenceService.getPreferences()).thenReturn(preferences);
    when(uiService.currentThemeProperty()).thenReturn(new SimpleObjectProperty<>());
    when(userService.currentUserProperty()).thenReturn(new SimpleStringProperty());

    instance = new SettingsController(userService, preferenceService, uiService, i18n, eventBus, notificationService);
    loadFxml("theme/settings/settings.fxml", param -> instance);
  }

  @Test
  public void testOnLanguageSelected() throws Exception {
    instance.languageComboBox.setValue(LanguageInfo.AUTO);
    preferences.getLocalization().setLanguage(LanguageInfo.DE);

    instance.onLanguageSelected();

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnLanguageSelectedThatIsAlreadySet() throws Exception {
    instance.languageComboBox.setValue(LanguageInfo.DE);
    preferences.getLocalization().setLanguage(LanguageInfo.DE);

    instance.onLanguageSelected();

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnTimeSelected() throws Exception {
    instance.timeComboBox.setValue(TimeInfo.AUTO);

    instance.onTimeFormatSelected();

    assertThat(preferences.getChat().getTimeFormat(), is(instance.timeComboBox.getValue()));
  }
}
