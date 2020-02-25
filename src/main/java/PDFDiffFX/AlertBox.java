package PDFDiffFX;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AlertBox {
    public static void display(String title, String msg) {
        Stage window = new Stage();

        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setWidth(500);

        Text text = new Text(msg);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10,10,10,10));
        layout.setMaxHeight(2000);
        layout.getChildren().addAll(text);
        layout.setAlignment(Pos.CENTER);
        layout.setMinHeight(25 * msg.split("\n").length);

        Scene scene = new Scene(layout);
        window.setScene(scene);
        text.wrappingWidthProperty().bind(scene.widthProperty().subtract(20));
        window.showAndWait(); // capture focus to prevent clicking outside
    }
}
