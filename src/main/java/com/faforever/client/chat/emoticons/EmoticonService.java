package com.faforever.client.chat.emoticons;

import com.faforever.client.exception.AssetLoadException;
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
import java.io.InputStream;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmoticonService implements InitializingBean {

  @VisibleForTesting
  static final ClassPathResource EMOTICONS_JSON_FILE_RESOURCE = new ClassPathResource("images/emoticons/emoticons.json");

  private final ObjectMapper objectMapper;

  private List<EmoticonsGroup> emoticonsGroups;

  private final Decoder decoder = Base64.getDecoder();
  private final Map<String, Image> shortcodeToImage = new HashMap<>();
  private final Map<String, Emoticon> shortcodeToEmoticon = new HashMap<>();

  @Override
  public void afterPropertiesSet() {
    CompletableFuture.runAsync(this::loadAndVerifyEmoticons);
  }

  @VisibleForTesting
  void loadAndVerifyEmoticons() {
    try (InputStream emoticonsInputStream = EMOTICONS_JSON_FILE_RESOURCE.getInputStream()) {
      emoticonsGroups = List.of(objectMapper.readValue(emoticonsInputStream, EmoticonsGroup[].class));
      emoticonsGroups.stream().flatMap(emoticonsGroup -> emoticonsGroup.emoticons().stream()).forEach(emoticon -> {
        String base64SvgContent = emoticon.base64SvgContent();
        Image image = new Image(IOUtils.toInputStream(new String(decoder.decode(base64SvgContent))));
        emoticon.shortcodes().forEach(shortcode -> {
          if (shortcodeToImage.put(shortcode, image) != null || shortcodeToEmoticon.put(shortcode, emoticon) != null) {
            throw new ProgrammingError("Shortcode `" + shortcode + "` is already taken");
          }
        });
      });
    } catch (IOException e) {
      throw new AssetLoadException("Unable to load emoticons", e, "");
    }
  }

  public List<EmoticonsGroup> getEmoticonsGroups() {
    return emoticonsGroups;
  }

  public boolean isEmoticonShortcode(String shortcode) {
    return shortcodeToEmoticon.containsKey(shortcode);
  }

  public Image getImageByShortcode(String shortcode) {
    return shortcodeToImage.get(shortcode);
  }

  public Emoticon getEmoticonByShortcode(String shortcode) {
    return shortcodeToEmoticon.get(shortcode);
  }

}
