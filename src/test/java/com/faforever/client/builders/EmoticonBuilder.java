package com.faforever.client.builders;

import com.faforever.client.chat.emoticons.Emoticon;

import java.util.Arrays;

public class EmoticonBuilder {

  private final String content = "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjEgNSA0MiA0NiI+CiAgICA8cGF0aCBmaWxsPSIjMjE5NmYzIgogICAgICAgICAgZD0iTSA4Ljk2IDM0LjUgbCAtNS41OCAtNS41OCBsIDUuNTggLTUuNTggbCA1LjU4IDUuNTggbCAtNS41OCA1LjU4IHogTSAxNS42NiA0MS4xOSBsIC01LjU4IC01LjU4IGwgNS41OCAtNS41OCBsIDUuNTggNS41OCBsIC01LjU4IDUuNTggeiBNIDIyLjM1IDQ3Ljg4IGwgLTUuNTggLTUuNTggbCA1LjU4IC01LjU4IGwgNS41OCA1LjU4IGwgLTUuNTggNS41OCB6IE0gMjkuMDQgNDEuMTkgbCAtNS41OCAtNS41OCBsIDUuNTggLTUuNTggbCA1LjU4IDUuNTggbCAtNS41OCA1LjU4IHogTSAzNS43MyAzNC41IGwgLTUuNTggLTUuNTggbCA1LjU4IC01LjU4IGwgNS41OCA1LjU4IGwgLTUuNTggNS41OCB6IE0gMjIuMzUgMzQuNSBsIC0xMi4yMyAtMTIuMjMgbCAxMi4yMyAtMTIuMzEgbCAxMi4yNyAxMi4yNyBsIC0xMi4yNyAxMi4yNyB6Ii8+Cjwvc3ZnPg==";

  private final Emoticon emoticon = new Emoticon();

  public static EmoticonBuilder create() {
    return new EmoticonBuilder();
  }

  public EmoticonBuilder defaultValues() {
    shortcodes(":value:");
    base64SvgContent(content);
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
