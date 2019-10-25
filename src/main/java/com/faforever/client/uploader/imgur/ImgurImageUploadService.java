package com.faforever.client.uploader.imgur;

import com.faforever.client.task.TaskService;
import com.faforever.client.uploader.ImageUploadService;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


@Lazy
@Service
@RequiredArgsConstructor
public class ImgurImageUploadService implements ImageUploadService {

  private final TaskService taskService;
  private final ApplicationContext applicationContext;

  @Override
  public CompletableFuture<String> uploadImageInBackground(Image image) {
    ImgurUploadTask imgurUploadTask = applicationContext.getBean(ImgurUploadTask.class);
    imgurUploadTask.setImage(image);
    return taskService.submitTask(imgurUploadTask).getFuture();
  }
}
