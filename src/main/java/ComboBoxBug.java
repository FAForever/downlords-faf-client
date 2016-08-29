import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

public class ComboBoxBug extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    ComboBox<Integer> comboBox = new ComboBox<>();
    comboBox.setItems(FXCollections.observableArrayList(1, 2, 3));
    comboBox.getSelectionModel().select(0);
    primaryStage.setScene(new Scene(comboBox));
    primaryStage.show();

    System.out.println(comboBox.getValue().getClass());
  }
}
