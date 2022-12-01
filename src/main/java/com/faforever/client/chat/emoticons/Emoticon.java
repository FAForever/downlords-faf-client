package com.faforever.client.chat.emoticons;

import javafx.scene.image.Image;
import lombok.Data;

import java.util.List;

@Data
public class Emoticon {

  private List<String> shortcodes;
  private String base64SvgContent;
  private Image image;
}
