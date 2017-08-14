package com.faforever.client.chat;

import javafx.scene.Node;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UrlPreviewResolver {

  CompletableFuture<Optional<Preview>> resolvePreview(String urlString);

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
}
