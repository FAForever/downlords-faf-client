package com.faforever.client.filter;

import com.faforever.client.domain.GameBean;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.FiltersPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static com.faforever.client.builders.GameBeanBuilder.create;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class CustomGamesFilterControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private FeaturedModService featuredModService;
  @Mock
  private PlayerService playerService;

  @Mock
  private MutableListFilterController<GameBean> mapFolderNameBlackListFilter;
  @Mock
  private FilterCheckboxController<GameBean> privateGameFilter;
  @Mock
  private FilterCheckboxController<GameBean> simModsFilter;
  @Spy
  private FiltersPrefs filtersPrefs;
  @Spy
  private Preferences preferences;

  @InjectMocks
  private CustomGamesFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    // Order is important
    when(uiService.loadFxml(anyString())).thenReturn(
        privateGameFilter,
        simModsFilter,
        mock(FilterMultiCheckboxController.class),  // Featured mods
        mapFolderNameBlackListFilter
    );
    when(featuredModService.getFeaturedMods()).thenReturn(Flux.empty());
    when(mapFolderNameBlackListFilter.valueProperty()).thenReturn(new SimpleListProperty<>());
    when(privateGameFilter.valueProperty()).thenReturn(new SimpleBooleanProperty());
    when(simModsFilter.valueProperty()).thenReturn(new SimpleBooleanProperty());

    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testPrivateGameFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(privateGameFilter).registerListener(argumentCaptor.capture());

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, create().defaultValues().passwordProtected(false).get()));
    assertTrue(filter.apply(false, create().defaultValues().passwordProtected(true).get()));
    assertTrue(filter.apply(true, create().defaultValues().passwordProtected(false).get()));
    assertFalse(filter.apply(true, create().defaultValues().passwordProtected(true).get()));
  }

  @Test
  public void testMapFolderNameBlackListFilter() {
    ArgumentCaptor<BiFunction<List<String>, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(mapFolderNameBlackListFilter).registerListener(argumentCaptor.capture());

    GameBean game = create().defaultValues().mapFolderName("test_map.v011").get();
    BiFunction<List<String>, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Collections.emptyList(), game));
    assertFalse(filter.apply(List.of("test_"), game));
    assertFalse(filter.apply(List.of(".v011"), game));
    assertFalse(filter.apply(List.of("lenta", "test_map.v011"), game));
    assertTrue(filter.apply(List.of("lenta"), game));
    assertFalse(filter.apply(List.of("TEST"), game));
  }
}