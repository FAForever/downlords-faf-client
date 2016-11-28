package com.faforever.client.uploader.imgur;

import com.faforever.client.task.TaskService;
import com.faforever.client.uploader.ImageUploadService;
import javafx.scene.image.Image;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;


@Lazy
@Service
public class ImgurImageUploadService implements ImageUploadService {

  @Inject
  TaskService taskService;
  @Inject
  ApplicationContext applicationContext;

  @Override
  public CompletionStage<String> uploadImageInBackground(Image image) {
    ImgurUploadTask imgurUploadTask = applicationContext.getBean(ImgurUploadTask.class);
    imgurUploadTask.setImage(image);
    return taskService.submitTask(imgurUploadTask).getFuture();
  }
}
