package com.faforever.client.map;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class MapCardControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private MapService mapService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private I18n i18n;
  @Mock
  private ReviewsController<MapVersionReviewBean> reviewsController;
  @Mock
  private ReviewController<MapVersionReviewBean> reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @InjectMocks
  private MapCardController instance;
  private MapVersionBean mapBean;

  private final SimpleBooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    doAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0))).when(imageViewHelper)
        .createPlaceholderImageOnErrorObservable(any());

    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    lenient().when(mapService.isInstalledBinding(Mockito.<ObservableValue<MapVersionBean>>any())).thenReturn(installed);
    lenient().when(starsController.valueProperty()).thenReturn(new SimpleFloatProperty());
    lenient().when(mapService.downloadAndInstallMap(any(), isNull(), isNull())).thenReturn(Mono.empty());
    lenient().when(mapService.uninstallMap(any())).thenReturn(Mono.empty());
    mapBean = Instancio.of(MapVersionBean.class)
                       .set(field(MapVersionBean::folderName), "testMap")
                       .set(field(MapVersionBean::ranked), true)
                       .set(field(MapVersionBean::id), 23)
                       .set(field(MapVersionBean::size), MapSize.valueOf(1, 1))
                       .create();
    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(i18n.get("versionFormat", mapBean.version().getCanonical())).thenReturn("v10");

    loadFxml("theme/vault/map/map_card.fxml", param -> {
      if (param == ReviewsController.class) {
        return reviewsController;
      }
      if (param == ReviewController.class) {
        return reviewController;
      }
      if (param == StarsController.class) {
        return starsController;
      }
      if (param == StarController.class) {
        return starController;
      }
      return instance;
    });
  }

  @Test
  public void testSetMap() {
    when(mapService.loadPreview(mapBean, PreviewSize.SMALL)).thenReturn(new Image(InputStream.nullInputStream()));

    runOnFxThreadAndWait(() -> instance.setEntity(mapBean));

    assertThat(instance.nameLabel.getText(), is(mapBean.map().displayName()));
    assertThat(instance.authorLabel.getText(), is(mapBean.map().author().getUsername()));
    assertThat(instance.versionLabel.getText(), is("v10"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
  }

  @Test
  public void installedButtonVisibility() {
    runOnFxThreadAndWait(() -> instance.setEntity(mapBean));
    assertFalse(instance.uninstallButton.isVisible());
    assertTrue(instance.installButton.isVisible());

    runOnFxThreadAndWait(() -> installed.set(true));
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }

  @Test
  public void officialMapButtonVisibility() {
    when(mapService.isOfficialMap(mapBean)).thenReturn(true);
    runOnFxThreadAndWait(() -> instance.setEntity(mapBean));

    assertFalse(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }
}

