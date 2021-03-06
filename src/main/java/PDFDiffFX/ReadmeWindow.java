package PDFDiffFX;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ReadmeWindow  {

    static Stage stage;
    static String msg = "This tool generates comparison reports of the contents of two PDF files.  Drag and drop a PDF file into each of the two boxes, labeled \"File 1\" and \"File 2\".  File 1 represents the original document; File 2 represents the new or \"actual\" document. \n" +
            "\n" +
            "A textual comparison is generated by default.  Check the \"Visual\" box to also compare the documents graphically.\n" +
            "\n" +
            "Click the \"Browse ...\" button to select the folder where your results will be generated, and supply a title in the field labeled \"Name for this report\".  A folder with the title supplied here, containing the generated reports, will be placed in the folder you selected.\n" +
            "\n" +
            "Click the \"Generate Report\" button to trigger the comparison.  The results folder will be created and the reports placed inside.  When complete, a window will appear containing a summary of the differences found.  If you would like a copy of this summary to be added to the results folder, check the \"Copy this summary to file\" box.\n";

    public static void display()  {
        stage = new Stage();
        try {
            FXMLLoader loader = new FXMLLoader(ReadmeWindow.class.getResource("/readme.fxml"));
            Parent root = loader.load();
            stage.setTitle("Readme");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            AlertBox.display("Window error", "Could not open readme window");
            System.out.println(e.getMessage());
        }
    }

    // NPE from, I think, the static context. Go back to instantiation. On the right track with the label I think though

}
