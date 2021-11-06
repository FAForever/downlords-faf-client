package com.faforever.client.game;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.ModBeanBuilder;
import com.faforever.client.builders.ModVersionBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModManagerController;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.user.UserService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateGameControllerTest extends UITest {


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
  private UserService userService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private ModManagerController modManagerController;
  @Mock
  private GenerateMapController generateMapController;
  @Mock
  private MapFilterController mapFilterController;

  private Preferences preferences;
  private CreateGameController instance;
  private ObservableList<MapVersionBean> mapList;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new CreateGameController(mapService, modService, gameService, preferencesService, i18n, notificationService, userService, mapGeneratorService, uiService);

    mapList = FXCollections.observableArrayList();

    preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .installationPath(Path.of(""))
        .then()
        .get();
    when(mapGeneratorService.downloadGeneratorIfNecessary(any())).thenReturn(completedFuture(null));
    when(mapGeneratorService.getGeneratorStyles()).thenReturn(completedFuture(List.of()));
    when(uiService.showInDialog(any(), any(), anyString())).thenReturn(new Dialog());
    when(uiService.loadFxml("theme/play/generate_map.fxml")).thenReturn(generateMapController);
    when(uiService.loadFxml("theme/play/map_filter.fxml")).thenReturn(mapFilterController);
    when(mapFilterController.getFilterAppliedProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(mapFilterController.getRoot()).thenReturn(new Pane());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(mapService.getInstalledMaps()).thenReturn(mapList);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(emptyList()));
    when(mapService.loadPreview(anyString(), any())).thenReturn(new Image("/theme/images/default_achievement.png"));
    when(i18n.get(any(), any())).then(invocation -> invocation.getArgument(0));
    when(i18n.number(anyInt())).then(invocation -> invocation.getArgument(0).toString());
    when(userService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));
    when(userService.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
    when(modService.updateAndActivateModVersions(any()))
        .thenAnswer(invocation -> completedFuture(invocation.getArgument(0)));

    loadFxml("theme/play/create_game.fxml", clazz -> {
      if (clazz.equals(ModManagerController.class)) {
        return modManagerController;
      }
      return instance;
    });
  }

  @Test
  public void showOnlyToFriendsRemembered() {
    instance.onlyForFriendsCheckBox.setSelected(true);
    assertThat(preferences.getLastGame().isLastGameOnlyFriends(), is(true));
    instance.onlyForFriendsCheckBox.setSelected(false);
    assertThat(preferences.getLastGame().isLastGameOnlyFriends(), is(false));
    verify(preferencesService, times(3)).storeInBackground();
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
    mapList.add(MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().displayName("Test1").get()).get());
    mapList.add(MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().displayName("Test1").get()).get());
    instance.mapSearchTextField.setText("Test");

    runOnFxThreadAndWait(() -> {
      instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
      instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);
      instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
      instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);
    });

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(0));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForPopulated() {
    mapList.add(MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().displayName("Test1").get()).get());
    mapList.add(MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().displayName("Test1").get()).get());
    instance.mapSearchTextField.setText("Test");

    runOnFxThreadAndWait(() -> {
      instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
      instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);
    });

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
  public void testButtonBindingIfTitleNotAscii() {

    when(i18n.get("game.create.titleNotAscii")).thenReturn("title not ascii");
    instance.titleTextField.setText("ты");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is("ты"));
    assertThat(instance.createGameButton.getText(), is("title not ascii"));
  }

  @Test
  public void testButtonBindingIfNotConnected() {
    when(userService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.DISCONNECTED));
    when(userService.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);
    when(i18n.get("game.create.disconnected")).thenReturn("disconnected");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("disconnected"));
  }

  @Test
  public void testButtonBindingIfNotConnecting() {
    when(userService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTING));
    when(userService.getConnectionState()).thenReturn(ConnectionState.CONNECTING);
    when(i18n.get("game.create.connecting")).thenReturn("connecting");
    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("connecting"));
  }

  @Test
  public void testSelectLastMap() {
    MapVersionBean lastMapBean = MapVersionBeanBuilder.create().defaultValues().folderName("foo").map(MapBeanBuilder.create().defaultValues().get()).get();
    preferences.getLastGame().setLastMap("foo");

    mapList.add(MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get());
    mapList.add(lastMapBean);

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMapBean));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = new CreateGameController(mapService, modService, gameService, preferencesService, i18n, notificationService, userService, mapGeneratorService, uiService);

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

    MapBean map = MapBeanBuilder.create().defaultValues().get();
    when(mapService.updateLatestVersionIfNecessary(map.getLatestVersion())).thenReturn(completedFuture(map.getLatestVersion()));
    when(gameService.hostGame(any())).thenReturn(completedFuture(null));

    mapList.add(map.getLatestVersion());
    instance.mapListView.getSelectionModel().select(0);
    instance.onCreateButtonClicked();

    verify(closeAction).run();
  }

  @Test
  public void testCreateGameWithSelectedModAndMap() {
    ArgumentCaptor<NewGameInfo> newGameInfoArgumentCaptor = ArgumentCaptor.forClass(NewGameInfo.class);
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().uid("junit-mod").mod(ModBeanBuilder.create().defaultValues().get()).get();

    when(modManagerController.getSelectedModVersions()).thenReturn(List.of(modVersion));

    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    when(mapService.updateLatestVersionIfNecessary(map)).thenReturn(completedFuture(map));
    when(gameService.hostGame(newGameInfoArgumentCaptor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    runOnFxThreadAndWait(() -> {
      instance.mapListView.getSelectionModel().select(0);
      instance.setOnCloseButtonClickedListener(mock(Runnable.class));
      instance.onCreateButtonClicked();
    });

    verify(modManagerController).getSelectedModVersions();
    assertThat(newGameInfoArgumentCaptor.getValue().getSimMods(), contains("junit-mod"));
    assertThat(newGameInfoArgumentCaptor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testCreateGameWithOutdatedMod() {
    ArgumentCaptor<NewGameInfo> newGameInfoArgumentCaptor = ArgumentCaptor.forClass(NewGameInfo.class);
    ModVersionBean modVersion = new ModVersionBean();
    String uidMod = "outdated-mod";
    modVersion.setUid(uidMod);

    ModVersionBean newModVersion = new ModVersionBean();
    String newModUid = "new-mod";
    newModVersion.setUid(newModUid);

    List<ModVersionBean> selectedMods = singletonList(modVersion);
    when(modManagerController.getSelectedModVersions()).thenReturn(selectedMods);

    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    when(mapService.updateLatestVersionIfNecessary(map)).thenReturn(CompletableFuture.completedFuture(map));

    when(modService.updateAndActivateModVersions(eq(selectedMods)))
        .thenAnswer(invocation -> completedFuture(List.of(newModVersion)));

    when(gameService.hostGame(any())).thenReturn(completedFuture(null));

    runOnFxThreadAndWait(() -> {
      mapList.add(map);
      instance.mapListView.getSelectionModel().select(0);
      instance.setOnCloseButtonClickedListener(mock(Runnable.class));
    });

    instance.onCreateButtonClicked();

    verify(modManagerController).getSelectedModVersions();
    verify(gameService).hostGame(newGameInfoArgumentCaptor.capture());
    assertThat(newGameInfoArgumentCaptor.getValue().getSimMods(), contains(newModUid));
    assertThat(newGameInfoArgumentCaptor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testCreateGameOnSelectedMapIfNoNewVersionMap() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);
    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();

    when(mapService.updateLatestVersionIfNecessary(map)).thenReturn(completedFuture(map));
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    runOnFxThreadAndWait(() -> {
      instance.mapListView.getSelectionModel().select(0);
      instance.setOnCloseButtonClickedListener(mock(Runnable.class));
      instance.onCreateButtonClicked();
    });

    assertThat(captor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testCreateGameOnUpdatedMapIfNewVersionMapExist() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);

    MapVersionBean outdatedMap = MapVersionBeanBuilder.create().defaultValues().folderName("test.v0001").map(MapBeanBuilder.create().defaultValues().get()).get();
    MapVersionBean updatedMap = MapVersionBeanBuilder.create().defaultValues().folderName("test.v0002").map(MapBeanBuilder.create().defaultValues().get()).get();
    when(mapService.updateLatestVersionIfNecessary(outdatedMap)).thenReturn(completedFuture(updatedMap));
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(outdatedMap);
    runOnFxThreadAndWait(() -> {
      instance.mapListView.getSelectionModel().select(0);
      instance.setOnCloseButtonClickedListener(mock(Runnable.class));
      instance.onCreateButtonClicked();
    });

    assertThat(captor.getValue().getMap(), is(updatedMap.getFolderName()));
  }

  @Test
  public void testCreateGameOnSelectedMapImmediatelyIfThrowExceptionWhenUpdatingMap() {
    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);

    MapVersionBean map = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    when(mapService.updateLatestVersionIfNecessary(map))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("error when checking for update or updating map")));
    when(gameService.hostGame(captor.capture())).thenReturn(completedFuture(null));

    mapList.add(map);
    runOnFxThreadAndWait(() -> {
      instance.mapListView.getSelectionModel().select(0);
      instance.setOnCloseButtonClickedListener(mock(Runnable.class));
      instance.onCreateButtonClicked();
    });

    assertThat(captor.getValue().getMap(), is(map.getFolderName()));
  }

  @Test
  public void testInitGameTypeComboBoxPostPopulated() {
    FeaturedModBean featuredModBean = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMods()).thenReturn(completedFuture(singletonList(featuredModBean)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getItems(), contains(featuredModBean));
  }

  @Test
  public void testSelectLastOrDefaultSelectDefault() {
    FeaturedModBean featuredModBean = FeaturedModBeanBuilder.create().defaultValues().technicalName("something").get();
    FeaturedModBean featuredModBean2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.getLastGame().setLastGameType(null);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredModBean, featuredModBean2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredModBean2));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() {
    FeaturedModBean featuredModBean = FeaturedModBeanBuilder.create().defaultValues().technicalName("last").get();
    FeaturedModBean featuredModBean2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.getLastGame().setLastGameType("last");
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredModBean, featuredModBean2)));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredModBean));
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
    verify(preferencesService, times(2)).storeInBackground();
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

  @Test
  public void testOnMapFilterButtonClicked() {
    settingMapFilterPopupForTestEnvironment();
    runOnFxThreadAndWait(() -> instance.mapFilterButton.fire());
    assertTrue(instance.mapFilterPopup.isShowing());

    runOnFxThreadAndWait(() -> instance.mapFilterButton.fire());
    assertFalse(instance.mapFilterPopup.isShowing());
  }

  private void settingMapFilterPopupForTestEnvironment() {
    runOnFxThreadAndWait(() -> {
      getRoot().getChildren().add(instance.mapSearchTextField);
      instance.mapFilterPopup.setOpacity(0.0);
    });
  }
}
