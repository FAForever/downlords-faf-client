package com.faforever.client.builders;

import com.faforever.client.chat.emoticons.Emoticon;

import java.util.Arrays;

public class EmoticonBuilder {

  private final Emoticon emoticon = new Emoticon();

  public static EmoticonBuilder create() {
    return new EmoticonBuilder();
  }

  public EmoticonBuilder defaultValues() {
    shortcodes(":value:");
    base64SvgContent("base64SvgContent");
    return this;
  }

  public EmoticonBuilder shortcodes(String... shortcodes) {
    emoticon.setShortcodes(Arrays.asList(shortcodes));
    return this;
  }

  public EmoticonBuilder base64SvgContent(String base64SvgContent) {
    emoticon.setBase64SvgContent(base64SvgContent);
    return this;
  }

  public Emoticon get() {
    return emoticon;
  }
}
