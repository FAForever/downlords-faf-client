package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.ThemeService;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
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
  private GameService gameService;
  @Mock
  private ModService modService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private Environment environment;
  @Mock
  private I18n i18n;
  @Mock
  private ThemeService themeService;

  private CreateGameController instance;
  private ObservableList<MapBean> mapList;

  @Before
  public void setUp() throws Exception {
    instance = loadController("create_game.fxml");
    instance.preferencesService = preferencesService;
    instance.mapService = mapService;
    instance.gameService = gameService;
    instance.modService = modService;
    instance.environment = environment;
    instance.i18n = i18n;
    instance.themeService = themeService;

    mapList = FXCollections.observableArrayList();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(Paths.get(""));
    when(mapService.getInstalledMaps()).thenReturn(mapList);

    doAnswer(invocation -> getThemeFile(invocation.getArgumentAt(0, String.class)))
        .when(themeService).getThemeFile(any());

    doAnswer(invocation -> getThemeFile(invocation.getArgumentAt(0, String.class)))
        .when(themeService).getThemeFile(ThemeService.UNKNOWN_MAP_IMAGE);

    instance.postConstruct();
  }

  @Test
  public void testMapSearchTextFieldFilteringEmpty() throws Exception {
    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMapBeans.getSource(), empty());
  }

  @Test
  public void testMapSearchTextFieldFilteringPopulated() throws Exception {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().technicalName("test2").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("foo").get());

    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMapBeans.get(0).getDisplayName(), is("Test1"));
    assertThat(instance.filteredMapBeans.get(1).getFolderName(), is("test2"));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForEmpty() throws Exception {
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForEmpty() throws Exception {
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(-1));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedUpForPopulated() throws Exception {
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
  public void testMapSearchTextFieldKeyPressedDownForPopulated() throws Exception {
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    mapList.add(MapBuilder.create().defaultValues().displayName("Test1").get());
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(1));
  }

  @Test
  public void testSetLastGameTitle() throws Exception {
    when(preferences.getLastGameTitle()).thenReturn("testGame");
    when(preferences.getForgedAlliance().getPath()).thenReturn(Paths.get(""));
    instance.postConstruct();

    assertThat(instance.titleTextField.getText(), is("testGame"));
  }

  @Test
  public void testSelectLastMap() throws Exception {
    MapBean lastMapBean = MapBuilder.create().defaultValues().technicalName("foo").get();
    when(preferences.getLastMap()).thenReturn("foo");

    mapList.add(MapBuilder.create().defaultValues().technicalName("Test1").get());
    mapList.add(lastMapBean);
    instance.postConstruct();

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMapBean));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = loadController("create_game.fxml");

    assertThat(instance.gameTypeListView.getItems(), empty());
  }

  @Test
  public void testInitGameTypeComboBoxPostPopulated() throws Exception {
    instance.postConstruct();

    GameTypeBean gameTypeBean = mock(GameTypeBean.class);
    onGameTypeAdded(gameTypeBean);

    assertThat(instance.gameTypeListView.getItems(), hasSize(1));
    assertThat(instance.gameTypeListView.getItems().get(0), is(gameTypeBean));
  }

  private void onGameTypeAdded(GameTypeBean gameTypeBean) {
    ArgumentCaptor<MapChangeListener<String, GameTypeBean>> argument = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(instance.gameService, atLeastOnce()).addOnGameTypesChangeListener(argument.capture());

    MapChangeListener<String, GameTypeBean> listener = argument.getValue();

    @SuppressWarnings("unchecked")
    Change<String, GameTypeBean> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(gameTypeBean);

    listener.onChanged(change);
  }

  // FIXME fix this
  @Test
  public void testSelectLastOrDefaultSelectDefault() throws Exception {
    GameTypeBean gameTypeBean = mock(GameTypeBean.class);
    GameTypeBean gameTypeBean2 = mock(GameTypeBean.class);
    when(preferences.getLastGameType()).thenReturn(null);
    when(gameTypeBean.getName()).thenReturn(GameType.DEFAULT.getString());
    when(gameTypeBean2.getName()).thenReturn(null);

    instance.postConstruct();

    onGameTypeAdded(gameTypeBean2);
    onGameTypeAdded(gameTypeBean);

    assertThat(instance.gameTypeListView.getSelectionModel().getSelectedItem(), is(gameTypeBean));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() throws Exception {
    GameTypeBean gameTypeBean = mock(GameTypeBean.class);
    GameTypeBean gameTypeBean2 = mock(GameTypeBean.class);
    when(preferences.getLastGameType()).thenReturn("last");
    when(gameTypeBean.getName()).thenReturn(null);
    when(gameTypeBean2.getName()).thenReturn("last");

    instance.postConstruct();

    onGameTypeAdded(gameTypeBean);
    onGameTypeAdded(gameTypeBean2);

    assertThat(instance.gameTypeListView.getSelectionModel().getSelectedItem(), is(gameTypeBean2));
  }

  @Test
  public void testInitModListEmpty() throws Exception {
    assertThat(instance.modListView.getItems(), nullValue());
    instance.postConstruct();
    assertThat(instance.modListView.getItems(), nullValue());
  }

  @Test
  public void testInitModListPopulated() throws Exception {
    assertThat(instance.modListView.getItems(), nullValue());

    ModInfoBean modInfoBean1 = mock(ModInfoBean.class);
    ModInfoBean modInfoBean2 = mock(ModInfoBean.class);

    when(modService.getInstalledMods()).thenReturn(FXCollections.observableArrayList(
        modInfoBean1, modInfoBean2
    ));

    instance.postConstruct();

    assertThat(instance.modListView.getItems(), hasSize(2));
    assertThat(instance.modListView.getItems(), contains(modInfoBean1, modInfoBean2));
  }
}
