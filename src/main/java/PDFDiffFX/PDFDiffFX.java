package PDFDiffFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PDFDiffFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/pdfDiffFX.fxml"));
            primaryStage.setTitle("PDFDiff");
            primaryStage.setScene(new Scene(root, 650, 400));
            primaryStage.show();
        } catch (IOException e) {
            // open dialog describing error
            e.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}