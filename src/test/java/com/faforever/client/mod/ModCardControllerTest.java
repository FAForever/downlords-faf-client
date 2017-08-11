package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.Mod.ModType;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModCardControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  public ModService modService;
  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;

  private ModCardController instance;

  @Before
  public void setUp() throws Exception {
    instance = new ModCardController(modService, timeService, i18n);

    ObservableList<Mod> installedMods = FXCollections.observableArrayList();
    when(modService.getInstalledMods()).thenReturn(installedMods);
    when(i18n.get(ModType.UI.getI18nKey())).thenReturn(ModType.UI.name());

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
  public void testSetMod() throws Exception {
    Mod mod = ModInfoBeanBuilder.create()
        .defaultValues()
        .name("Mod name")
        .modType(ModType.UI)
        .author("Mod author")
        .thumbnailUrl(getClass().getResource("/theme/images/close.png").toExternalForm())
        .get();

    when(modService.loadThumbnail(mod)).thenReturn(new Image("/theme/images/close.png"));
    instance.setMod(mod);

    assertThat(instance.nameLabel.getText(), is("Mod name"));
    assertThat(instance.authorLabel.getText(), is("Mod author"));
    assertThat(instance.thumbnailImageView.getImage(), is(notNullValue()));
    verify(modService).loadThumbnail(mod);
  }

  @Test
  public void testSetModNoThumbnail() throws Exception {
    Mod mod = ModInfoBeanBuilder.create()
        .defaultValues()
        .thumbnailUrl(null)
        .modType(ModType.UI)
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
    Consumer<Mod> listener = mock(Consumer.class);
    instance.setOnOpenDetailListener(listener);
    instance.onShowModDetail();
    verify(listener).accept(any());
  }

  @Test
  public void testUiModLabel() {
    Mod mod = ModInfoBeanBuilder.create().defaultValues().modType(ModType.UI).get();
    instance.setMod(mod);
    assertThat(instance.typeLabel.getText(), equalTo(ModType.UI.name()));
  }
}
