package com.faforever.client.chat.emoticon;

import com.faforever.client.builders.EmoticonBuilder;
import com.faforever.client.builders.EmoticonGroupBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsGroup;
import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class EmoticonServiceTest extends ServiceTest {

  @Mock
  ObjectMapper objectMapper;

  private EmoticonService instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new EmoticonService(objectMapper);
  }

  @Test
  public void testAllShortcodesAreUnique() throws Exception {
    EmoticonsGroup[] emoticonsGroupsArray = new EmoticonsGroup[]{
        EmoticonGroupBuilder.create().defaultValues().get(),
        EmoticonGroupBuilder.create().defaultValues().emoticons(
            EmoticonBuilder.create().shortcodes(":value3:").get(),
            EmoticonBuilder.create().shortcodes(":value4:").get())
            .get()};
    when(objectMapper.readValue(any(File.class), eq(EmoticonsGroup[].class))).thenReturn(emoticonsGroupsArray);
    assertDoesNotThrow(() -> instance.afterPropertiesSet());
    assertEquals("\\Q:value1:\\E|\\Q:value3:\\E|\\Q:value2:\\E|\\Q:value4:\\E", instance.getEmoticonShortcodeDetectorPattern().pattern());
  }

  @Test
  public void testThrowWhenShortcodesAreNotUnique() throws Exception {
    EmoticonsGroup[] emoticonsGroupsArray = new EmoticonsGroup[]{
        EmoticonGroupBuilder.create().defaultValues().get(),
        EmoticonGroupBuilder.create().defaultValues().emoticons(
            EmoticonBuilder.create().shortcodes(":value3:").get(),
            EmoticonBuilder.create().shortcodes(":value3:").get())
            .get()};
    when(objectMapper.readValue(any(File.class), eq(EmoticonsGroup[].class))).thenReturn(emoticonsGroupsArray);
    assertThrows(ProgrammingError.class, () -> instance.afterPropertiesSet());
  }
}
