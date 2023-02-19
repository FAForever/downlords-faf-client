package com.faforever.client.i18n;

import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class I18nTest extends ServiceTest {

  @InjectMocks
  private I18n instance;


  @Spy
  private LocalizationPrefs localizationPrefs;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ReloadableResourceBundleMessageSource messageSource;

  @BeforeEach
  public void setUp() throws Exception {
    localizationPrefs.setLanguage(Locale.GERMAN);

    dataPrefs.setBaseDataDirectory(Path.of("."));

    messageSource.setBasenames("classpath:i18n/messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

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
  public void testLoadedLanguagesAreComplete() throws IOException {
    final Path path = Path.of("src", "main", "resources", "i18n");
    try (Stream<Path> walk = Files.walk(path)) {
      final long messageFileCount = walk.filter(propertiesFile -> Files.isRegularFile(propertiesFile) && propertiesFile.getFileName().toString().endsWith(".properties")).count();
      assertThat(instance.getAvailableLanguages(), hasSize((int) messageFileCount));
    }
  }
}
