package com.faforever.client.remote.gson;

import com.faforever.client.game.Faction;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class FactionTypeAdapter extends TypeAdapter<Faction> {

  public static final FactionTypeAdapter INSTANCE = new FactionTypeAdapter();

  private FactionTypeAdapter() {
    // private
  }

  @Override
  public void write(JsonWriter out, Faction value) throws IOException {
    out.value(value.getString());
  }

  @Override
  public Faction read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    String s = in.nextString();
    Faction faction = Faction.fromString(s);
    if (faction != null) {
      return faction;
    } else {
      // this is necessary as the faction is expressed everywhere as a string but the game launch message
      // uses id's for legacy purposes which have been ignored by the java client so far
      try {
        return Faction.fromFaValue(Integer.parseInt(s));
      } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
        return null;
      }
    }
  }
}
