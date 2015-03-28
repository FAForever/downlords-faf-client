package com.faforever.client.legacy.message;

import com.faforever.client.legacy.QStreamWriter;

import java.io.IOException;

public interface Serializable extends java.io.Serializable {

  void serialize(QStreamWriter writer, String username, String session) throws IOException;
}
