package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModManagerController;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateGameControllerTest extends AbstractPlainJavaFxTest {


  private static final KeyEvent keyUpPressed = new KeyEvent(KeyEvent.KEY_PRESSED, "0", "", KeyCode.UP, false, false, false, false);
  private static final KeyEvent keyUpReleased = new KeyEvent(KeyEvent.KEY_RELEASED, "0", "", KeyCode.UP, false, false, false, false);
  private static final KeyEvent keyDownPressed = new KeyEvent(KeyEvent.KEY_PRESSED, "0", "", KeyCode.DOWN, false, false, false, false);
  private static final KeyEvent keyDownReleased = new KeyEvent(KeyEvent.KEY_RELEASED, "0", "", KeyCode.DOWN, false, false, false, false);

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
  @Mock
  private
  GameService gameService;
  @Mock
  private
  NotificationService notificationService;
  @Mock
  private
  ReportingService reportingService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private FafService fafService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private ModManagerController modManagerController;

  private Preferences preferences;
  private CreateGameController instance;
  private ObservableList<MapBean> mapList;

  @Before
  public void setUp() throws Exception {
    instance = new CreateGameController(mapService, modService, gameService, preferencesService, i18n, notificationService, reportingService, fafService, mapGeneratorService, uiService);

    mapList = FXCollections.observableArrayList();

    preferences = new Preferences();
    preferences.getForgedAlliance().setInstallationPath(Paths.get("."));
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(mapService.getInstalledMaps()).thenReturn(mapList);
    when(modService.getFeaturedMods()).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(mapService.loadPreview(anyString(), any())).thenReturn(new Image("/theme/images/close.png"));
    when(i18n.get(any(), any())).then(invocation -> invocation.getArgument(0));
    when(i18n.number(anyInt())).then(invocation -> invocation.getArgument(0).toString());
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));

    loadFxml("theme/play/create_game.fxml", clazz -> {
      if (clazz.equals(ModManagerController.class)) {
        return modManagerController;
      }
      return instance;
    });
  }

  @Test
  public void testMapSearchTextFieldFilteringEmpty() {
    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMapBeans.getSource(), empty());
  }

  @Test
  public void testMapSearchTextFieldFilteringPopulated() {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().folderName("test2").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("foo").get());

    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMapBeans.get(0).getFolderName(), is("test2"));
    assertThat(instance.filteredMapBeans.get(1).getDisplayName(), is("Test1"));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForEmpty() {
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForEmpty() {
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForPopulated() {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(0));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForPopulated() {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(1));
  }

  @Test
  public void testSetLastGameTitle() {
    preferences.getLastGamePrefs().setLastGameTitle("testGame");
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is("testGame"));
  }


  @Test
  public void testButtonBindingIfFeaturedModNotSet() {
    preferences.getLastGamePrefs().setLastGameTitle("123");
    when(i18n.get("game.create.featuredModMissing")).thenReturn("Mod missing");
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is("123"));
    assertThat(instance.createGameButton.getText(), is("Mod missing"));
  }

  @Test
  public void testButtonBindingIfTitleNotSet() {

    when(i18n.get("game.create.titleMissing")).thenReturn("title missing");
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("title missing"));
  }

  @Test
  public void testButtonBindingIfNotConnected() {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.DISCONNECTED));
    when(i18n.get("game.create.disconnected")).thenReturn("disconnected");
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("disconnected"));
  }

  @Test
  public void testButtonBindingIfNotConnecting() {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTING));
    when(i18n.get("game.create.connecting")).thenReturn("connecting");
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("connecting"));
  }

  @Test
  public void testSelectLastMap() {
    MapBean lastMapBean = MapBuilder.create().defaultValues().folderName("foo").get();
    preferences.getLastGamePrefs().setLastMap("foo");

    mapList.add(MapBuilder.create().defaultValues().folderName("Test1").get());
    mapList.add(lastMapBean);

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMapBean));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = new CreateGameController(mapService, modService, gameService, preferencesService, i18n, notificationService,
        reportingService, fafService, mapGeneratorService, uiService);

    loadFxml("theme/play/create_game.fxml", clazz -> {
      if (clazz.equals(ModManagerController.class)) {
        return modManagerController;
      }
      return instance;
    });

    assertThat(instance.featuredModListView.getItems(), empty());
  }

  @Test
  public void testCreateGame() {
    ArgumentCaptor<NewGameInfo> newGameInfoArgumentCaptor = ArgumentCaptor.forClass(NewGameInfo.class);
    ModVersion modVersion = new ModVersion();
    String uidMod = "junit-mod";
    modVersion.setUid(uidMod);
    when(modManagerController.apply()).thenReturn(Collections.singletonList(modVersion));

    Runnable closeRunnable = mock(Runnable.class);
    instance.setOnCloseButtonClickedListener(closeRunnable);

    when(gameService.hostGame(newGameInfoArgumentCaptor.capture())).thenReturn(CompletableFuture.completedFuture(null));

    String mapFolderName = "junit-map-folder";
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").folderName(mapFolderName).get());
    instance.mapListView.getSelectionModel().select(0);

    instance.onCreateButtonClicked();

    verify(modManagerController).apply();
    verify(closeRunnable).run();

    assertThat(newGameInfoArgumentCaptor.getValue().getSimMods(), contains(uidMod));
    assertThat(newGameInfoArgumentCaptor.getValue().getMap(), is(mapFolderName));
  }

  @Test
  public void testInitGameTypeComboBoxPostPopulated() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMods()).thenReturn(completedFuture(singletonList(featuredMod)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getItems(), hasSize(1));
    assertThat(instance.featuredModListView.getItems().get(0), is(featuredMod));
  }

  @Test
  public void testSelectLastOrDefaultSelectDefault() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("something").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.getLastGamePrefs().setLastGameType(null);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredMod2));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("last").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.getLastGamePrefs().setLastGameType("last");
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredMod));
  }

  @Test
  public void testOnlyFriendsBinding() {
    instance.onlyForFriendsCheckBox.setSelected(true);
    assertThat(preferences.getLastGamePrefs().isLastGameOnlyFriends(), is(true));
    instance.onlyForFriendsCheckBox.setSelected(false);
    assertThat(preferences.getLastGamePrefs().isLastGameOnlyFriends(), is(false));
  }

  @Test
  public void testPasswordIsSaved() {
    instance.passwordTextField.setText("1234");
    assertEquals(preferences.getLastGamePrefs().getLastGamePassword(), "1234");
    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testCreateGameTitleTextBorderColor() {
    PseudoClass invalidClass = PseudoClass.getPseudoClass("invalid");
    instance.titleTextField.setText("Test");
    assertThat(instance.titleTextField.getPseudoClassStates().contains(invalidClass), is(false));
    instance.titleTextField.setText("");
    assertThat(instance.titleTextField.getPseudoClassStates().contains(invalidClass), is(true));
  }

}
