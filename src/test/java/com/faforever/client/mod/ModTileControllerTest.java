package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModTileControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  public ModService modService;
  @Mock
  public NotificationService notificationService;
  @Mock
  public I18n i18n;
  @Mock
  public ReportingService reportingService;

  private ModTileController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("mod_tile.fxml");
    instance.modService = modService;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.reportingService = reportingService;

    ObservableList<ModInfoBean> installedMods = FXCollections.observableArrayList();
    when(modService.getInstalledMods()).thenReturn(installedMods);
  }

  @Test
  public void testSetMod() throws Exception {
    ModInfoBean mod = ModInfoBeanBuilder.create()
        .defaultValues()
        .name("Mod name")
        .author("Mod author")
        .thumbnailUrl(getClass().getResource("/theme/images/tray_icon.png").toExternalForm())
        .get();

    when(modService.loadThumbnail(mod)).thenReturn(new Image("/theme/images/tray_icon.png"));
    instance.setMod(mod);

    assertThat(instance.nameLabel.getText(), is("Mod name"));
    assertThat(instance.authorLabel.getText(), is("Mod author"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
    verify(modService).loadThumbnail(mod);
  }

  @Test
  public void testSetModNoThumbnail() throws Exception {
    ModInfoBean mod = ModInfoBeanBuilder.create()
        .defaultValues()
        .thumbnailUrl(null)
        .get();

    Image image = mock(Image.class);
    when(modService.loadThumbnail(mod)).thenReturn(image);

    instance.setMod(mod);

    assertThat(instance.thumbnailImageView.getImage(), notNullValue());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.modTileRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testShowModDetail() throws Exception {
    Consumer<ModInfoBean> listener = mock(Consumer.class);
    instance.setOnOpenDetailListener(listener);
    instance.onShowModDetail();
    verify(listener).accept(any());
  }
}
