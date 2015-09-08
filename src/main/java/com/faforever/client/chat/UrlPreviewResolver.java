package com.faforever.client.chat;

import javafx.scene.Node;

public interface UrlPreviewResolver {

  class Preview {

    private final Node node;
    private final String description;

    public Preview(Node node, String description) {
      this.node = node;
      this.description = description;
    }

    public Node getNode() {
      return node;
    }

    public String getDescription() {
      return description;
    }
  }

  Preview resolvePreview(String urlString);
}
