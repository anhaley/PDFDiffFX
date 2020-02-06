package PDFDiffFX;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.io.File;

public class Controller implements Initializable {

    public StackPane layout;

    private void dragOverHandler(DragEvent e) {
        if (e.getGestureSource() != layout && e.getDragboard().hasFiles()) {
            e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        e.consume();
    }

    private void dragDroppedHandler(DragEvent e, String file) {
        Dragboard db = e.getDragboard();
        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        boolean success;
        if ( (success = db.hasFiles() ) ) {
            if (file.equals("file1")) {
                pathFile1 = db.getFiles().toString();
                pathFile1 = pathFile1.substring(1, pathFile1.length()-1);
            } else if (file.equals("file2")) {
                pathFile2 = db.getFiles().toString();
                pathFile2 = pathFile2.substring(1, pathFile2.length()-1);
            } else {
                success = false;
            }
        }
        e.setDropCompleted(success);
        e.consume();
    }

    private void generateReport() {

    }

    public Button buttonGenerate;
    public TextField textOutPath;
    public TitledPane paneFile1;
    public TitledPane paneFile2;

    private String pathFile1;
    private String pathFile2;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        paneFile1.setOnDragOver(this::dragOverHandler);
        paneFile2.setOnDragOver(this::dragOverHandler);
        paneFile1.setOnDragDropped(e -> dragDroppedHandler(e, "file1"));
        paneFile2.setOnDragDropped(e -> dragDroppedHandler(e, "file2"));

    }
}
