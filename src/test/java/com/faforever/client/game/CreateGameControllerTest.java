package com.faforever.client.game;

import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.util.Callback;
import com.sun.org.apache.xpath.internal.operations.Mod;
import javafx.collections.MapChangeListener.Change;
import com.faforever.client.map.MapService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractSpringJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateGameControllerTest extends AbstractSpringJavaFxTest {


  private CreateGameController instance;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  MapService mapService;

  @Autowired
  GameService gameService;

  @Autowired
  ModService modService;

  private ObservableList<MapInfoBean> mapList;
  private static KeyEvent keyUpPressed = new KeyEvent(KeyEvent.KEY_PRESSED, "0", "", KeyCode.UP, false, false, false, false);
  private static KeyEvent keyUpReleased = new KeyEvent(KeyEvent.KEY_RELEASED, "0", "", KeyCode.UP, false, false, false, false);
  private static KeyEvent keyDownPressed = new KeyEvent(KeyEvent.KEY_PRESSED, "0", "", KeyCode.DOWN, false, false, false, false);
  private static KeyEvent keyDownReleased = new KeyEvent(KeyEvent.KEY_RELEASED, "0", "", KeyCode.DOWN, false, false, false, false);

  @Before
  public void setUp() throws Exception {
    mapList = FXCollections.observableArrayList();

    when(preferencesService.getPreferences().getForgedAlliance().getPath()).thenReturn(Paths.get(""));
    when(mapService.getLocalMaps()).thenReturn(mapList);

    instance = loadController("create_game.fxml");
  }

  @Test
  public void testMapSearchTextFieldFilteringEmpty() throws Exception {
    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMaps.getSource(), empty());
  }

  @Test
  public void testMapSearchTextFieldFilteringPopulated() throws Exception {
    mapList.add(new MapInfoBean("Test1"));
    mapList.add(new MapInfoBean("test2"));
    mapList.add(new MapInfoBean("foo"));

    instance.mapSearchTextField.setText("Test");

    assertThat(instance.filteredMaps.get(0).getDisplayName(), is("Test1"));
    assertThat(instance.filteredMaps.get(1).getDisplayName(), is("test2"));
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
    mapList.add(new MapInfoBean("Test1"));
    mapList.add(new MapInfoBean("Test2"));
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyUpReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(0));
  }

  @Test
  public void testMapSearchTextFieldKeyPressedDownForPopulated() throws Exception {
    mapList.add(new MapInfoBean("Test1"));
    mapList.add(new MapInfoBean("Test2"));
    instance.mapSearchTextField.setText("Test");

    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownPressed);
    instance.mapSearchTextField.getOnKeyPressed().handle(keyDownReleased);

    assertThat(instance.mapListView.getSelectionModel().getSelectedIndex(), is(1));
  }

  @Test
  public void testSetLastGameTitle() throws Exception {
    when(preferencesService.getPreferences().getLastGameTitle()).thenReturn("testGame");
    when(preferencesService.getPreferences().getForgedAlliance().getPath()).thenReturn(Paths.get(""));
    instance = loadController("create_game.fxml");

    assertThat(instance.titleTextField.getText(), is("testGame"));
  }

  @Test
  public void testSelectLastMap() throws Exception {
    MapInfoBean lastMap = new MapInfoBean("foo");
    when(preferencesService.getPreferences().getLastMap()).thenReturn("foo");

    mapList.add(new MapInfoBean("Test1"));
    mapList.add(lastMap);
    instance = loadController("create_game.fxml");

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMap));
  }

  @Test
  public void testInitGameTypeComboBoxEmpty() throws Exception {
    instance = loadController("create_game.fxml");

    assertThat(instance.gameTypeComboBox.getItems(), empty());
  }

  @Test
  public void testInitGameTypeComboBoxPostPopulated() throws Exception {
    instance = loadController("create_game.fxml");

    GameTypeBean gameTypeBean = mock(GameTypeBean.class);
    onGameTypeAdded(gameTypeBean);

    assertThat(instance.gameTypeComboBox.getItems(), hasSize(1));
    assertThat(instance.gameTypeComboBox.getItems().get(0), is(gameTypeBean));
  }

  //TODO implement
  @Test
  public void testInitGameTypeComboBoxPrePopulated() throws Exception {
  }

  //FIXME fix this
  @Test
  public void testSelectLastOrDefaultSelectDefault() throws Exception {
    GameTypeBean gameTypeBean = mock(GameTypeBean.class);
    GameTypeBean gameTypeBean2 = mock(GameTypeBean.class);
    when(preferencesService.getPreferences().getLastGameType()).thenReturn(null);
    when(gameTypeBean.getName()).thenReturn(FeaturedMod.DEFAULT_MOD.getString());
    when(gameTypeBean2.getName()).thenReturn(null);

    instance = loadController("create_game.fxml");

    onGameTypeAdded(gameTypeBean2);
    onGameTypeAdded(gameTypeBean);

    assertThat(instance.gameTypeComboBox.getSelectionModel().getSelectedItem(), is(gameTypeBean));
  }

  @Test
  public void testSelectLastOrDefaultSelectLast() throws Exception {
    GameTypeBean gameTypeBean = mock(GameTypeBean.class);
    GameTypeBean gameTypeBean2 = mock(GameTypeBean.class);
    when(preferencesService.getPreferences().getLastGameType()).thenReturn("last");
    when(gameTypeBean.getName()).thenReturn(null);
    when(gameTypeBean2.getName()).thenReturn("last");

    instance = loadController("create_game.fxml");

    onGameTypeAdded(gameTypeBean);
    onGameTypeAdded(gameTypeBean2);

    assertThat(instance.gameTypeComboBox.getSelectionModel().getSelectedItem(), is(gameTypeBean2));
  }

  private void onGameTypeAdded(GameTypeBean gameTypeBean) {
    ArgumentCaptor<MapChangeListener<String, GameTypeBean>> argument = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(instance.gameService, atLeastOnce()).addOnGameTypeInfoListener(argument.capture());

    MapChangeListener<String, GameTypeBean> listener = argument.getValue();

    Change change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(gameTypeBean);

    listener.onChanged(change);
  }

  @Test
  public void testInitModListEmpty() {
    ArgumentCaptor<Callback<List<ModInfoBean>>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(modService).getInstalledModsInBackground(callbackCaptor.capture());

    instance = loadController("create_game.fxml");
    assertThat(instance.modListView.getItems(), empty());

    callbackCaptor.getValue().success(new ArrayList<>());
    assertThat(instance.modListView.getItems(), empty());
  }

  @Test
  public void testInitModListPopulated() {
    ArgumentCaptor<Callback<List<ModInfoBean>>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(modService).getInstalledModsInBackground(callbackCaptor.capture());

    instance = loadController("create_game.fxml");
    assertThat(instance.modListView.getItems(), empty());

    ModInfoBean modInfoBean = mock(ModInfoBean.class);
    List<ModInfoBean> result = new ArrayList<ModInfoBean>();
    result.add(mock(ModInfoBean.class));
    callbackCaptor.getValue().success(result);

    assertThat(instance.modListView.getItems(), hasItem(modInfoBean));
  }


  @Test
  public void testPostConstruct() throws Exception {

  }

  @Test
  public void testOnRandomMapButtonClicked() throws Exception {

  }

  @Test
  public void testOnCreateButtonClicked() throws Exception {

  }

  @Test
  public void testGetRoot() throws Exception {

  }
}