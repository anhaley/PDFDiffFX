package PDFDiffFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class PDFDiffFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pdfDiffFX.fxml"));
            Parent root = loader.load();
            Controller controller = loader.getController();
            // set this button to open a dialog to choose output directory
            controller.btnOutDir.setOnAction( e -> {
                DirectoryChooser chooser = new DirectoryChooser();
                File defaultDir = new File("results");
                if (!defaultDir.exists())
                    defaultDir.mkdir();
                chooser.setInitialDirectory(new File("results"));
                try {
                    controller.textOutDir.setText(chooser.showDialog(primaryStage).getAbsolutePath());
                } catch (NullPointerException ignored) {} // null returned when "Cancel" clicked
            });

//            Parent root = FXMLLoader.load(getClass().getResource("/pdfDiffFX.fxml"));
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