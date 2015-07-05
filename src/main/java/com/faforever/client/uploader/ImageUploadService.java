package com.faforever.client.uploader;

import com.faforever.client.util.Callback;
import javafx.scene.image.Image;

public interface ImageUploadService {

  void uploadImage(Image image, Callback<String> callback);
}
