package com.faforever.client.uploader.imgur;

import com.faforever.client.task.TaskService;
import com.faforever.client.uploader.ImageUploadService;
import javafx.scene.image.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;

public class ImgurImageUploadService implements ImageUploadService {

  @Autowired
  TaskService taskService;

  @Autowired
  ApplicationContext applicationContext;

  @Override
  public CompletableFuture<String> uploadImageInBackground(Image image) {
    ImgurUploadTask imgurUploadTask = applicationContext.getBean(ImgurUploadTask.class);
    imgurUploadTask.setImage(image);
    return taskService.submitTask(imgurUploadTask);
  }
}
