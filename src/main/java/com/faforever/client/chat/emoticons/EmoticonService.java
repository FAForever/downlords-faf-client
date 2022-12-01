package com.faforever.client.chat.emoticons;

import com.faforever.client.exception.ProgrammingError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmoticonService implements InitializingBean {

  @VisibleForTesting
  static final ClassPathResource EMOTICONS_JSON_FILE_RESOURCE = new ClassPathResource("images/emoticons/emoticons.json");

  private final ObjectMapper objectMapper;
  private final Decoder decoder = Base64.getDecoder();

  private List<EmoticonsGroup> emoticonsGroups;
  private final HashMap<String, String> shortcodeToBase64SvgContent = new HashMap<>();
  private Pattern emoticonShortcodeDetectorPattern;

  @Override
  public void afterPropertiesSet() throws IOException {
    loadAndVerifyEmoticons();
  }

  private void loadAndVerifyEmoticons() throws IOException, ProgrammingError {
    emoticonsGroups = Arrays.asList(objectMapper.readValue(EMOTICONS_JSON_FILE_RESOURCE.getInputStream(), EmoticonsGroup[].class));
    emoticonsGroups.stream().flatMap(emoticonsGroup -> emoticonsGroup.getEmoticons().stream())
        .forEach(emoticon -> emoticon.getShortcodes().forEach(shortcode -> {
          if (shortcodeToBase64SvgContent.containsKey(shortcode)) {
            throw new ProgrammingError("Shortcode `" + shortcode + "` is already taken");
          }
          String base64SvgContent = emoticon.getBase64SvgContent();
          shortcodeToBase64SvgContent.put(shortcode, base64SvgContent);
          emoticon.setImage(new Image(IOUtils.toInputStream(new String(decoder.decode(base64SvgContent)))));
        }));
    emoticonShortcodeDetectorPattern = Pattern.compile(shortcodeToBase64SvgContent.keySet().stream().map(Pattern::quote)
        .collect(Collectors.joining("|")));
  }

  public List<EmoticonsGroup> getEmoticonsGroups() {
    return emoticonsGroups;
  }

  public Pattern getEmoticonShortcodeDetectorPattern() {
    return emoticonShortcodeDetectorPattern;
  }

  public String getBase64SvgContentByShortcode(String shortcode) {
    return shortcodeToBase64SvgContent.get(shortcode);
  }
}
