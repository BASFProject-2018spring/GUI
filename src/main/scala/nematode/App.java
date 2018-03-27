package nematode;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nematode.controllers.MainController;

/**
 * The entry point of the application.
 */
public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        java.net.URL mainFXML = getClass().getClassLoader().getResource("main.fxml");

        FXMLLoader loader = new FXMLLoader(mainFXML);

        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setStage(primaryStage);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("Nematode Classifier");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * This main function is not necessary as JavaFX provides a main function. However, sbt struggles finding JFX's main
     * function, so we have to provide one here and tell sbt to use it as the entry point.
     */
    public static void main(String[] args) {
        //ResourceList.main(null);

        Application.launch(App.class, args);
    }
}
