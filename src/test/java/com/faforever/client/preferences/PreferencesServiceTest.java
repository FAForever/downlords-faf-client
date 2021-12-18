package com.faforever.client.preferences;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.serialization.ColorMixin;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.client.serialization.PathDeserializer;
import com.faforever.client.serialization.PathSerializer;
import com.faforever.client.serialization.SimpleListPropertyInstantiator;
import com.faforever.client.serialization.SimpleMapPropertyInstantiator;
import com.faforever.client.serialization.SimpleSetPropertyInstantiator;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.eventbus.EventBus;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.paint.Color;
import org.bridj.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreferencesServiceTest extends ServiceTest {

  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");


  private PreferencesService instance;

  @Mock
  private EventBus eventBus;
  @Mock
  private ClientProperties clientProperties;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() throws Exception {
    objectMapper = new ObjectMapper()
        .setSerializationInclusion(Include.NON_EMPTY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addMixIn(Color.class, ColorMixin.class)
        .addMixIn(Faction.class, FactionMixin.class);

    TypeFactory typeFactory = objectMapper.getTypeFactory();

    Module preferencesModule = new SimpleModule()
        .addSerializer(Path.class, new PathSerializer())
        .addDeserializer(Path.class, new PathDeserializer())
        .addValueInstantiator(SimpleMapProperty.class, new SimpleMapPropertyInstantiator(objectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleMapProperty.class)))
        .addValueInstantiator(SimpleListProperty.class, new SimpleListPropertyInstantiator(objectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleListProperty.class)))
        .addValueInstantiator(SimpleSetProperty.class, new SimpleSetPropertyInstantiator(objectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleSetProperty.class)))
        .addAbstractTypeMapping(ObservableMap.class, SimpleMapProperty.class)
        .addAbstractTypeMapping(ObservableList.class, SimpleListProperty.class)
        .addAbstractTypeMapping(ObservableSet.class, SimpleSetProperty.class)
        .addAbstractTypeMapping(MapProperty.class, SimpleMapProperty.class)
        .addAbstractTypeMapping(ListProperty.class, SimpleListProperty.class)
        .addAbstractTypeMapping(SetProperty.class, SimpleSetProperty.class);

    objectMapper.registerModule(preferencesModule);

    instance = new PreferencesService(clientProperties);
  }

  @Test
  public void testGetPreferencesDirectory() throws Exception {
    assertThat(instance.getPreferencesDirectory(), notNullValue());
  }

  @Test
  public void testGetFafBinDirectory() throws Exception {
    assertThat(instance.getFafBinDirectory(), is(instance.getFafDataDirectory().resolve("bin")));
  }

  @Test
  public void testGetFafDataDirectory() throws Exception {
    if (Platform.isWindows()) {
      assertThat(instance.getFafDataDirectory(), is(Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever")));
    } else {
      assertThat(instance.getFafDataDirectory(), is(Path.of(System.getProperty("user.home")).resolve(".faforever")));
    }
  }

  @Test
  public void testGetFafReposDirectory() throws Exception {
    assertThat(instance.getPatchReposDirectory(), is(instance.getFafDataDirectory().resolve("repos")));
  }

  @Test
  public void testGetCorruptedReplaysDirectory() throws Exception {
    Path result = instance.getCorruptedReplaysDirectory();
    Path expected = instance.getReplaysDirectory().resolve("corrupt");
    assertThat(result, is(expected));
  }

  @Test
  public void testGetReplaysDirectory() throws Exception {
    assertThat(instance.getReplaysDirectory(), is(instance.getFafDataDirectory().resolve("replays")));
  }

  @Test
  public void testGetCacheDirectory() throws Exception {
    assertThat(instance.getCacheDirectory(), is(instance.getFafDataDirectory().resolve("cache")));
  }

  @Test
  public void testGetFafLogDirectory() throws Exception {
    assertThat(instance.getFafLogDirectory(), is(instance.getFafDataDirectory().resolve("logs")));
  }

  @Test
  public void testGetNewLogFile() throws Exception {
    assertTrue(GAME_LOG_PATTERN.matcher(instance.getNewGameLogFile(0).getFileName().toString()).matches());
  }

  @Test
  public void testPreferencesSerializable() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    assertDoesNotThrow(() -> objectMapper.readValue(objectMapper.writeValueAsString(preferences), Preferences.class));
  }
}
