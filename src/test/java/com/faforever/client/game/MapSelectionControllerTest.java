package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.GeneratorOptions;
import com.faforever.client.map.generator.MapGenerationResult;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class MapSelectionControllerTest extends PlatformTest {
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;

  private MapSelectionController controller;

  @BeforeEach
  public void setUp() throws Exception {
    controller = new MapSelectionController(i18n, mapService);

    loadFxml("theme/play/generate_map_selection.fxml", _ -> controller);
  }

  @Test
  public void testSetMapResults() {
    // Create test results
    Path tempDir = Path.of("test_maps");
    Path mapDir1 = tempDir.resolve("map1");
    Path mapDir2 = tempDir.resolve("map2");
    
    GeneratorOptions options = GeneratorOptions.builder().build();
    
    MapGenerationResult result1 = new MapGenerationResult(
        "neroxis_map_generator_2.0.0_seed1",
        options,
        mapDir1, Optional.empty()
    );
    
    MapGenerationResult result2 = new MapGenerationResult(
        "neroxis_map_generator_2.0.0_seed2",
        options,
        mapDir2, Optional.empty()
    );

    List<MapGenerationResult> results = List.of(result1, result2);
    controller.setMapResults(results);

    assertEquals(2, controller.mapsGrid.getChildren().size());
  }

  @Test
  public void testSetMapResultsEmpty() {
    controller.setMapResults(List.of());

    assertTrue(controller.mapsGrid.getChildren().isEmpty());
  }

  @Test
  public void testSelectButtonClicked() {
    Path tempDir = Path.of("test_maps");
    Path mapDir = tempDir.resolve("test_map");

    GeneratorOptions options = GeneratorOptions.builder().build();

    MapGenerationResult result = new MapGenerationResult(
        "neroxis_map_generator_2.0.0_test",
        options,
        mapDir, Optional.empty()
    );

    controller.setMapResults(List.of(result));

    // Set selected result manually since we can't simulate UI click
    controller.setSelectedResult(result);

    assertTrue(controller.mapsGrid.getChildren().getFirst().getStyleClass().contains("selected"));
  }
}
