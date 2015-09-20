package com.faforever.client.uploader.imgur;

import com.faforever.client.task.TaskService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.util.Callback;
import javafx.scene.image.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class ImgurImageUploadService implements ImageUploadService {

  @Autowired
  TaskService taskService;

  @Autowired
  ApplicationContext applicationContext;

  @Override
  public void uploadImageInBackground(Image image, Callback<String> callback) {
    ImgurUploadTask imgurUploadTask = applicationContext.getBean(ImgurUploadTask.class);
    imgurUploadTask.setImage(image);
    taskService.submitTask(new ImgurUploadTask(), callback);
  }
}
