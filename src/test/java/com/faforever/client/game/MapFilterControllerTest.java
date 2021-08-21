package com.faforever.client.game;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.map.MapSize;
import com.faforever.client.test.UITest;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapFilterControllerTest extends UITest {

  private MapFilterController instance;
  private TextField mapNameTextField;
  private FilteredList<MapVersionBean> filteredMapList;

  private final List<MapVersionBean> maps = List.of(
      MapVersionBeanBuilder.create().defaultValues().id(0).map(MapBeanBuilder.create().displayName("map1").get()).folderName("map1.v001").maxPlayers(8).size(MapSize.valueOf(512, 512)).get(),
      MapVersionBeanBuilder.create().defaultValues().id(1).map(MapBeanBuilder.create().displayName("map2").get()).folderName("map2.v001").maxPlayers(16).size(MapSize.valueOf(1024, 1024)).get(),
      MapVersionBeanBuilder.create().defaultValues().id(2).map(MapBeanBuilder.create().displayName("map3").get()).folderName("map3.v001").maxPlayers(4).size(MapSize.valueOf(256, 256)).get(),
      MapVersionBeanBuilder.create().defaultValues().id(3).map(MapBeanBuilder.create().displayName("map4").get()).folderName("map4.v001").maxPlayers(4).size(MapSize.valueOf(512, 512)).get()
  );

  @BeforeEach
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
    assertEquals("map2", filteredMapList.get(0).getMap().getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNumberOfPlayers() {
    runOnFxThreadAndWait(() -> instance.numberOfPlayersTextField.setText("8"));

    assertEquals(1, filteredMapList.size());
    assertEquals("map1", filteredMapList.get(0).getMap().getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByMapWidthSize() {
    runOnFxThreadAndWait(() -> instance.mapWidthInKmTextField.setText("5"));

    assertEquals(1, filteredMapList.size());
    assertEquals("map3", filteredMapList.get(0).getMap().getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNameAndNumberOfPLayers() {
    runOnFxThreadAndWait(() -> {
      instance.mapWidthInKmTextField.setText("10");
      instance.numberOfPlayersTextField.setText("4");
    });

    assertEquals(1, filteredMapList.size());
    assertEquals("map4", filteredMapList.get(0).getMap().getDisplayName());
    verifyFilterAppliedProperty(true);
  }

  @Test
  public void testFilterByNameAndMapHeightSize() {
    runOnFxThreadAndWait(() -> {
      mapNameTextField.setText("map");
      instance.mapHeightInKmTextField.setText("20");
    });

    assertEquals(1, filteredMapList.size());
    assertEquals("map2", filteredMapList.get(0).getMap().getDisplayName());
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
