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
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private FafService fafService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private ModManagerController modManagerController;
  @Mock
  private GenerateMapController generateMapController;

  private Preferences preferences;
  private CreateGameController instance;
  private ObservableList<MapBean> mapList;
  private MapFilterController mapFilterController;

  @Before
  public void setUp() throws Exception {
    instance = new CreateGameController(mapService, modService, gameService, preferencesService, i18n, notificationService, fafService, mapGeneratorService, uiService);
    mapFilterController = new MapFilterController();

    mapList = FXCollections.observableArrayList();

    preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .installationPath(Paths.get(""))
        .then()
        .get();
    when(mapGeneratorService.downloadGeneratorIfNecessary(any())).thenReturn(completedFuture(null));
    when(mapGeneratorService.getGeneratorStyles()).thenReturn(completedFuture(List.of()));
    when(uiService.showInDialog(any(), any(), anyString())).thenReturn(new Dialog());
    when(uiService.loadFxml("theme/play/generate_map.fxml")).thenReturn(generateMapController);
    when(uiService.loadFxml("theme/play/map_filter.fxml")).thenReturn(mapFilterController);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(mapService.getInstalledMaps()).thenReturn(mapList);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(emptyList()));
    when(mapService.loadPreview(anyString(), any())).thenReturn(new Image("/theme/images/default_achievement.png"));
    when(i18n.get(any(), any())).then(invocation -> invocation.getArgument(0));
    when(i18n.number(anyInt())).then(invocation -> invocation.getArgument(0).toString());
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));

    loadFxml("theme/play/map_filter.fxml", clazz -> mapFilterController);
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
  public void showOnlyToFriendsRemembered() {
    instance.onlyForFriendsCheckBox.setSelected(true);
    assertThat(preferences.getLastGame().isLastGameOnlyFriends(), is(true));
    instance.onlyForFriendsCheckBox.setSelected(false);
    assertThat(preferences.getLastGame().isLastGameOnlyFriends(), is(false));
    verify(preferencesService, times(2)).storeInBackground();
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
    preferences.getLastGame().setLastGameTitle("testGame");

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is("testGame"));
  }


  @Test
  public void testButtonBindingIfFeaturedModNotSet() {
    preferences.getLastGame().setLastGameTitle("123");
    when(i18n.get("game.create.featuredModMissing")).thenReturn("Mod missing");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is("123"));
    assertThat(instance.createGameButton.getText(), is("Mod missing"));
  }

  @Test
  public void testButtonBindingIfTitleNotSet() {

    when(i18n.get("game.create.titleMissing")).thenReturn("title missing");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("title missing"));
  }

  @Test
  public void testButtonBindingIfNotConnected() {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.DISCONNECTED));
    when(i18n.get("game.create.disconnected")).thenReturn("disconnected");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("disconnected"));
  }

  @Test
  public void testButtonBindingIfNotConnecting() {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTING));
    when(i18n.get("game.create.connecting")).thenReturn("connecting");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("connecting"));
  }

  @Test
  public void testSelectLastMap() {
    MapBean lastMapBean = MapBuilder.create().defaultValues().folderName("foo").get();
    preferences.getLastGame().setLastMap("foo");

    mapList.add(MapBuilder.create().defaultValues().folderName("Test1").get());
    mapList.add(lastMapBean);

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMapBean));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = new CreateGameController(mapService, modService, gameService, preferencesService, i18n, notificationService, fafService, mapGeneratorService, uiService);

    loadFxml("theme/play/create_game.fxml", clazz -> {
      if (clazz.equals(ModManagerController.class)) {
        return modManagerController;
      }
      return instance;
    });

    assertThat(instance.featuredModListView.getItems(), empty());
  }

  @Test
  public void testCloseButtonTriggeredAfterCreatingGame() {
    Runnable closeAction = mock(Runnable.class);
    instance.setOnCloseButtonClickedListener(closeAction);

    MapBean map = MapBuilder.create().defaultValues().get();
    when(mapService.updateLatestVersionIfNecessary(map)).thenReturn(completedFuture(map));
    when(gameService.hostGame(any())).thenReturn(completedFuture(null));

    mapList.add(map);
    instance.mapListView.getSelectionModel().select(0);
    instance.onCreateButtonClicked();

    verify(closeAction).run();
  }

  @Test
  public void testCreateGameWithSelectedModAndMap() {
    ArgumentCaptor<NewGameInfo> newGameInfoArgumentCaptor = ArgumentCaptor.forClass(NewGameInfo.class);
    ModVersion modVersion = new ModVersion();
    String uidMod = "junit-mod";
    modVersion.setUid(uidMod);

    when(modManagerController.apply()).thenReturn(Collections.singletonList(modVersion));

    MapBean map = MapBuilder.create().defaultValues().get();
    when(mapService.isOfficialMap(map)).thenReturn(false);
    when(mapService.updateLatestVersionIfNecessary(map)).thenReturn(completedFuture(map));
    when(gameService.hostGame(newGameInfoArgumentCaptor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    instance.mapListView.getSelectionModel().select(0);
    instance.setOnCloseButtonClickedListener(mock(Runnable.class));
    instance.onCreateButtonClicked();

    verify(modManagerController).apply();
    assertThat(newGameInfoArgumentCaptor.getValue().getSimMods(), contains(uidMod));
    assertThat(newGameInfoArgumentCaptor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testCreateGameOnSelectedMapIfNoNewVersionMap() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);
    MapBean map = MapBuilder.create().defaultValues().get();

    when(mapService.updateLatestVersionIfNecessary(map)).thenReturn(completedFuture(map));
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    instance.mapListView.getSelectionModel().select(0);
    instance.setOnCloseButtonClickedListener(mock(Runnable.class));
    instance.onCreateButtonClicked();

    assertThat(captor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testCreateGameOnUpdatedMapIfNewVersionMapExist() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);

    MapBean outdatedMap = MapBuilder.create().defaultValues().folderName("test.v0001").get();
    MapBean updatedMap = MapBuilder.create().defaultValues().folderName("test.v0002").get();
    when(mapService.updateLatestVersionIfNecessary(outdatedMap)).thenReturn(completedFuture(updatedMap));
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(outdatedMap);
    instance.mapListView.getSelectionModel().select(0);
    instance.setOnCloseButtonClickedListener(mock(Runnable.class));
    instance.onCreateButtonClicked();

    assertThat(captor.getValue().getMap(), is(updatedMap.getFolderName()));
  }

  @Test
  public void testCreateGameOnOfficialMap() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);

    MapBean map = MapBuilder.create().defaultValues().get();
    when(mapService.isOfficialMap(map)).thenReturn(true);
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    instance.mapListView.getSelectionModel().select(0);
    instance.setOnCloseButtonClickedListener(mock(Runnable.class));
    instance.onCreateButtonClicked();

    assertThat(captor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testCreateGameOnSelectedMapImmediatelyIfThrowExceptionWhenUpdatingMap() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);

    MapBean map = MapBuilder.create().defaultValues().get();
    when(mapService.updateLatestVersionIfNecessary(map))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("error when checking for update or updating map")));
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    instance.mapListView.getSelectionModel().select(0);
    instance.setOnCloseButtonClickedListener(mock(Runnable.class));
    instance.onCreateButtonClicked();

    assertThat(captor.getValue().getMap(), is(map.getFolderName()));
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

    preferences.getLastGame().setLastGameType(null);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredMod2));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("last").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.getLastGame().setLastGameType("last");
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredMod));
  }

  @Test
  public void testOnlyFriendsBinding() {
    instance.onlyForFriendsCheckBox.setSelected(true);
    assertThat(preferences.getLastGame().isLastGameOnlyFriends(), is(true));
    instance.onlyForFriendsCheckBox.setSelected(false);
    assertThat(preferences.getLastGame().isLastGameOnlyFriends(), is(false));
  }

  @Test
  public void testPasswordIsSaved() {
    instance.passwordTextField.setText("1234");
    assertEquals(preferences.getLastGame().getLastGamePassword(), "1234");
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

  @Test
  public void testOnGenerateMapClicked() {
    instance.onGenerateMapButtonClicked();

    verify(mapGeneratorService).queryMaxSupportedVersion();
    verify(mapGeneratorService).setGeneratorVersion(any());
    verify(mapGeneratorService).downloadGeneratorIfNecessary(any());
    verify(mapGeneratorService).getGeneratorStyles();
    verify(generateMapController).setStyles(any());
    verify(generateMapController).setOnCloseButtonClickedListener(any());
  }
}
