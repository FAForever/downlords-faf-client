import com.faforever.client.fx.JavaFxUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.bridj.Pointer;
import org.bridj.PointerIO;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.bridj.cpp.com.shell.ITaskbarList3.TbpFlag;

import java.util.concurrent.CompletableFuture;

import static org.bridj.cpp.com.COMRuntime.newInstance;

public class Scratch extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    primaryStage.setScene(new Scene(new Pane()));
    primaryStage.show();

    ITaskbarList3 taskBar = CompletableFuture.supplyAsync(() -> {
      try {
        return newInstance(ITaskbarList3.class);
      } catch (ClassNotFoundException e) {
        return null;
      }
    }).join();
    long hwndVal = com.sun.jna.Pointer.nativeValue(JavaFxUtil.getNativeWindow());
    Pointer taskBarPointer = Pointer.pointerToAddress(hwndVal, (PointerIO) null);
    taskBar.SetProgressState(taskBarPointer, TbpFlag.TBPF_ERROR);
    taskBar.SetProgressValue(taskBarPointer, 50, 100);

  }
}
