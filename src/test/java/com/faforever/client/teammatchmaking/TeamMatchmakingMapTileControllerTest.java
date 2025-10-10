package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.test.PlatformTest;
import com.faforever.commons.lobby.VetoData;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    lenient().when(mapService.loadPreview(any(MapVersion.class), any(PreviewSize.class))).thenReturn(new Image(InputStream.nullInputStream()));
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
      instance.bindVetoesBoxProperties();
    });

    assertFalse(instance.vetoesBox.isVisible());
    assertTrue(instance.vetoesBox.isMouseTransparent());
  }

  @Test
  public void testVetoBoxVisibleWhenModeEnabled() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.bindVetoesBoxProperties();
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
      instance.bindVetoesBoxProperties();
      vetoModeEnabled.set(false);
    });

    assertFalse(instance.vetoesBox.isVisible());

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 1, mapPoolAssignment.mapPool().mapPool().id()));
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

    verify(matchmakerPrefs).setVetoData(new VetoData(mapPoolAssignment.id(), 1, mapPoolAssignment.mapPool().mapPool().id()));
  }

  @Test
  public void testRemoveVetoToken() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 2, mapPoolAssignment.mapPool().mapPool().id()));
    });

    runOnFxThreadAndWait(() -> instance.minusButton.fire());

    verify(matchmakerPrefs).setVetoData(new VetoData(mapPoolAssignment.id(), 1, mapPoolAssignment.mapPool().mapPool().id()));
  }

  @Test
  public void testCannotExceedMaxTokens() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(2);
      vetoTokensLeft.set(5);
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 2, mapPoolAssignment.mapPool().mapPool().id()));
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
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 2, mapPoolAssignment.mapPool().mapPool().id()));
    });

    assertTrue(instance.root.getStyleClass().contains("banned"));
  }

  @Test
  public void testBannedStyleRemoved() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(2);
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 2, mapPoolAssignment.mapPool().mapPool().id()));
    });

    assertTrue(instance.root.getStyleClass().contains("banned"));

    runOnFxThreadAndWait(() -> {
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 1, mapPoolAssignment.mapPool().mapPool().id()));
    });

    assertFalse(instance.root.getStyleClass().contains("banned"));
  }

  @Test
  public void testVetoIconActiveColor() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 1, mapPoolAssignment.mapPool().mapPool().id()));
    });

    assertThat(instance.vetoSvg.getFill().toString(), is("0xffd700ff"));
  }

  @Test
  public void testVetoIconInactiveColor() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
    });

    assertThat(instance.vetoSvg.getFill().toString(), is("0xffffffff"));
  }

  @Test
  public void testTokenCountSyncsFromPreferences() {
    runOnFxThreadAndWait(() -> {
      instance.setMapAssignment(mapPoolAssignment);
      instance.setVetoTokensMax(3);
      matchmakerPrefs.setVetoData(new VetoData(mapPoolAssignment.id(), 2, mapPoolAssignment.mapPool().mapPool().id()));
    });

    assertThat(instance.tokenCounterLabel.getText(), is("2"));
  }
}