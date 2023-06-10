package com.faforever.client.chat;

import javafx.scene.Node;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UrlPreviewResolver {

  CompletableFuture<Optional<Preview>> resolvePreview(String urlString);

  record Preview(Node node, String description) {}
}
