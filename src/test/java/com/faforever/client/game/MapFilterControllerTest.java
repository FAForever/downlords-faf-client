package com.faforever.client.game;

import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapSize;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.faforever.client.map.MapBeanBuilder.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapFilterControllerTest extends AbstractPlainJavaFxTest {

  private MapFilterController instance;
  private TextField mapNameTextField;
  private FilteredList<MapBean> filteredMapList;

  private final List<MapBean> maps = List.of(
      create().folderName("map1.v001").displayName("map1").players(8).size(MapSize.valueOf(512, 512)).get(),
      create().folderName("map2.v001").displayName("map2").players(16).size(MapSize.valueOf(1024, 1024)).get(),
      create().folderName("map3.v001").displayName("map3").players(4).size(MapSize.valueOf(256, 256)).get(),
      create().folderName("map4.v001").displayName("map4").players(4).size(MapSize.valueOf(512, 512)).get()
  );

  @Before
  public void setUp() throws IOException {
    instance = new MapFilterController();
    loadFxml("theme/play/map_filter.fxml", clazz -> instance);

    filteredMapList = new FilteredList<>(FXCollections.observableArrayList(maps));
    mapNameTextField = new TextField();
    instance.setMapNameTextField(mapNameTextField);
    instance.setFilteredMapList(filteredMapList);
  }

  @Test
  public void testFilterByMapName() {
    runOnFxThreadAndWait(() -> mapNameTextField.setText("p2"));

    assertEquals(1, filteredMapList.size());
    assertEquals("map2", filteredMapList.get(0).getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNumberOfPlayers() {
    runOnFxThreadAndWait(() -> instance.numberOfPlayersTextField.setText("8"));

    assertEquals(1, filteredMapList.size());
    assertEquals("map1", filteredMapList.get(0).getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByMapWidthSize() {
    runOnFxThreadAndWait(() -> instance.mapWidthInKmTextField.setText("5"));

    assertEquals(1, filteredMapList.size());
    assertEquals("map3", filteredMapList.get(0).getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNameAndNumberOfPLayers() {
    runOnFxThreadAndWait(() -> {
      instance.mapWidthInKmTextField.setText("10");
      instance.numberOfPlayersTextField.setText("4");
    });

    assertEquals(1, filteredMapList.size());
    assertEquals("map4", filteredMapList.get(0).getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNameAndMapHeightSize() {
    runOnFxThreadAndWait(() -> {
      mapNameTextField.setText("map");
      instance.mapHeightInKmTextField.setText("20");
    });

    assertEquals(1, filteredMapList.size());
    assertEquals("map2", filteredMapList.get(0).getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNameIfNoMatch() {
    runOnFxThreadAndWait(() -> mapNameTextField.setText("map6"));
    assertTrue(filteredMapList.isEmpty());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterNotAppliedWhenValuesAreEmpty() {
    runOnFxThreadAndWait(() -> {
      mapNameTextField.setText("");
      instance.numberOfPlayersTextField.setText("");
      instance.mapWidthInKmTextField.setText("");
      instance.mapHeightInKmTextField.setText("");
    });
    verifyFilterAppliedProperty(false);
  }

  private void verifyFilterAppliedProperty(boolean value) {
    assertEquals(instance.getFilterAppliedProperty().getValue(), value);
  }
}
