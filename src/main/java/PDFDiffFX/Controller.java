package PDFDiffFX;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.io.File;

public class Controller implements Initializable {

    public StackPane layout;
    public CheckBox checkBoxCopySummary;
    public CheckBox checkBoxReportVisual;
    public CheckBox checkBoxReportText;
    public Label labelReports;
    public Label labelInstructions;
    public Label labelOutPath;
    public Button buttonGenerate;
    public TextField textOutPath;
    public TitledPane paneFile1;
    public TitledPane paneFile2;

    private String pathFile1;
    private String pathFile2;

    @FXML
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

    @FXML
    private void generateReport() {
        if (pathFile1 == null) {
            AlertBox.display("No file provided", "Provide a file path for File 1.");
            return;
        } else if (pathFile2 == null) {
            AlertBox.display("No file provided", "Provide a file path for File 2.");
        }
        File file1 = new File(pathFile1);
        File file2 = new File(pathFile2);

        if (!file1.exists()) {
            AlertBox.display("File not found", pathFile1 + " could not be opened.");
            return;
        } else if (!file2.exists()) {
            AlertBox.display("File not found", pathFile2 + " could not be opened.");
            return;
        }
        // get text out path
        String pathOut = textOutPath.getText();
        // get -d flag
        String dumpArg = checkBoxCopySummary.isSelected() ? "-d" : null;
        PDFDiff.main(new String[] {pathFile1, pathFile2, pathOut, dumpArg});

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        paneFile1.setOnDragDropped(e -> dragDroppedHandler(e, "file1"));
        paneFile2.setOnDragDropped(e -> dragDroppedHandler(e, "file2"));

        // make a file explorer/chooser for output path

        // display modal with report summary

    }
}
