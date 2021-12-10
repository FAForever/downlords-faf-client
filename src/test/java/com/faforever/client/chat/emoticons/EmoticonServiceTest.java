package com.faforever.client.chat.emoticons;

import com.faforever.client.builders.EmoticonBuilder;
import com.faforever.client.builders.EmoticonGroupBuilder;
import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.test.ServiceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class EmoticonServiceTest extends ServiceTest {

  @Mock
  private ObjectMapper objectMapper;

  private EmoticonService instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new EmoticonService(objectMapper);
  }

  @Test
  public void testAllProductionShortcodesAreUnique() throws Exception {
    when(objectMapper.readValue(any(InputStream.class), eq(EmoticonsGroup[].class)))
        .thenReturn(new ObjectMapper().readValue(EmoticonService.EMOTICONS_JSON_FILE_RESOURCE.getInputStream(), EmoticonsGroup[].class));

    assertDoesNotThrow(() -> instance.afterPropertiesSet());
    String shortcodePattern = instance.getEmoticonShortcodeDetectorPattern().pattern();
    assertFalse(shortcodePattern.isBlank());
    assertFalse(instance.getEmoticonsGroups().isEmpty());
    instance.getEmoticonsGroups().stream().flatMap(emoticonsGroup -> emoticonsGroup.getEmoticons().stream())
        .flatMap(emoticon -> emoticon.getShortcodes().stream())
        .forEach(shortcode -> {
          assertTrue(shortcodePattern.contains(shortcode));
          assertFalse(instance.getBase64SvgContentByShortcode(shortcode).isBlank());
        });
  }

  @Test
  public void testAllProductionGroupsAreUnique() throws Exception {
    EmoticonsGroup[] groups = new ObjectMapper().readValue(EmoticonService.EMOTICONS_JSON_FILE_RESOURCE.getInputStream(), EmoticonsGroup[].class);

    Set<String> groupNames = Arrays.stream(groups).map(EmoticonsGroup::getName).collect(Collectors.toSet());
    assertEquals(groupNames.size(), groups.length);
  }

  @Test
  public void testThrowWhenShortcodesAreNotUniqueInEmoticon() throws Exception {
    EmoticonsGroup[] emoticonsGroupsArray = new EmoticonsGroup[]{
        EmoticonGroupBuilder.create().defaultValues().get(),
        EmoticonGroupBuilder.create().defaultValues().emoticons(
            EmoticonBuilder.create().shortcodes(":value3:", ":value3:").get()
        ).get()};
    when(objectMapper.readValue(any(InputStream.class), eq(EmoticonsGroup[].class))).thenReturn(emoticonsGroupsArray);

    assertThrows(ProgrammingError.class, () -> instance.afterPropertiesSet());
  }

  @Test
  public void testThrowWhenShortcodesAreNotUniqueInGroup() throws Exception {
    EmoticonsGroup[] emoticonsGroupsArray = new EmoticonsGroup[]{
        EmoticonGroupBuilder.create().defaultValues().get(),
        EmoticonGroupBuilder.create().defaultValues().emoticons(
            EmoticonBuilder.create().shortcodes(":value3:").get(),
            EmoticonBuilder.create().shortcodes(":value3:").get())
            .get()};
    when(objectMapper.readValue(any(InputStream.class), eq(EmoticonsGroup[].class))).thenReturn(emoticonsGroupsArray);

    assertThrows(ProgrammingError.class, () -> instance.afterPropertiesSet());
  }

  @Test
  public void testThrowWhenShortcodesAreNotUniqueAcrossGroups() throws Exception {
    EmoticonsGroup[] emoticonsGroupsArray = new EmoticonsGroup[]{
        EmoticonGroupBuilder.create().defaultValues().emoticons(EmoticonBuilder.create().shortcodes(":value3:").get()).get(),
        EmoticonGroupBuilder.create().defaultValues().emoticons(EmoticonBuilder.create().shortcodes(":value3:").get()).get()
    };
    when(objectMapper.readValue(any(InputStream.class), eq(EmoticonsGroup[].class))).thenReturn(emoticonsGroupsArray);

    assertThrows(ProgrammingError.class, () -> instance.afterPropertiesSet());
  }

  @Test
  public void testGetSvgContentByShortcode() throws Exception {
    EmoticonsGroup emoticonsGroup = EmoticonGroupBuilder.create().defaultValues().get();
    Emoticon emoticon = emoticonsGroup.getEmoticons().get(0);
    EmoticonsGroup[] emoticonsGroupsArray = new EmoticonsGroup[]{emoticonsGroup};
    when(objectMapper.readValue(any(InputStream.class), eq(EmoticonsGroup[].class))).thenReturn(emoticonsGroupsArray);

    instance.afterPropertiesSet();
    assertEquals(emoticon.getBase64SvgContent(), instance.getBase64SvgContentByShortcode(emoticon.getShortcodes().get(0)));
    emoticon.getShortcodes().forEach(shortcode -> {
      assertTrue(instance.getEmoticonShortcodeDetectorPattern().pattern().contains(shortcode));
    });
    assertTrue(instance.getEmoticonShortcodeDetectorPattern().pattern().contains("|"));
  }
}
