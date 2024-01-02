package com.faforever.client.builders;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonsGroup;

import java.util.List;

public class EmoticonGroupBuilder {

  private String name;
  private String attribution;
  private List<Emoticon> emoticons;

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
    this.name = name;
    return this;
  }

  public EmoticonGroupBuilder attribution(String attribution) {
    this.attribution = attribution;
    return this;
  }

  public EmoticonGroupBuilder emoticons(Emoticon... emoticons) {
    this.emoticons = List.of(emoticons);
    return this;
  }

  public EmoticonsGroup get() {
    return new EmoticonsGroup(name, attribution, emoticons);
  }
}
