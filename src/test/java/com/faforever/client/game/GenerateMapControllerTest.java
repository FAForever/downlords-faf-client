package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.GenerationType;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenerateMapControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private
  NotificationService notificationService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private I18n i18n;
  @Mock
  private CreateGameController createGameController;

  private Preferences preferences;
  private GenerateMapController instance;

  public void unbindProperties() {
    preferences.getGeneratorPrefs().spawnCountPropertyProperty().unbind();
    preferences.getGeneratorPrefs().mapSizePropertyProperty().unbind();
    preferences.getGeneratorPrefs().waterRandomPropertyProperty().unbind();
    preferences.getGeneratorPrefs().plateauRandomPropertyProperty().unbind();
    preferences.getGeneratorPrefs().mountainRandomPropertyProperty().unbind();
    preferences.getGeneratorPrefs().rampRandomPropertyProperty().unbind();
    preferences.getGeneratorPrefs().waterDensityPropertyProperty().unbind();
    preferences.getGeneratorPrefs().plateauDensityPropertyProperty().unbind();
    preferences.getGeneratorPrefs().mountainDensityPropertyProperty().unbind();
    preferences.getGeneratorPrefs().rampDensityPropertyProperty().unbind();
  }

  @Before
  public void setUp() throws Exception {
    instance = new GenerateMapController(preferencesService, notificationService, mapGeneratorService, i18n);

    preferences = new Preferences();
    preferences.getForgedAlliance().setInstallationPath(Paths.get("."));
    when(preferencesService.getPreferences()).thenReturn(preferences);

    loadFxml("theme/play/generate_map.fxml", clazz -> instance);
    unbindProperties();
  }

  @Test
  public void testBadMapNameFails() {
    doNothing().when(notificationService).addImmediateErrorNotification(any(IllegalArgumentException.class), anyString());

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();
    instance.previousMapName.setText("Bad");
    instance.onGenerateMap();

    verify(notificationService).addImmediateErrorNotification(any(IllegalArgumentException.class), eq("mapGenerator.invalidName"));
  }

  @Test
  public void testSetLastSpawnCount() {
    preferences.getGeneratorPrefs().setSpawnCountProperty(10);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.spawnCountSpinner.getValue().intValue(), 10);
  }

  @Test
  public void testSetLastMapSize() {
    preferences.getGeneratorPrefs().setMapSizeProperty("10km");
    preferences.getGeneratorPrefs().setSpawnCountProperty(8);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.mapSizeSpinner.getValue(), "10km");
    assertEquals((int) instance.spawnCountSpinner.getValue(), 8);
  }

  @Test
  public void testSetLastWaterRandom() {
    preferences.getGeneratorPrefs().setWaterRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.waterRandom.isSelected());
  }

  @Test
  public void testSetLastPlateauRandom() {
    preferences.getGeneratorPrefs().setPlateauRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.plateauRandom.isSelected());
  }

  @Test
  public void testSetLastMountainRandom() {
    preferences.getGeneratorPrefs().setMountainRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.mountainRandom.isSelected());
  }

  @Test
  public void testSetLastRampRandom() {
    preferences.getGeneratorPrefs().setRampRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.rampRandom.isSelected());
  }

  @Test
  public void testSetLastWaterSlider() {
    preferences.getGeneratorPrefs().setWaterDensityProperty(71);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.waterSlider.getValue(), 71, 0);
  }

  @Test
  public void testSetLastMountainSlider() {
    preferences.getGeneratorPrefs().setMountainDensityProperty(71);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.mountainSlider.getValue(), 71, 0);
  }

  @Test
  public void testSetLastPlateauSlider() {
    preferences.getGeneratorPrefs().setPlateauDensityProperty(71);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.plateauSlider.getValue(), 71, 0);
  }

  @Test
  public void testSetLastRampSlider() {
    preferences.getGeneratorPrefs().setRampDensityProperty(71);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.rampSlider.getValue(), 71, 0);
  }

  @Test
  public void testWaterSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setWaterRandomProperty(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.waterSliderBox.isVisible());
  }

  @Test
  public void testPlateauSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setPlateauRandomProperty(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.plateauSliderBox.isVisible());
  }

  @Test
  public void testMountainSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setMountainRandomProperty(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.mountainSliderBox.isVisible());
  }

  @Test
  public void testRampSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setRampRandomProperty(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.rampSliderBox.isVisible());
  }

  @Test
  public void testWaterSliderVisibilityWhenNotRandom() {
    preferences.getGeneratorPrefs().setWaterRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.waterSliderBox.isVisible());
  }

  @Test
  public void testPlateauSliderVisibilityWhenNotRandom() {
    preferences.getGeneratorPrefs().setPlateauRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.plateauSliderBox.isVisible());
  }

  @Test
  public void testMountainSliderVisibilityWhenNotRandom() {

    preferences.getGeneratorPrefs().setMountainRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.mountainSliderBox.isVisible());
  }

  @Test
  public void testRampSliderVisibilityWhenNotRandom() {
    preferences.getGeneratorPrefs().setRampRandomProperty(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.rampSliderBox.isVisible());
  }

  @Test
  public void testOptionsNotDisabledWithoutMapName() {
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();
    instance.previousMapName.setText("neroxis_map_generator");
    instance.previousMapName.setText("");

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertFalse(instance.rampRandomBox.isDisabled());
    assertFalse(instance.rampSliderBox.isDisabled());
    assertFalse(instance.waterRandomBox.isDisabled());
    assertFalse(instance.waterSliderBox.isDisabled());
    assertFalse(instance.plateauRandomBox.isDisabled());
    assertFalse(instance.plateauSliderBox.isDisabled());
    assertFalse(instance.mountainRandomBox.isDisabled());
    assertFalse(instance.mountainSliderBox.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithMapName() {
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();
    instance.previousMapName.setText("neroxis_map_generator");

    assertTrue(instance.generationTypeComboBox.isDisabled());
    assertTrue(instance.rampRandomBox.isDisabled());
    assertTrue(instance.rampSliderBox.isDisabled());
    assertTrue(instance.waterRandomBox.isDisabled());
    assertTrue(instance.waterSliderBox.isDisabled());
    assertTrue(instance.plateauRandomBox.isDisabled());
    assertTrue(instance.plateauSliderBox.isDisabled());
    assertTrue(instance.mountainRandomBox.isDisabled());
    assertTrue(instance.mountainSliderBox.isDisabled());
  }

  @Test
  public void testOptionsNotDisabledWithCasual() {
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();
    instance.generationTypeComboBox.setValue(GenerationType.TOURNAMENT);
    instance.generationTypeComboBox.setValue(GenerationType.CASUAL);

    assertFalse(instance.rampRandomBox.isDisabled());
    assertFalse(instance.rampSliderBox.isDisabled());
    assertFalse(instance.waterRandomBox.isDisabled());
    assertFalse(instance.waterSliderBox.isDisabled());
    assertFalse(instance.plateauRandomBox.isDisabled());
    assertFalse(instance.plateauSliderBox.isDisabled());
    assertFalse(instance.mountainRandomBox.isDisabled());
    assertFalse(instance.mountainSliderBox.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithTournament() {
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();
    instance.generationTypeComboBox.setValue(GenerationType.TOURNAMENT);

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertTrue(instance.rampRandomBox.isDisabled());
    assertTrue(instance.rampSliderBox.isDisabled());
    assertTrue(instance.waterRandomBox.isDisabled());
    assertTrue(instance.waterSliderBox.isDisabled());
    assertTrue(instance.plateauRandomBox.isDisabled());
    assertTrue(instance.plateauSliderBox.isDisabled());
    assertTrue(instance.mountainRandomBox.isDisabled());
    assertTrue(instance.mountainSliderBox.isDisabled());
  }

  @Test
  public void testOptionsDisabledWithBlind() {
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();
    instance.generationTypeComboBox.setValue(GenerationType.BLIND);

    assertFalse(instance.generationTypeComboBox.isDisabled());
    assertTrue(instance.rampRandomBox.isDisabled());
    assertTrue(instance.rampSliderBox.isDisabled());
    assertTrue(instance.waterRandomBox.isDisabled());
    assertTrue(instance.waterSliderBox.isDisabled());
    assertTrue(instance.plateauRandomBox.isDisabled());
    assertTrue(instance.plateauSliderBox.isDisabled());
    assertTrue(instance.mountainRandomBox.isDisabled());
    assertTrue(instance.mountainSliderBox.isDisabled());
  }

  @Test
  public void testGetOptionMap() {
    preferences.getGeneratorPrefs().setWaterRandomProperty(false);
    preferences.getGeneratorPrefs().setMountainRandomProperty(false);
    preferences.getGeneratorPrefs().setPlateauRandomProperty(false);
    preferences.getGeneratorPrefs().setRampRandomProperty(false);
    preferences.getGeneratorPrefs().setWaterDensityProperty(1);
    preferences.getGeneratorPrefs().setPlateauDensityProperty(2);
    preferences.getGeneratorPrefs().setMountainDensityProperty(3);
    preferences.getGeneratorPrefs().setRampDensityProperty(4);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    Map<String, Float> optionMap = instance.getOptionMap();

    assertEquals(optionMap.get("landDensity"), 1 - 1 / 127f, 1 / 127f / 2);
    assertEquals(optionMap.get("plateauDensity"), 2 / 127f, 1 / 127f / 2);
    assertEquals(optionMap.get("mountainDensity"), 3 / 127f, 1 / 127f / 2);
    assertEquals(optionMap.get("rampDensity"), 4 / 127f, 1 / 127f / 2);
  }

  @Test
  public void testGetOptionMapRandom() {
    preferences.getGeneratorPrefs().setWaterRandomProperty(true);
    preferences.getGeneratorPrefs().setMountainRandomProperty(true);
    preferences.getGeneratorPrefs().setPlateauRandomProperty(true);
    preferences.getGeneratorPrefs().setRampRandomProperty(true);

    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    Map<String, Float> optionMap = instance.getOptionMap();

    assertFalse(optionMap.containsKey("landDensity"));
    assertFalse(optionMap.containsKey("mountainDensity"));
    assertFalse(optionMap.containsKey("plateauDensity"));
    assertFalse(optionMap.containsKey("rampDensity"));
  }

  @Test
  public void testOnGenerateMapNoName() {
    preferences.getGeneratorPrefs().setWaterRandomProperty(true);
    preferences.getGeneratorPrefs().setMountainRandomProperty(true);
    preferences.getGeneratorPrefs().setPlateauRandomProperty(true);
    preferences.getGeneratorPrefs().setRampRandomProperty(true);
    preferences.getGeneratorPrefs().setSpawnCountProperty(10);
    preferences.getGeneratorPrefs().setMapSizeProperty("10km");
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    when(mapGeneratorService.generateMap(anyInt(), anyInt(), any(), any())).thenReturn(CompletableFuture.completedFuture("testname"));


    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    instance.setOnCloseButtonClickedListener(() -> {
    });
    instance.setCreateGameController(createGameController);
    instance.onGenerateMap();
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapGeneratorService).generateMap(eq(10), eq(512), eq(new HashMap<>()), eq(GenerationType.CASUAL));
  }
}

