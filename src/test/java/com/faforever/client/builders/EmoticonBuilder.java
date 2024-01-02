package com.faforever.client.builders;

import com.faforever.client.chat.emoticons.Emoticon;
import javafx.scene.image.Image;

import java.util.List;

public class EmoticonBuilder {

  private List<String> shortcodes;
  private String base64SvgContent;
  private Image image;

  public static EmoticonBuilder create() {
    return new EmoticonBuilder();
  }

  public EmoticonBuilder defaultValues() {
    shortcodes(":value:");
    base64SvgContent("base64SvgContent");
    return this;
  }

  public EmoticonBuilder shortcodes(String... shortcodes) {
    this.shortcodes = List.of(shortcodes);
    return this;
  }

  public EmoticonBuilder base64SvgContent(String base64SvgContent) {
    this.base64SvgContent = base64SvgContent;
    return this;
  }

  public EmoticonBuilder image(Image image) {
    this.image = image;
    return this;
  }

  public Emoticon get() {
    return new Emoticon(shortcodes, base64SvgContent, image);
  }
}
