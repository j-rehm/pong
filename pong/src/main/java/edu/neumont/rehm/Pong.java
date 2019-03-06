package edu.neumont.rehm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import edu.neumont.rehm.view.PongView;

import java.net.URL;

/**
 * The Main class for Pong
 */
public class Pong extends Application {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        Application.launch(Pong.class, args);
    }

    public void start(Stage stage) throws Exception {
        URL location = this.getClass().getClassLoader().getResource("PongView.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        Parent root = loader.load();
        PongView view = loader.getController();
        view.init(stage);
        stage.setScene(new Scene(root));
    }
}
