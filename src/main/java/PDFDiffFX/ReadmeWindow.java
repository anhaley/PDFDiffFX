package PDFDiffFX;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class ReadmeWindow  {

    @FXML
    private Button readmeBtnExit1, readmeBtnExit2;
    static Stage stage;

    public static void display()  {
        stage = new Stage();
        try {
            FXMLLoader loader = new FXMLLoader(ReadmeWindow.class.getResource("/readme.fxml"));
            Parent root = loader.load();
            stage.setTitle("Readme");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            AlertBox.display("Window error", "Could not open readme window");
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void closeReadme() {
        stage.close();
    }

}
