package com.faforever.client.builders;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonsGroup;

import java.util.Arrays;

public class EmoticonGroupBuilder {

  private final EmoticonsGroup emoticonsGroup = new EmoticonsGroup();

  public static EmoticonGroupBuilder create() {
    return new EmoticonGroupBuilder();
  }

  public EmoticonGroupBuilder defaultValues() {
    name("group");
    attribution("https://test.com");
    emoticons(
        EmoticonBuilder.create().defaultValues().shortcodes(":value1:").get(),
        EmoticonBuilder.create().defaultValues().shortcodes(":value2:").get()
    );
    return this;
  }

  public EmoticonGroupBuilder name(String name) {
    emoticonsGroup.setName(name);
    return this;
  }

  public EmoticonGroupBuilder attribution(String attribution) {
    emoticonsGroup.setAttribution(attribution);
    return this;
  }

  public EmoticonGroupBuilder emoticons(Emoticon... emoticons) {
    emoticonsGroup.setEmoticons(Arrays.asList(emoticons));
    return this;
  }

  public EmoticonsGroup get() {
    return emoticonsGroup;
  }
}
