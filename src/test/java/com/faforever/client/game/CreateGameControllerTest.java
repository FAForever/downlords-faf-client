package com.faforever.client.game;

import com.faforever.client.map.MapService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractSpringJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class CreateGameControllerTest extends AbstractSpringJavaFxTest {


  private CreateGameController instance;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  MapService mapService;

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

    instance = loadController("create_game.fxml");

    assertThat(instance.mapListView.getSelectionModel().getSelectedItem(), is(lastMap));
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