package PDFDiffFX;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    public StackPane layout;
    public CheckBox checkBoxCopySummary;
    public CheckBox checkBoxReportVisual;
    public CheckBox checkBoxReportText;
    public Label labelReports;
    public Label labelInstructions;
    public Label labelOutPath;
    public Button buttonGenerate;
    public TextField textOutDir;
    public TextField textOutName;
    public TitledPane paneFile1;
    public TitledPane paneFile2;
    public Button btnOutDir;
    public ImageView imgFile1;
    public ImageView imgFile2;

    private String pathFile1;
    private String pathFile2;
    Image pdfIcon;

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
        if ((success = db.hasFiles())) {
            if (file.equals("file1")) {
                pathFile1 = db.getFiles().toString();
                pathFile1 = pathFile1.substring(1, pathFile1.length() - 1);
                if (pathFile1.endsWith(".pdf")) {
                    imgFile1.setImage(pdfIcon);
                    String[] pathParts = pathFile1.split("[/\\\\]");
                    paneFile1.setText(pathParts[pathParts.length - 1]);
                } else {
                    imgFile1.setImage(null);
                    paneFile1.setText("Invalid file");
                }
            } else if (file.equals("file2")) {
                pathFile2 = db.getFiles().toString();
                pathFile2 = pathFile2.substring(1, pathFile2.length() - 1);
                if (pathFile2.endsWith(".pdf")) {
                    imgFile2.setImage(pdfIcon);
                    String[] pathParts = pathFile2.split("[/\\\\]");
                    paneFile2.setText(pathParts[pathParts.length - 1]);
                } else {
                    imgFile2.setImage(null);
                    paneFile2.setText("Invalid file");
                }
            } else {
                success = false;
            }
        }
        e.setDropCompleted(success);
        e.consume();
    }

    private boolean validateInputFiles() {
        if (pathFile1 == null) {
            AlertBox.display("No file provided", "Provide a file for File 1.");
            return false;
        } else if (pathFile2 == null) {
            AlertBox.display("No file provided", "Provide a file for File 2.");
            return false;
        }
        File file1 = new File(pathFile1);
        File file2 = new File(pathFile2);
        if (!file1.exists()) {
            AlertBox.display("File not found", pathFile1 + " could not be opened.");
            return false;
        } else if (!file2.exists()) {
            AlertBox.display("File not found", pathFile2 + " could not be opened.");
            return false;
        }
        if (!pathFile1.endsWith(".pdf")) {
            AlertBox.display("Invalid file type", "Select a PDF file for File 1.");
            return false;
        } else if (!pathFile2.endsWith(".pdf")) {
            AlertBox.display("Invalid file type", "Select a PDF file for File 2.");
            return false;
        }
        return true;
    }

    @FXML
    private void generateReport() {

        // get input files
        if (!validateInputFiles())
            return;

        // get output path
        String pathOut = textOutDir.getText();
        if (pathOut == null) {
            AlertBox.display("No directory provided", "Select a directory where the files will be saved.");
            return;
        }
        String name = textOutName.getText();
        if (name == null) {
            AlertBox.display("No file name provided", "Enter a name for the generated files.");
            return;
        }

        // get -d flag
        String dumpArg = checkBoxCopySummary.isSelected() ? "-d" : null;
        String graphicalArg = checkBoxReportVisual.isSelected() ? "-g" : null;

        PDFDiff.main(new String[]{pathFile1, pathFile2, pathOut + "/" + name, dumpArg, graphicalArg});

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        paneFile1.setOnDragDropped(e -> dragDroppedHandler(e, "file1"));
        paneFile2.setOnDragDropped(e -> dragDroppedHandler(e, "file2"));
        try {
            pdfIcon = new Image(getClass().getResource("/pdfIcon.png").toURI().toString(), 100, 100, false, false);
        } catch (URISyntaxException _e) {
            System.out.println("Error reading image URI");
        }
    }
}
