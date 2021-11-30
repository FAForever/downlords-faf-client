package com.faforever.client.chat.emoticons;

import lombok.Data;

import java.util.List;

@Data
public class EmoticonsGroup {

  private String name;
  private String attribution;
  private List<Emoticon> emoticons;
}
