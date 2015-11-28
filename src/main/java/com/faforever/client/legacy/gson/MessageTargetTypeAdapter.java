package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.MessageTarget;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MessageTargetTypeAdapter extends TypeAdapter<MessageTarget> {

  public static final MessageTargetTypeAdapter INSTANCE = new MessageTargetTypeAdapter();

  private MessageTargetTypeAdapter() {
  }

  @Override
  public void write(JsonWriter out, MessageTarget value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public MessageTarget read(JsonReader in) throws IOException {
    return MessageTarget.fromString(in.nextString());
  }
}
