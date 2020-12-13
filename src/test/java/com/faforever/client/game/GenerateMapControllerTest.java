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
    preferences.getGeneratorPrefs().spawnCountProperty().unbind();
    preferences.getGeneratorPrefs().mapSizeProperty().unbind();
    preferences.getGeneratorPrefs().waterRandomProperty().unbind();
    preferences.getGeneratorPrefs().plateauRandomProperty().unbind();
    preferences.getGeneratorPrefs().mountainRandomProperty().unbind();
    preferences.getGeneratorPrefs().rampRandomProperty().unbind();
    preferences.getGeneratorPrefs().waterLowProperty().unbind();
    preferences.getGeneratorPrefs().plateauLowProperty().unbind();
    preferences.getGeneratorPrefs().mountainLowProperty().unbind();
    preferences.getGeneratorPrefs().rampLowProperty().unbind();
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
    preferences.getGeneratorPrefs().setSpawnCount(10);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.spawnCountSpinner.getValue().intValue(), 10);
  }

  @Test
  public void testSetLastMapSize() {
    preferences.getGeneratorPrefs().setMapSize("10km");
    preferences.getGeneratorPrefs().setSpawnCount(8);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.mapSizeSpinner.getValue(), "10km");
    assertEquals((int) instance.spawnCountSpinner.getValue(), 8);
  }

  @Test
  public void testSetLastWaterRandom() {
    preferences.getGeneratorPrefs().setWaterRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.waterRandom.isSelected());
  }

  @Test
  public void testSetLastPlateauRandom() {
    preferences.getGeneratorPrefs().setPlateauRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.plateauRandom.isSelected());
  }

  @Test
  public void testSetLastMountainRandom() {
    preferences.getGeneratorPrefs().setMountainRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.mountainRandom.isSelected());
  }

  @Test
  public void testSetLastRampRandom() {
    preferences.getGeneratorPrefs().setRampRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.rampRandom.isSelected());
  }

  @Test
  public void testSetLastWaterSlider() {
    preferences.getGeneratorPrefs().setWaterLow(71);
    preferences.getGeneratorPrefs().setWaterHigh(91);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.waterSlider.getLowValue(), 71, 0);
    assertEquals(instance.waterSlider.getHighValue(), 91, 0);
  }

  @Test
  public void testSetLastMountainSlider() {
    preferences.getGeneratorPrefs().setMountainLow(71);
    preferences.getGeneratorPrefs().setMountainHigh(91);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.mountainSlider.getLowValue(), 71, 0);
    assertEquals(instance.mountainSlider.getHighValue(), 91, 0);
  }

  @Test
  public void testSetLastPlateauSlider() {
    preferences.getGeneratorPrefs().setPlateauLow(71);
    preferences.getGeneratorPrefs().setPlateauHigh(91);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.plateauSlider.getLowValue(), 71, 0);
    assertEquals(instance.plateauSlider.getHighValue(), 91, 0);
  }

  @Test
  public void testSetLastRampSlider() {
    preferences.getGeneratorPrefs().setRampLow(71);
    preferences.getGeneratorPrefs().setRampHigh(91);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.rampSlider.getLowValue(), 71, 0);
    assertEquals(instance.rampSlider.getHighValue(), 91, 0);
  }

  @Test
  public void testWaterSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setWaterRandom(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.waterSliderBox.isVisible());
  }

  @Test
  public void testPlateauSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setPlateauRandom(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.plateauSliderBox.isVisible());
  }

  @Test
  public void testMountainSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setMountainRandom(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.mountainSliderBox.isVisible());
  }

  @Test
  public void testRampSliderVisibilityWhenRandom() {
    preferences.getGeneratorPrefs().setRampRandom(true);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.rampSliderBox.isVisible());
  }

  @Test
  public void testWaterSliderVisibilityWhenNotRandom() {
    preferences.getGeneratorPrefs().setWaterRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.waterSliderBox.isVisible());
  }

  @Test
  public void testPlateauSliderVisibilityWhenNotRandom() {
    preferences.getGeneratorPrefs().setPlateauRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.plateauSliderBox.isVisible());
  }

  @Test
  public void testMountainSliderVisibilityWhenNotRandom() {

    preferences.getGeneratorPrefs().setMountainRandom(false);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.mountainSliderBox.isVisible());
  }

  @Test
  public void testRampSliderVisibilityWhenNotRandom() {
    preferences.getGeneratorPrefs().setRampRandom(false);
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
  public void testGetOptionMapRange() {
    preferences.getGeneratorPrefs().setWaterRandom(false);
    preferences.getGeneratorPrefs().setMountainRandom(false);
    preferences.getGeneratorPrefs().setPlateauRandom(false);
    preferences.getGeneratorPrefs().setRampRandom(false);
    preferences.getGeneratorPrefs().setWaterLow(1);
    preferences.getGeneratorPrefs().setWaterHigh(10);
    preferences.getGeneratorPrefs().setPlateauLow(11);
    preferences.getGeneratorPrefs().setPlateauHigh(20);
    preferences.getGeneratorPrefs().setMountainLow(21);
    preferences.getGeneratorPrefs().setMountainHigh(30);
    preferences.getGeneratorPrefs().setRampLow(31);
    preferences.getGeneratorPrefs().setRampHigh(40);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    Map<String, Float> optionMap = instance.getOptionMap();

    assertTrue(optionMap.get("landDensity") <= 1 - 1 / 127f && optionMap.get("landDensity") > 1 - 10 / 127f);
    assertTrue(optionMap.get("plateauDensity") >= 11 / 127f && optionMap.get("plateauDensity") < 20 / 127f);
    assertTrue(optionMap.get("mountainDensity") >= 21 / 127f && optionMap.get("mountainDensity") < 30 / 127f);
    assertTrue(optionMap.get("rampDensity") >= 31 / 127f && optionMap.get("rampDensity") < 40 / 127f);
  }

  @Test
  public void testGetOptionMapValue() {
    preferences.getGeneratorPrefs().setWaterRandom(false);
    preferences.getGeneratorPrefs().setMountainRandom(false);
    preferences.getGeneratorPrefs().setPlateauRandom(false);
    preferences.getGeneratorPrefs().setRampRandom(false);
    preferences.getGeneratorPrefs().setWaterLow(11);
    preferences.getGeneratorPrefs().setWaterHigh(11);
    preferences.getGeneratorPrefs().setPlateauLow(10);
    preferences.getGeneratorPrefs().setPlateauHigh(10);
    preferences.getGeneratorPrefs().setMountainLow(12);
    preferences.getGeneratorPrefs().setMountainHigh(12);
    preferences.getGeneratorPrefs().setRampLow(13);
    preferences.getGeneratorPrefs().setRampHigh(13);
    preferences.getForgedAlliance().setInstallationPath(Paths.get(""));

    WaitForAsyncUtils.asyncFx(() -> instance.initialize());
    WaitForAsyncUtils.waitForFxEvents();

    Map<String, Float> optionMap = instance.getOptionMap();

    assertEquals(optionMap.get("landDensity"), 1 - 11 / 127f, 0);
    assertEquals(optionMap.get("plateauDensity"), 10 / 127f, 0);
    assertEquals(optionMap.get("mountainDensity"), 12 / 127f, 0);
    assertEquals(optionMap.get("rampDensity"), 13 / 127f, 0);
  }

  @Test
  public void testGetOptionMapRandom() {
    preferences.getGeneratorPrefs().setWaterRandom(true);
    preferences.getGeneratorPrefs().setMountainRandom(true);
    preferences.getGeneratorPrefs().setPlateauRandom(true);
    preferences.getGeneratorPrefs().setRampRandom(true);

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
    preferences.getGeneratorPrefs().setWaterRandom(true);
    preferences.getGeneratorPrefs().setMountainRandom(true);
    preferences.getGeneratorPrefs().setPlateauRandom(true);
    preferences.getGeneratorPrefs().setRampRandom(true);
    preferences.getGeneratorPrefs().setSpawnCount(10);
    preferences.getGeneratorPrefs().setMapSize("10km");
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

