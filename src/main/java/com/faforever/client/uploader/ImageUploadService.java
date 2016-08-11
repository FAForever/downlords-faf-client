package com.faforever.client.uploader;

import javafx.scene.image.Image;

import java.util.concurrent.CompletionStage;

public interface ImageUploadService {

  CompletionStage<String> uploadImageInBackground(Image image);
}
