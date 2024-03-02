package com.faforever.client.mod;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.function.Consumer;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModCardControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private ModService modService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  @InjectMocks
  private ModCardController instance;
  private ModVersionBean modVersion;

  private final SimpleBooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    lenient().doAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0))).when(imageViewHelper)
        .createPlaceholderImageOnErrorObservable(any());

    lenient().when(modService.isInstalledBinding(Mockito.any())).thenReturn(installed);
    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    lenient().when(starsController.valueProperty()).thenReturn(new SimpleFloatProperty());
    lenient().when(i18n.get(ModType.UI.getI18nKey())).thenReturn(ModType.UI.name());

    modVersion = Instancio.create(ModVersionBean.class);

    loadFxml("theme/vault/mod/mod_card.fxml", clazz -> {
      if (clazz == StarsController.class) {
        return starsController;
      }
      if (clazz == StarController.class) {
        return starController;
      }
      return instance;
    });
  }

  @Test
  public void testSetMod() {
    when(modService.loadThumbnail(modVersion)).thenReturn(new Image("/theme/images/default_achievement.png"));
    runOnFxThreadAndWait(() -> instance.setEntity(modVersion));

    assertEquals(modVersion.mod().displayName(), instance.nameLabel.getText());
    assertEquals(modVersion.mod().author(), instance.authorLabel.getText());
    assertNotNull(instance.thumbnailImageView.getImage());
    verify(modService).loadThumbnail(modVersion);
  }

  @Test
  public void testSetModNoThumbnail() {
    Image image = new Image(InputStream.nullInputStream());
    when(modService.loadThumbnail(modVersion)).thenReturn(image);

    runOnFxThreadAndWait(() -> instance.setEntity(modVersion));

    assertNotNull(instance.thumbnailImageView.getImage());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.modTileRoot, instance.getRoot());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testShowModDetail() {
    Consumer<ModVersionBean> listener = mock(Consumer.class);
    instance.setOnOpenDetailListener(listener);
    instance.onShowModDetail();
    verify(listener).accept(any());
  }

  @Test
  public void testUiModLabel() {
    ModVersionBean modVersion = Instancio.of(ModVersionBean.class)
                                         .set(field(ModVersionBean::modType), ModType.UI)
                                         .create();
    runOnFxThreadAndWait(() -> instance.setEntity(modVersion));
    assertEquals(ModType.UI.name(), instance.typeLabel.getText());
  }

  @Test
  public void installedButtonVisibility() {
    runOnFxThreadAndWait(() -> instance.setEntity(modVersion));

    assertFalse(instance.uninstallButton.isVisible());
    assertTrue(instance.installButton.isVisible());

    runOnFxThreadAndWait(() -> installed.set(true));
    assertTrue(instance.uninstallButton.isVisible());
    assertFalse(instance.installButton.isVisible());
  }
}
