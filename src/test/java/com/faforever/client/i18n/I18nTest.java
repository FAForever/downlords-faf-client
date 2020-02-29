package com.faforever.client.i18n;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class I18nTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private I18n instance;

  @Mock
  private PreferencesService preferencesService;

  @Before
  public void setUp() throws Exception {
    Preferences preferences = new Preferences();
    preferences.getLocalization().setLanguage(Locale.GERMAN);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getLanguagesDirectory()).thenReturn(temporaryFolder.newFolder("languages").toPath());

    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames("classpath:i18n/messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

    instance = new I18n(messageSource, preferencesService);

    instance.afterPropertiesSet();
  }

  @Test
  public void get() {
    assertThat(instance.get("key"), is("Schlüssel"));
  }

  @Test
  public void getWithLocale() {
    assertThat(instance.get("key", Locale.GERMAN), is("Schlüssel"));
  }

  @Test
  public void getUserSpecificLocale() {
    assertThat(instance.getUserSpecificLocale(), is(Locale.GERMAN));
  }

  @Test
  public void getQuantizedZero() {
    assertThat(instance.getQuantized("tree", "trees", 0), is("Bäume"));
  }

  @Test
  public void getQuantizedOne() {
    assertThat(instance.getQuantized("tree", "trees", 1), is("Baum"));
  }

  @Test
  public void getQuantizedTwo() {
    assertThat(instance.getQuantized("tree", "trees", 2), is("Bäume"));
  }

  @Test
  public void getQuantizedNegativeOne() {
    assertThat(instance.getQuantized("tree", "trees", -1), is("Baum"));
  }

  @Test
  public void getQuantizedNegativeTwo() {
    assertThat(instance.getQuantized("tree", "trees", -2), is("Bäume"));
  }

  @Test
  public void number() {
    assertThat(instance.number(1), is("1"));
  }

  @Test
  public void numberWithSign() {
    assertThat(instance.numberWithSign(1), is("+1"));
  }

  @Test
  public void rounded() {
    assertThat(instance.rounded(1.234, 2), is("1,23"));
  }

  @Test
  public void getAvailableLanguages() {
    assertThat(instance.getAvailableLanguages(), hasSize(12));
  }
}
