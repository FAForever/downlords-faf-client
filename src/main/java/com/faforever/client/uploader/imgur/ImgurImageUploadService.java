package com.faforever.client.uploader.imgur;

import com.faforever.client.task.TaskService;
import com.faforever.client.uploader.ImageUploadService;
import javafx.scene.image.Image;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.concurrent.CompletionStage;

public class ImgurImageUploadService implements ImageUploadService {

  @Resource
  TaskService taskService;

  @Resource
  ApplicationContext applicationContext;

  @Override
  public CompletionStage<String> uploadImageInBackground(Image image) {
    ImgurUploadTask imgurUploadTask = applicationContext.getBean(ImgurUploadTask.class);
    imgurUploadTask.setImage(image);
    return taskService.submitTask(imgurUploadTask).getFuture();
  }
}
