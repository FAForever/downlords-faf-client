package com.faforever.client.chat;

import javafx.scene.Node;

public interface UrlPreviewResolver {

  class Preview {

    public final Node node;
    public final String description;

    public Preview(Node node, String description) {
      this.node = node;
      this.description = description;
    }
  }

  Preview resolvePreview(String urlString);
}
