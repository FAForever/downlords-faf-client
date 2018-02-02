package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.Mod;
import com.faforever.client.mod.ModService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
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

  private Preferences preferences;
  private CreateGameController instance;
  private ObservableList<MapBean> mapList;

  @Before
  public void setUp() throws Exception {
    instance = new CreateGameController(fafService, mapService, modService, gameService, preferencesService, i18n, notificationService, reportingService);

    mapList = FXCollections.observableArrayList();

    preferences = new Preferences();
    preferences.getForgedAlliance().setPath(Paths.get("."));
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(mapService.getInstalledMaps()).thenReturn(mapList);
    when(modService.getFeaturedMods()).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(modService.getInstalledMods()).thenReturn(FXCollections.observableList(emptyList()));
    when(mapService.loadPreview(anyString(), any())).thenReturn(new Image("/theme/images/close.png"));
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));

    loadFxml("theme/play/create_game.fxml", clazz -> instance);
  }

  private void initInstance() {
    Platform.runLater(() -> instance.onDisplay());
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testMapSearchTextFieldFilteringEmpty() throws Exception {
    instance.mapSearchTextField.setText("Test");

    initInstance();

    assertThat(instance.filteredMapBeans.getSource(), empty());
  }

  @Test
  public void testMapSearchTextFieldFilteringPopulated() throws Exception {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().folderName("test2").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("foo").get());

    initInstance();

    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMapBeans.get(0).getFolderName(), is("test2"));
    assertThat(instance.filteredMapBeans.get(1).getDisplayName(), is("Test1"));
  }

  @Test
  public void testSelectMap() throws Exception {
    MapBean testMap = MapBuilder.create().defaultValues().folderName("test2").get();
    mapList.add(testMap);
    instance.mapListView.setItems(mapList);

    assertTrue(instance.selectMap("test2"));
    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), equalTo(testMap));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForEmpty() throws Exception {
    initInstance();

    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForPopulated() throws Exception {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());

    initInstance();

    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(0));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForPopulated() throws Exception {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());

    initInstance();

    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(1));
  }

  @Test
  public void testSetLastGameTitle() throws Exception {
    preferences.setLastGameTitle("testGame");
    preferences.getForgedAlliance().setPath(Paths.get(""));

    initInstance();

    assertThat(instance.titleTextField.getText(), is("testGame"));
  }


  @Test
  public void testButtonBindingIfFeaturedModNotSet() throws Exception {
    preferences.setLastGameTitle("123");
    when(i18n.get("game.create.featuredModMissing")).thenReturn("Mod missing");
    preferences.getForgedAlliance().setPath(Paths.get(""));

    initInstance();

    assertThat(instance.titleTextField.getText(), is("123"));
    assertThat(instance.createGameButton.getText(), is("Mod missing"));
  }

  @Test
  public void testButtonBindingIfTitleNotSet() throws Exception {

    when(i18n.get("game.create.titleMissing")).thenReturn("title missing");
    preferences.getForgedAlliance().setPath(Paths.get(""));

    initInstance();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("title missing"));
  }

  @Test
  public void testButtonBindingIfNotConnected() throws Exception {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.DISCONNECTED));
    when(i18n.get("game.create.disconnected")).thenReturn("disconnected");
    preferences.getForgedAlliance().setPath(Paths.get(""));

    initInstance();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("disconnected"));
  }

  @Test
  public void testButtonBindingIfNotConnecting() throws Exception {
    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTING));
    when(i18n.get("game.create.connecting")).thenReturn("connecting");
    preferences.getForgedAlliance().setPath(Paths.get(""));

    initInstance();

    assertThat(instance.titleTextField.getText(), is(""));
    assertThat(instance.createGameButton.getText(), is("connecting"));
  }

  @Test
  public void testSelectLastMap() throws Exception {
    MapBean lastMapBean = MapBuilder.create().defaultValues().folderName("foo").get();
    preferences.setLastMap("foo");

    mapList.add(MapBuilder.create().defaultValues().folderName("Test1").get());
    mapList.add(lastMapBean);

    initInstance();

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMapBean));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = new CreateGameController(fafService, mapService, modService, gameService, preferencesService, i18n, notificationService, reportingService);
    loadFxml("theme/play/create_game.fxml", clazz -> instance);

    assertThat(instance.featuredModListView.getItems(), empty());
  }

  @Test
  public void testInitGameTypeComboBoxPostPopulated() throws Exception {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().get();
    when(modService.getFeaturedMods()).thenReturn(completedFuture(singletonList(featuredMod)));

    initInstance();

    assertThat(instance.featuredModListView.getItems(), hasSize(1));
    assertThat(instance.featuredModListView.getItems().get(0), is(featuredMod));
  }

  @Test
  public void testSelectLastOrDefaultSelectDefault() throws Exception {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("something").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.setLastGameType(null);
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    initInstance();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredMod2));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() throws Exception {
    FeaturedMod featuredMod = FeaturedModBeanBuilder.create().defaultValues().technicalName("last").get();
    FeaturedMod featuredMod2 = FeaturedModBeanBuilder.create().defaultValues().technicalName(KnownFeaturedMod.DEFAULT.getTechnicalName()).get();

    preferences.setLastGameType("last");
    when(modService.getFeaturedMods()).thenReturn(completedFuture(asList(featuredMod, featuredMod2)));

    initInstance();

    assertThat(instance.featuredModListView.getSelectionModel().getSelectedItem(), is(featuredMod));
  }

  @Test
  public void testInitModListPopulated() throws Exception {
    assertThat(instance.modListView.getItems(), empty());

    Mod mod1 = mock(Mod.class);
    Mod mod2 = mock(Mod.class);

    when(modService.getInstalledMods()).thenReturn(FXCollections.observableArrayList(
        mod1, mod2
    ));
    initInstance();

    assertThat(instance.modListView.getItems(), hasSize(2));
    assertThat(instance.modListView.getItems(), contains(mod1, mod2));
  }
}
