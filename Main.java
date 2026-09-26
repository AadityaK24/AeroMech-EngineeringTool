import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // Main title
        Label title = new Label("AeroMech Engineering Tool");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label subtitle = new Label(
                "Aerospace & Mechatronics Engineering Simulator"
        );

        // Input fields
        Label massLabel = new Label("Mass (kg):");
        TextField massInput = new TextField();
        massInput.setPromptText("Enter mass");

        Label accelerationLabel = new Label("Acceleration (m/s²):");
        TextField accelerationInput = new TextField();
        accelerationInput.setPromptText("Enter acceleration");

        // Calculate button
        Button calculateButton = new Button("Calculate Force");

        // Result
        Label resultLabel = new Label("Result will appear here.");
        resultLabel.setStyle("-fx-font-size: 16px;");

        calculateButton.setOnAction(event -> {

            try {

                double mass = Double.parseDouble(massInput.getText());
                double acceleration =
                        Double.parseDouble(accelerationInput.getText());

                double force = mass * acceleration;

                resultLabel.setText(
                        String.format("Force = %.2f N", force)
                );

            } catch (NumberFormatException e) {

                resultLabel.setText(
                        "Please enter valid numerical values."
                );
            }
        });

        // Grid for inputs
        GridPane inputGrid = new GridPane();

        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setAlignment(Pos.CENTER);

        inputGrid.add(massLabel, 0, 0);
        inputGrid.add(massInput, 1, 0);

        inputGrid.add(accelerationLabel, 0, 1);
        inputGrid.add(accelerationInput, 1, 1);

        // Main layout
        VBox layout = new VBox(
                15,
                title,
                subtitle,
                inputGrid,
                calculateButton,
                resultLabel
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        // Window
        Scene scene = new Scene(layout, 700, 450);

        stage.setTitle("AeroMech Engineering Tool");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        launch(args);

    }
}