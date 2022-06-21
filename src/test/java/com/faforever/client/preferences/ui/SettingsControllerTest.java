package com.faforever.client.preferences.ui;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.debugger.DownloadFAFDebuggerTask;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.CoturnHostPort;
import com.faforever.client.preferences.LanguageChannel;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.TimeInfo;
import com.faforever.client.preferences.tasks.DeleteDirectoryTask;
import com.faforever.client.preferences.tasks.MoveDirectoryTask;
import com.faforever.client.settings.LanguageItemController;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.Theme;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.update.ClientUpdateService;
import com.faforever.client.user.UserService;
import com.faforever.commons.api.dto.CoturnServer;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SettingsControllerTest extends UITest {
  private static final Theme FIRST_THEME = new Theme("First", "none", 1, "1");
  private static final Theme SECOND_THEME = new Theme("Second", "none", 1, "1");

  @InjectMocks
  private SettingsController instance;
  @Mock
  private ApplicationContext applicationContext;
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
  private GameService gameService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private ClientUpdateService clientUpdateService;
  @Mock
  private TaskService taskService;
  @Mock
  private CoturnService coturnService;
  @Spy
  private IceServerMapper iceServerMapper = Mappers.getMapper(IceServerMapper.class);

  private Preferences preferences;
  private SimpleSetProperty<Locale> availableLanguages;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(iceServerMapper);
    preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferenceService.getPreferences()).thenReturn(preferences);
    when(uiService.currentThemeProperty()).thenReturn(new SimpleObjectProperty<>());
    when(uiService.getCurrentTheme())
        .thenReturn(FIRST_THEME);
    when(uiService.getAvailableThemes())
        .thenReturn(Arrays.asList(
            FIRST_THEME,
            SECOND_THEME
        ));
    CoturnServer coturnServer = new CoturnServer();
    coturnServer.setHost("Test");
    when(coturnService.getActiveCoturns()).thenReturn(CompletableFuture.completedFuture(List.of(coturnServer)));
    when(gameService.isGamePrefsPatchedToAllowMultiInstances()).thenReturn(CompletableFuture.completedFuture(true));

    availableLanguages = new SimpleSetProperty<>(FXCollections.observableSet());
    when(i18n.getAvailableLanguages()).thenReturn(new ReadOnlySetWrapper<>(availableLanguages));

    loadFxml("theme/settings/settings.fxml", param -> instance);
  }

  @Test
  public void testThemesDisplayed() throws Exception{
    assertThat(instance.themeComboBox.getSelectionModel().getSelectedItem(), is(FIRST_THEME));
    assertThat(instance.themeComboBox.getItems(), hasItem(FIRST_THEME));
    assertThat(instance.themeComboBox.getItems(), hasItem(SECOND_THEME));
  }

  @Test
  public void testSelectingSecondThemeCausesReloadAndRestartPrompt() throws Exception {
    when(uiService.doesThemeNeedRestart(SECOND_THEME)).thenReturn(true);
    instance.themeComboBox.getSelectionModel().select(SECOND_THEME);
    WaitForAsyncUtils.waitForFxEvents();
    verify(uiService).setTheme(SECOND_THEME);
    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testSelectingDefaultThemeDoesNotCausesRestartPrompt() throws Exception {
    when(uiService.doesThemeNeedRestart(SECOND_THEME)).thenReturn(false);
    instance.themeComboBox.getSelectionModel().select(SECOND_THEME);
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService, never()).addNotification(any(PersistentNotification.class));
    verify(uiService).setTheme(SECOND_THEME);
  }

  @Test
  public void testSearchForBetaUpdateIfOptionIsTurnedOn() throws Exception {
    instance.prereleaseToggle.setSelected(true);
    verify(clientUpdateService).checkForUpdateInBackground();
    instance.prereleaseToggle.setSelected(false);
    verifyNoMoreInteractions(clientUpdateService);
  }

  @Test
  public void testOnLanguageSelected() throws Exception {
    preferences.getLocalization().setLanguage(Locale.US);
    instance.onLanguageSelected(Locale.GERMAN);

    verify(notificationService).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnLanguageSelectedThatIsAlreadySet() throws Exception {
    preferences.getLocalization().setLanguage(Locale.GERMAN);
    instance.onLanguageSelected(Locale.GERMAN);

    verify(notificationService, never()).addNotification(any(PersistentNotification.class));
  }

  @Test
  public void testOnTimeSelected() throws Exception {
    instance.timeComboBox.setValue(TimeInfo.AUTO);

    instance.onTimeFormatSelected();

    assertThat(preferences.getChat().getTimeFormat(), is(instance.timeComboBox.getValue()));
  }

  @Test
  public void testAvailableLanguagesChange() throws Exception {
    LanguageItemController languageItemController = mock(LanguageItemController.class);
    when(languageItemController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/settings/language_item.fxml")).thenReturn(languageItemController);

    availableLanguages.clear();
    availableLanguages.addAll(Set.of(Locale.FRENCH));

    verify(languageItemController).setLocale(Locale.FRENCH);
    verify(languageItemController).setOnSelectedListener(any());
    verify(uiService).loadFxml("theme/settings/language_item.fxml");
  }

  @Test
  public void testOnAddChannelButtonPressed() throws Exception {
    preferences.getChat().getAutoJoinChannels().clear();
    instance.channelTextField.setText("#newbie");
    instance.onAddAutoChannel();
    List<String> expected = Collections.singletonList("#newbie");
    assertThat(preferences.getChat().getAutoJoinChannels(), is(expected));
  }

  @Test
  public void testLanguageChannels() throws Exception {
    Map<Locale, LanguageChannel> languagesToChannels = ChatPrefs.LOCALE_LANGUAGES_TO_CHANNELS;
    Entry<Locale, LanguageChannel> firstEntry = languagesToChannels.entrySet().iterator().next();
    Locale.setDefault(firstEntry.getKey());

    List<String> expected = Collections.singletonList(firstEntry.getValue().getChannelName());
    preferences.getChat().getAutoJoinChannels().setAll(expected);

    assertThat(instance.autoChannelListView.getItems(), is(expected));
  }

  @Test
  public void testOnAddMirrorButtonPressed() throws Exception {
    preferences.getMirror().getMirrorURLs().clear();
    instance.mirrorURITextField.setText("https://faf-mirror.example.com/");
    instance.onAddMirrorURL();
    List<URI> expected = Collections.singletonList(URI.create("https://faf-mirror.example.com/"));
    assertThat(preferences.getMirror().getMirrorURLs(), is(expected));
    assertThat(instance.mirrorURITextField.getText(), is(""));
  }

  @Test
  public void testCoturnSelected() throws Exception {
    ObservableSet<CoturnHostPort> preferredCoturnServers = preferences.getForgedAlliance().getPreferredCoturnServers();
    preferredCoturnServers.clear();
    runOnFxThreadAndWait(() -> instance.initialize());

    assertEquals(0, preferredCoturnServers.size());

    runOnFxThreadAndWait(() -> instance.preferredCoturnListView.getSelectionModel().select(0));

    assertEquals(1, preferredCoturnServers.size());
    assertTrue(preferredCoturnServers.contains(new CoturnHostPort("Test", null)));
    verify(preferenceService, times(4)).storeInBackground();

    runOnFxThreadAndWait(() -> instance.initialize());

    assertEquals(1, instance.preferredCoturnListView.getSelectionModel().getSelectedItems().size());
  }

  @Test
  public void testOnAddMirrorButtonPressedDuplicate() throws Exception {
    preferences.getMirror().getMirrorURLs().clear();
    instance.mirrorURITextField.setText("https://faf-mirror.example.com");
    instance.onAddMirrorURL();
    assertThat(instance.mirrorURITextField.getText(), is(""));
    instance.mirrorURITextField.setText("https://faf-mirror.example.com");
    instance.onAddMirrorURL();
    assertThat(instance.mirrorURITextField.getText(), is("https://faf-mirror.example.com"));
    instance.onAddMirrorURL();
    List<URI> expected = Collections.singletonList(URI.create("https://faf-mirror.example.com/"));
    assertThat(preferences.getMirror().getMirrorURLs(), is(expected));
    assertThat(instance.mirrorURITextField.getText(), is("https://faf-mirror.example.com"));
  }

  @Test
  public void testOnAddMirrorButtonPressedEmpty() throws Exception {
    preferences.getMirror().getMirrorURLs().clear();
    instance.mirrorURITextField.setText("");
    instance.onAddMirrorURL();
    assertThat(preferences.getMirror().getMirrorURLs(), is(Collections.emptyList()));
    assertThat(instance.mirrorURITextField.getText(), is(""));
  }

  @Test
  public void testOnAddMirrorButtonPressedBadUrl() throws Exception {
    preferences.getMirror().getMirrorURLs().clear();
    instance.mirrorURITextField.setText("faf-mirror.example.com");
    instance.onAddMirrorURL();
    assertThat(preferences.getMirror().getMirrorURLs(), is(Collections.emptyList()));
    assertThat(instance.mirrorURITextField.getText(), is("faf-mirror.example.com"));
    instance.mirrorURITextField.setText("spam://faf-mirror.example.com");
    instance.onAddMirrorURL();
    assertThat(preferences.getMirror().getMirrorURLs(), is(Collections.emptyList()));
    assertThat(instance.mirrorURITextField.getText(), is("spam://faf-mirror.example.com"));
  }

  @Test
  public void testSetVaultLocation() throws Exception {
    MoveDirectoryTask moveDirectoryTask = mock(MoveDirectoryTask.class);
    Path newVaultLocation = Path.of(".");
    when(platformService.askForPath(any())).thenReturn(Optional.of(newVaultLocation));
    when(applicationContext.getBean(MoveDirectoryTask.class)).thenReturn(moveDirectoryTask);

    instance.onSelectVaultLocation();

    verify(moveDirectoryTask).setOldDirectory(preferences.getForgedAlliance().getVaultBaseDirectory());
    verify(moveDirectoryTask).setNewDirectory(newVaultLocation);
    verify(platformService).askForPath(any());
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testSetDataLocation() throws Exception {
    MoveDirectoryTask moveDirectoryTask = mock(MoveDirectoryTask.class);
    Path newDataLocation = Path.of(".");
    when(platformService.askForPath(any())).thenReturn(Optional.of(newDataLocation));
    when(applicationContext.getBean(MoveDirectoryTask.class)).thenReturn(moveDirectoryTask);

    instance.onSelectDataLocation();

    verify(moveDirectoryTask).setOldDirectory(preferences.getData().getBaseDataDirectory());
    verify(moveDirectoryTask).setNewDirectory(newDataLocation);
    verify(platformService).askForPath(any());
    verify(taskService).submitTask(moveDirectoryTask);
  }

  @Test
  public void testSetGameLocation() throws Exception {
    instance.onSelectGameLocation();

    verify(eventBus).post(any(GameDirectoryChooseEvent.class));
  }

  @Test
  public void testClearCache() throws Exception {
    DeleteDirectoryTask deleteDirectoryTask = mock(DeleteDirectoryTask.class);
    when(applicationContext.getBean(DeleteDirectoryTask.class)).thenReturn(deleteDirectoryTask);
    when(taskService.submitTask(any(DeleteDirectoryTask.class))).thenReturn(deleteDirectoryTask);
    when(deleteDirectoryTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    instance.onClearCacheClicked();

    verify(taskService).submitTask(any(DeleteDirectoryTask.class));
  }

  @Test
  public void testSetFAFDebuggerOn() throws Exception {
    DownloadFAFDebuggerTask downloadFAFDebuggerTask = mock(DownloadFAFDebuggerTask.class);
    when(applicationContext.getBean(DownloadFAFDebuggerTask.class)).thenReturn(downloadFAFDebuggerTask);
    when(taskService.submitTask(any(DownloadFAFDebuggerTask.class))).thenReturn(downloadFAFDebuggerTask);
    when(downloadFAFDebuggerTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    instance.onUpdateDebuggerClicked();

    verify(taskService).submitTask(any(DownloadFAFDebuggerTask.class));
  }

  @Test
  public void testSetFAFDebuggerOnException() throws Exception {
    DownloadFAFDebuggerTask downloadFAFDebuggerTask = mock(DownloadFAFDebuggerTask.class);
    when(applicationContext.getBean(DownloadFAFDebuggerTask.class)).thenReturn(downloadFAFDebuggerTask);
    when(taskService.submitTask(any(DownloadFAFDebuggerTask.class))).thenReturn(downloadFAFDebuggerTask);
    when(downloadFAFDebuggerTask.getFuture()).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));
    instance.onUpdateDebuggerClicked();

    verify(taskService).submitTask(any(DownloadFAFDebuggerTask.class));
    verify(notificationService).addImmediateErrorNotification(any(FakeTestException.class), eq("settings.fa.updateDebugger.failed"));
  }
}
