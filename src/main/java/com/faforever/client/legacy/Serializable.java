package com.faforever.client.legacy;

import java.io.IOException;

interface Serializable extends java.io.Serializable {

  void serialize(QStreamWriter writer, String username, String session) throws IOException;
}
