package com.faforever.client.uploader;

import javafx.scene.image.Image;

import java.util.concurrent.CompletableFuture;

public interface ImageUploadService {

  CompletableFuture<String> uploadImageInBackground(Image image);
}
