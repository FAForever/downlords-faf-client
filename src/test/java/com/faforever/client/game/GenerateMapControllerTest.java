package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.GenerationType;
import com.faforever.client.map.generator.GeneratorOptions;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.test.PlatformTest;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenerateMapControllerTest extends PlatformTest {


  @Mock
  private
  NotificationService notificationService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private I18n i18n;
  @Spy
  private GeneratorPrefs generatorPrefs;

  @InjectMocks
  private GenerateMapController instance;

  public void unbindProperties() {
    generatorPrefs.generationTypeProperty().unbind();
    generatorPrefs.spawnCountProperty().unbind();
    generatorPrefs.mapSizeInKmProperty().unbind();
    generatorPrefs.numTeamsProperty().unbind();
    generatorPrefs.seedProperty().unbind();
    generatorPrefs.fixedSeedProperty().unbind();
    generatorPrefs.customStyleProperty().unbind();
    generatorPrefs.commandLineArgsProperty().unbind();
    generatorPrefs.reclaimDensityMinProperty().unbind();
    generatorPrefs.reclaimDensityMaxProperty().unbind();
    generatorPrefs.resourceDensityMinProperty().unbind();
    generatorPrefs.resourceDensityMaxProperty().unbind();
  }

  @BeforeEach
  public void setUp() throws Exception {
    generatorPrefs.setSpawnCount(10);
    generatorPrefs.setMapSizeInKm(10.0);

    loadFxml("theme/play/generate_map.fxml", clazz -> instance);
    unbindProperties();
  }

  @Test
  public void testBadMapNameFails() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.previousMapName.setText("Bad");
    instance.onGenerateMap();

    verify(notificationService).addImmediateWarnNotification("mapGenerator.invalidName");
  }

  @Test
  public void testSetLastSpawnCount() {
    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertEquals(10, instance.spawnCountSpinner.getValue().intValue());
  }

  @Test
  public void testSetLastNumTeams() {
    generatorPrefs.setNumTeams(5);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertEquals(5, instance.numTeamsSpinner.getValue().intValue());
  }

  @Test
  public void testSetLastMapSize() {

    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertEquals(10.0, instance.mapSizeSpinner.getValue());
    assertEquals(10, (int) instance.spawnCountSpinner.getValue());
  }

  @Test
  public void testSetLastSymmetry() {
    generatorPrefs.getSymmetries().setAll("Test");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.setSymmetries(new ArrayList<>(List.of("Test")));

    assertEquals(instance.symmetryCheckComboBox.getCheckModel().getCheckedItems(), List.of("Test"));
  }

  @Test
  public void testSetLastFixedSeed() {
    generatorPrefs.setFixedSeed(true);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertTrue(instance.fixedSeedCheckBox.isSelected());
  }

  @Test
  public void testSetLastSeed() {
    generatorPrefs.setSeed("100");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertEquals("100", instance.seedTextField.getText());
  }

  @Test
  public void testSetLastMapStyle() {
    generatorPrefs.getMapStyles().setAll("TEST");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.setStyles(new ArrayList<>(List.of("TEST")));

    assertEquals(List.of("TEST"), instance.mapStyleCheckComboBox.getCheckModel().getCheckedItems());
  }

  @Test
  public void testSetLastCustomStyle() {
    generatorPrefs.setCustomStyle(true);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertTrue(instance.customStyleCheckBox.isSelected());
  }

  @Test
  public void testSetLastTerrainStyle() {
    generatorPrefs.getTerrainStyles().setAll("TEST");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.setTerrainStyles(new ArrayList<>(List.of("TEST")));

    assertEquals(List.of("TEST"), instance.terrainCheckComboBox.getCheckModel().getCheckedItems());
  }

  @Test
  public void testSetLastTextureStyle() {
    generatorPrefs.getTextureStyles().setAll("TEST");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.setTextureStyles(new ArrayList<>(List.of("TEST")));

    assertEquals(List.of("TEST"), instance.biomeCheckComboBox.getCheckModel().getCheckedItems());
  }

  @Test
  public void testSetLastResourceStyle() {
    generatorPrefs.getResourceStyles().setAll("TEST");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.setResourceStyles(new ArrayList<>(List.of("TEST")));

    assertEquals(List.of("TEST"), instance.resourcesCheckComboBox.getCheckModel().getCheckedItems());
  }

  @Test
  public void testSetLastPropStyle() {
    generatorPrefs.getPropStyles().setAll("TEST");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.setPropStyles(new ArrayList<>(List.of("TEST")));

    assertEquals(List.of("TEST"), instance.propsCheckComboBox.getCheckModel().getCheckedItems());
  }

  @Test
  public void testSetLastCommandLineArgs() {
    generatorPrefs.setCommandLineArgs("--help");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertEquals("--help", instance.commandLineArgsText.getText());
    assertTrue(instance.commandLineArgsText.isVisible());
    assertTrue(instance.commandLineLabel.isVisible());
  }

  @Test
  public void testCommandLineArgsNotVisibleWhenNotSetInitially() {
    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertFalse(instance.commandLineArgsText.isVisible());
    assertFalse(instance.commandLineLabel.isVisible());
  }

  @Test
  public void testToggleCommandLineArgs() {
    runOnFxThreadAndWait(() -> reinitialize(instance));

    assertFalse(instance.commandLineArgsText.isVisible());
    assertFalse(instance.commandLineLabel.isVisible());

    runOnFxThreadAndWait(() -> instance.toggleCommandlineInput());

    assertTrue(instance.commandLineArgsText.isVisible());
    assertTrue(instance.commandLineLabel.isVisible());
  }

  @Test
  public void testOptionsNotDisabledWithoutMapName() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.previousMapName.setText("neroxis_map_generator");
    instance.previousMapName.setText("");

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertFalse(instance.spawnCountSpinner.isDisabled());
    assertFalse(instance.numTeamsSpinner.isDisabled());
    assertFalse(instance.mapSizeSpinner.isDisabled());
    assertFalse(instance.symmetryCheckComboBox.isDisabled());
    assertFalse(instance.fixedSeedCheckBox.isDisabled());
    assertFalse(instance.customStyleCheckBox.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithMapName() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.previousMapName.setText("neroxis_map_generator");
    instance.customStyleCheckBox.setSelected(true);

    assertTrue(instance.commandLineArgsText.isDisabled());
    assertTrue(instance.generationTypeComboBox.isDisabled());
    assertTrue(instance.spawnCountSpinner.isDisabled());
    assertTrue(instance.numTeamsSpinner.isDisabled());
    assertTrue(instance.mapSizeSpinner.isDisabled());
    assertTrue(instance.symmetryCheckComboBox.isDisabled());
    assertTrue(instance.fixedSeedCheckBox.isDisabled());
    assertTrue(instance.seedTextField.isDisabled());
    assertTrue(instance.seedRerollButton.isDisabled());
    assertTrue(instance.mapStyleCheckComboBox.isDisabled());
    assertTrue(instance.customStyleCheckBox.isDisabled());
    assertTrue(instance.terrainCheckComboBox.isDisabled());
    assertTrue(instance.biomeCheckComboBox.isDisabled());
    assertTrue(instance.resourcesCheckComboBox.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
    assertTrue(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithCommandLine() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.commandLineArgsText.setText("--help");
    instance.customStyleCheckBox.setSelected(true);

    assertTrue(instance.generationTypeComboBox.isDisabled());
    assertTrue(instance.spawnCountSpinner.isDisabled());
    assertTrue(instance.numTeamsSpinner.isDisabled());
    assertTrue(instance.mapSizeSpinner.isDisabled());
    assertTrue(instance.symmetryCheckComboBox.isDisabled());
    assertTrue(instance.fixedSeedCheckBox.isDisabled());
    assertTrue(instance.seedTextField.isDisabled());
    assertTrue(instance.seedRerollButton.isDisabled());
    assertTrue(instance.mapStyleCheckComboBox.isDisabled());
    assertTrue(instance.customStyleCheckBox.isDisabled());
    assertTrue(instance.terrainCheckComboBox.isDisabled());
    assertTrue(instance.biomeCheckComboBox.isDisabled());
    assertTrue(instance.resourcesCheckComboBox.isDisabled());
    assertTrue(instance.propsCheckComboBox.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
    assertTrue(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithoutCustomStyle() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.customStyleCheckBox.setSelected(false);

    assertFalse(instance.mapStyleCheckComboBox.isDisabled());
    assertTrue(instance.terrainCheckComboBox.isDisabled());
    assertTrue(instance.biomeCheckComboBox.isDisabled());
    assertTrue(instance.resourcesCheckComboBox.isDisabled());
    assertTrue(instance.propsCheckComboBox.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
    assertTrue(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testOptionsNotDisabledWithCustomStyle() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.customStyleCheckBox.setSelected(true);

    assertTrue(instance.mapStyleCheckComboBox.isDisabled());
    assertFalse(instance.terrainCheckComboBox.isDisabled());
    assertFalse(instance.biomeCheckComboBox.isDisabled());
    assertFalse(instance.resourcesCheckComboBox.isDisabled());
    assertFalse(instance.propsCheckComboBox.isDisabled());
    assertFalse(instance.resourcesDensitySlider.isDisabled());
    assertFalse(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testSeedDisabledWithoutFixedSeed() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.fixedSeedCheckBox.setSelected(false);

    assertTrue(instance.seedTextField.isDisabled());
    assertTrue(instance.seedRerollButton.isDisabled());
  }

  @Test
  public void testSeedNotDisabledWithFixedSeed() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.fixedSeedCheckBox.setSelected(true);

    assertFalse(instance.seedTextField.isDisabled());
    assertFalse(instance.seedRerollButton.isDisabled());
  }

  @Test
  public void testOptionsNotDisabledWithCasual() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.generationTypeComboBox.setValue(GenerationType.TOURNAMENT);
    instance.generationTypeComboBox.setValue(GenerationType.CASUAL);

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertFalse(instance.spawnCountSpinner.isDisabled());
    assertFalse(instance.numTeamsSpinner.isDisabled());
    assertFalse(instance.mapSizeSpinner.isDisabled());
    assertFalse(instance.symmetryCheckComboBox.isDisabled());
    assertFalse(instance.fixedSeedCheckBox.isDisabled());
    assertFalse(instance.customStyleCheckBox.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithTournament() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.generationTypeComboBox.setValue(GenerationType.TOURNAMENT);
    instance.customStyleCheckBox.setSelected(true);

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertFalse(instance.spawnCountSpinner.isDisabled());
    assertFalse(instance.numTeamsSpinner.isDisabled());
    assertFalse(instance.mapSizeSpinner.isDisabled());
    assertTrue(instance.symmetryCheckComboBox.isDisabled());
    assertTrue(instance.fixedSeedCheckBox.isDisabled());
    assertTrue(instance.seedTextField.isDisabled());
    assertTrue(instance.seedRerollButton.isDisabled());
    assertTrue(instance.mapStyleCheckComboBox.isDisabled());
    assertTrue(instance.customStyleCheckBox.isDisabled());
    assertTrue(instance.terrainCheckComboBox.isDisabled());
    assertTrue(instance.biomeCheckComboBox.isDisabled());
    assertTrue(instance.resourcesCheckComboBox.isDisabled());
    assertTrue(instance.propsCheckComboBox.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
    assertTrue(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithBlind() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.generationTypeComboBox.setValue(GenerationType.BLIND);
    instance.customStyleCheckBox.setSelected(true);

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertFalse(instance.spawnCountSpinner.isDisabled());
    assertFalse(instance.numTeamsSpinner.isDisabled());
    assertFalse(instance.mapSizeSpinner.isDisabled());
    assertTrue(instance.symmetryCheckComboBox.isDisabled());
    assertTrue(instance.fixedSeedCheckBox.isDisabled());
    assertTrue(instance.seedTextField.isDisabled());
    assertTrue(instance.seedRerollButton.isDisabled());
    assertTrue(instance.mapStyleCheckComboBox.isDisabled());
    assertTrue(instance.customStyleCheckBox.isDisabled());
    assertTrue(instance.terrainCheckComboBox.isDisabled());
    assertTrue(instance.biomeCheckComboBox.isDisabled());
    assertTrue(instance.resourcesCheckComboBox.isDisabled());
    assertTrue(instance.propsCheckComboBox.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
    assertTrue(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithUnexplored() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.generationTypeComboBox.setValue(GenerationType.UNEXPLORED);
    instance.customStyleCheckBox.setSelected(true);

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertFalse(instance.spawnCountSpinner.isDisabled());
    assertFalse(instance.numTeamsSpinner.isDisabled());
    assertFalse(instance.mapSizeSpinner.isDisabled());
    assertTrue(instance.symmetryCheckComboBox.isDisabled());
    assertTrue(instance.fixedSeedCheckBox.isDisabled());
    assertTrue(instance.seedTextField.isDisabled());
    assertTrue(instance.seedRerollButton.isDisabled());
    assertTrue(instance.mapStyleCheckComboBox.isDisabled());
    assertTrue(instance.customStyleCheckBox.isDisabled());
    assertTrue(instance.terrainCheckComboBox.isDisabled());
    assertTrue(instance.biomeCheckComboBox.isDisabled());
    assertTrue(instance.resourcesCheckComboBox.isDisabled());
    assertTrue(instance.propsCheckComboBox.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
    assertTrue(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testGetGenerateMapWithName() {
    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.previousMapName.setText("neroxis_map_generator_0.0.0_12345");
    instance.setOnCloseButtonClickedListener(() -> {});
    when(mapGeneratorService.isGeneratedMap("neroxis_map_generator_0.0.0_12345")).thenReturn(true);
    when(mapGeneratorService.generateMap(anyString())).thenReturn(Mono.empty());

    runOnFxThreadAndWait(() -> instance.onGenerateMap());

    verify(mapGeneratorService).generateMap("neroxis_map_generator_0.0.0_12345");
    verify(mapGeneratorService, never()).generateMap(any(GeneratorOptions.class));
  }

  @Test
  public void testGetGenerateMapNoNameCustomStyle() {
    generatorPrefs.setCustomStyle(true);
    generatorPrefs.setFixedSeed(true);
    generatorPrefs.setSeed("100");
    generatorPrefs.setSpawnCount(2);
    generatorPrefs.setNumTeams(2);
    generatorPrefs.setMapSizeInKm(10.0);
    generatorPrefs.setGenerationType(GenerationType.CASUAL);
    generatorPrefs.setResourceDensityMin(5);
    generatorPrefs.setResourceDensityMax(5);
    generatorPrefs.setReclaimDensityMin(10);
    generatorPrefs.setReclaimDensityMax(10);

    instance.symmetryCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("SYMMETRY")));
    instance.symmetryCheckComboBox.getCheckModel().check(0);
    instance.terrainCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("TERRAIN")));
    instance.terrainCheckComboBox.getCheckModel().check(0);
    instance.biomeCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("BIOME")));
    instance.biomeCheckComboBox.getCheckModel().check(0);
    instance.resourcesCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("MEXES")));
    instance.resourcesCheckComboBox.getCheckModel().check(0);
    instance.propsCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("PROPS")));
    instance.propsCheckComboBox.getCheckModel().check(0);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    ArgumentCaptor<GeneratorOptions> captor = ArgumentCaptor.forClass(GeneratorOptions.class);

    runOnFxThreadAndWait(() -> instance.onGenerateMap());

    verify(mapGeneratorService).generateMap(captor.capture());

    GeneratorOptions result = captor.getValue();

    assertEquals("100", result.seed());
    assertEquals("SYMMETRY", result.symmetry());
    assertEquals(2, result.spawnCount());
    assertEquals(512, result.mapSize());
    assertEquals(2, result.numTeams());
    assertEquals(GenerationType.CASUAL, result.generationType());
    assertNull(result.commandLineArgs());
    assertEquals("TERRAIN", result.terrainStyle());
    assertEquals("BIOME", result.textureStyle());
    assertEquals("MEXES", result.resourceStyle());
    assertEquals("PROPS", result.propStyle());
    assertEquals(10 / 127f, result.reclaimDensity());
    assertEquals(5 / 127f, result.resourceDensity());
  }

  @Test
  public void testGetGenerateMapWithCommandLineArgs() {
    generatorPrefs.setCommandLineArgs("--test");

    runOnFxThreadAndWait(() -> reinitialize(instance));

    ArgumentCaptor<GeneratorOptions> captor = ArgumentCaptor.forClass(GeneratorOptions.class);

    runOnFxThreadAndWait(() -> instance.onGenerateMap());

    verify(mapGeneratorService).generateMap(captor.capture());

    GeneratorOptions result = captor.getValue();

    assertEquals("--test", result.commandLineArgs());
  }

  @Test
  public void testGetGenerateMapNoNameMapStyle() {
    generatorPrefs.setCustomStyle(false);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    ArgumentCaptor<GeneratorOptions> captor = ArgumentCaptor.forClass(GeneratorOptions.class);

    runOnFxThreadAndWait(() -> instance.onGenerateMap());

    verify(mapGeneratorService).generateMap(captor.capture());

    GeneratorOptions result = captor.getValue();

    assertNull(result.terrainStyle());
    assertNull(result.textureStyle());
    assertNull(result.resourceStyle());
    assertNull(result.propStyle());
    assertNull(result.resourceDensity());
    assertNull(result.reclaimDensity());
  }

  @Test
  public void testResourceComboRandomSliderDisabled(){
    generatorPrefs.setCustomStyle(true);
    instance.resourcesCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("RESOURCES")));
    instance.propsCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("OPTIMUS")));
    instance.propsCheckComboBox.getCheckModel().check(0);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    ArgumentCaptor<GeneratorOptions> captor = ArgumentCaptor.forClass(GeneratorOptions.class);

    runOnFxThreadAndWait(() -> instance.onGenerateMap());

    verify(mapGeneratorService).generateMap(captor.capture());

    GeneratorOptions result = captor.getValue();
    assertNull(result.resourceStyle());
    assertEquals("RANDOM", instance.resourcesCheckComboBox.getTitle());
    assertEquals("OPTIMUS", result.propStyle());
    assertFalse(instance.resourcesDensitySlider.isDisabled());
    assertFalse(instance.reclaimDensitySlider.isDisabled());
  }

  @Test
  public void testPropComboRandomSliderDisabled(){
    generatorPrefs.setCustomStyle(true);
    instance.propsCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("PROPS")));
    instance.resourcesCheckComboBox.getItems().setAll(FXCollections.observableList(List.of("OPTIMUS")));
    instance.resourcesCheckComboBox.getCheckModel().check(0);

    runOnFxThreadAndWait(() -> reinitialize(instance));

    ArgumentCaptor<GeneratorOptions> captor = ArgumentCaptor.forClass(GeneratorOptions.class);

    runOnFxThreadAndWait(() -> instance.onGenerateMap());

    verify(mapGeneratorService).generateMap(captor.capture());

    GeneratorOptions result = captor.getValue();
    assertNull(result.propStyle());
    assertEquals("RANDOM", instance.propsCheckComboBox.getTitle());
    assertEquals("OPTIMUS", result.resourceStyle());
    assertFalse(instance.reclaimDensitySlider.isDisabled());
    assertFalse(instance.resourcesDensitySlider.isDisabled());
  }

  @Test
  public void testCustomStyleFalseAndPropAndResourceComboNotRandomSlidersDisabled(){

    runOnFxThreadAndWait(() -> reinitialize(instance));
    instance.customStyleCheckBox.setSelected(false);
    instance.setPropStyles(new ArrayList<>(List.of("Maximus")));
    instance.setResourceStyles(new ArrayList<>(List.of("Decimus")));


    assertTrue(instance.reclaimDensitySlider.isDisabled());
    assertTrue(instance.resourcesDensitySlider.isDisabled());
  }
}

