package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.preferences.VetoKey;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamMatchmakingMapTileControllerTest extends PlatformTest {

  @Mock
  private MapService mapService;
  @Mock
  private I18n i18n;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Spy
  private MatchmakerPrefs matchmakerPrefs;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;

  @InjectMocks
  private TeamMatchmakingMapTileController instance;

  private MapPoolAssignment mapPoolAssignment;
  private SimpleBooleanProperty vetoModeEnabled;
  private SimpleIntegerProperty vetoTokensLeft;

  @BeforeEach
  public void setUp() throws Exception {
    mapPoolAssignment = Instancio.create(MapPoolAssignment.class);
    vetoModeEnabled = new SimpleBooleanProperty(false);
    vetoTokensLeft = new SimpleIntegerProperty(5);

    matchmakerPrefs.getAppliedVetoes().clear();

    lenient().when(mapService.loadPreview(any(MapVersion.class), any(PreviewSize.class)))
             .thenReturn(new Image(InputStream.nullInputStream()));
    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(i18n.get("mapPreview.size", 10, 10)).thenReturn("10km x 10km");

    loadFxml("theme/play/teammatchmaking/matchmaking_map_tile.fxml", clazz -> instance);

    instance.setVetoModeEnabled(vetoModeEnabled);
    instance.setVetoTokensLeft(vetoTokensLeft.asObject());
  }

  @Test
  public void testMapNameDisplay() {
    when(mapGeneratorService.isGeneratedMap(mapPoolAssignment.mapVersion().map().displayName())).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.setMapAssignment(mapPoolAssignment));

    assertThat(instance.nameLabel.getText(), is(mapPoolAssignment.mapVersion().map().displayName()));
  }

  @Test
  public void testMapAuthorDisplay() {
    lenient().when(mapGeneratorService.isGeneratedMap(any())).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.setMapAssignment(mapPoolAssignment));

    assertThat(instance.authorLabel.getText(), is(mapPoolAssignment.mapVersion().map().author().getUsername()));
  }

  @Test
  public void testGeneratedMapDisplay() {
    lenient().when(mapGeneratorService.isGeneratedMap(any())).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.setMapAssignment(mapPoolAssignment));

    assertThat(instance.nameLabel.getText(), is("map generator"));
  }

  @Test
  public void testVetoBoxHiddenWhenModeDisabled() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.bindVetoModeDependentProperties();
    });

    assertFalse(instance.vetoesBox.isVisible());
    assertTrue(instance.vetoesBox.isMouseTransparent());
  }

  @Test
  public void testVetoBoxVisibleWhenModeEnabled() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.bindVetoModeDependentProperties();
      vetoModeEnabled.set(true);
    });

    assertTrue(instance.vetoesBox.isVisible());
    assertFalse(instance.vetoesBox.isMouseTransparent());
  }

  @Test
  public void testVetoBoxVisibleWithAppliedTokens() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      instance.bindVetoModeDependentProperties();
      vetoModeEnabled.set(false);
    });

    assertFalse(instance.vetoesBox.isVisible());

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 1);
    });

    assertTrue(instance.vetoesBox.isVisible());
  }

  @Test
  public void testAddVetoToken() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      vetoTokensLeft.set(5);
      instance.vetoButton.fire();
    });

    verify(teamMatchmakingService).setTokensForMap(
        new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 1);
  }

  @Test
  public void testRemoveVetoToken() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 2);
    });

    runOnFxThreadAndWait(() -> instance.minusButton.fire());

    verify(teamMatchmakingService).setTokensForMap(
        new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 1);
  }

  @Test
  public void testCannotExceedMaxTokens() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(2);
      vetoTokensLeft.set(5);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 2);
      instance.vetoButton.fire();
    });

    assertThat(instance.tokenCounterLabel.getText(), is("2"));
  }

  @Test
  public void testCannotExceedAvailableTokens() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(5);
      vetoTokensLeft.set(0);
      instance.vetoButton.fire();
    });

    assertThat(instance.tokenCounterLabel.getText(), is("0"));
  }

  @Test
  public void testBannedStyleApplied() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(2);
      instance.setMaxPerMap(2);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 2);
    });

    assertTrue(instance.root.getStyleClass().contains("tmm-maplist-tile_banned"));
  }

  @Test
  public void testBannedStyleRemoved() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(2);
      instance.setMaxPerMap(2);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 2);
    });

    assertTrue(instance.root.getStyleClass().contains("tmm-maplist-tile_banned"));

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 1);
    });

    assertFalse(instance.root.getStyleClass().contains("tmm-maplist-tile_banned"));
  }

  @Test
  public void testVetoIconActiveColor() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 1);
    });

    assertTrue(instance.vetoSvg.getStyleClass().contains("tmm-maplist-palm_active"));
  }

  @Test
  public void testVetoIconInactiveColor() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
    });

    assertFalse(instance.vetoSvg.getStyleClass().contains("tmm-maplist-palm_active"));
  }

  @Test
  public void testTokenCountSyncsFromPreferences() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 2);
    });

    assertThat(instance.tokenCounterLabel.getText(), is("2"));
  }

  @Test
  public void testMapTileClickTriggersListener() {
    AtomicReference<MapVersion> clickedMap = new AtomicReference<>();

    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setOnTileClickedListener(clickedMap::set);

      MouseEvent mouseEvent = new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false,
                                             false, false, true, false, false, false, false, false, null);
      instance.root.fireEvent(mouseEvent);
    });

    assertNotNull(clickedMap.get());
    assertThat(clickedMap.get(), is(mapPoolAssignment.mapVersion()));
  }

  @Test
  public void testMapTileClickDoesNotTriggerListenerForGeneratedMaps() {
    AtomicReference<MapVersion> clickedMap = new AtomicReference<>();

    runOnFxThreadAndWait(() -> {
      when(mapGeneratorService.isGeneratedMap(mapPoolAssignment.mapVersion().map().displayName())).thenReturn(true);
      instance.setMapAssignment(mapPoolAssignment);
      instance.setOnTileClickedListener(clickedMap::set);

      MouseEvent mouseEvent = new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1, false, false,
                                             false, false, true, false, false, false, false, false, null);
      instance.root.fireEvent(mouseEvent);
    });

    assertNull(clickedMap.get());
  }


  @Test
  public void testVetoButtonClickDoesNotTriggerListener() {
    AtomicReference<MapVersion> clickedMap = new AtomicReference<>();

    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      vetoTokensLeft.set(5);
      instance.setOnTileClickedListener(clickedMap::set);
      instance.vetoButton.fire();
    });

    assertNull(clickedMap.get());
  }

  @Test
  public void testMinusButtonClickDoesNotTriggerListener() {
    AtomicReference<MapVersion> clickedMap = new AtomicReference<>();

    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.getAppliedVetoes().put(
          new VetoKey(mapPoolAssignment.mapPool().mapPool().id(), mapPoolAssignment.id()), 2);
      instance.setOnTileClickedListener(clickedMap::set);
      instance.minusButton.fire();
    });

    assertNull(clickedMap.get());
  }
}