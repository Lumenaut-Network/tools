package com.lumenaut.poolmanager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 10/01/2018 - 4:38 PM
 */
public class MainController {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FXML BINDINGS

    @FXML
    private AnchorPane primaryStage;

    @FXML
    private Button getFederationDataBtn;

    @FXML
    private Button compareDataBtn;

    @FXML
    private Button payBtn;

    @FXML
    private TextField poolAddressTextField;

    @FXML
    private TextField poolSecretTextField;

    @FXML
    private MenuItem closeButton;

    @FXML
    private TextArea inflationPoolDataTextArea;

    @FXML
    private ProgressBar progressBar;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Spinner task
    private AtomicBoolean applicationBusy = new AtomicBoolean(false);

    // Application stateful buttons
    private List<Button> statefulButtons = new ArrayList<>();

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region ACCESSORS

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    public MainController() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region INTERFACES IMPLEMENTATIONS

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHOD OVERRIDES

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Add all buttons that should react to the application "busy" state
        statefulButtons.add(getFederationDataBtn);
        statefulButtons.add(payBtn);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // CLOSE BUTTON HANDLER

        closeButton.setOnAction(event -> Platform.exit());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // GET FEDERATION DATA BUTTON HANDLER

        getFederationDataBtn.setOnAction(event -> {
            // Get the target pool key
            final String poolAddress = poolAddressTextField.getText();

            // Check if we have an address
            // TODO Also check if it's a valid hash or let the request fail?
            if (poolAddress == null || poolAddress.isEmpty()) {
                showError("You must specify the inflation pool's address below");
            } else {
                // Clear existing data
                inflationPoolDataTextArea.clear();

                // Start spinning
                setBusyState(true);

                // Build and submit async task
                final CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> {
                    try {
                        return FederationNetwork.getVoters(poolAddress);
                    } catch (Exception e) {
                        // Cancel applicationBusy state and show error
                        Platform.runLater(() -> {
                            setBusyState(false);
                            showError(e.getMessage());
                        });
                    }

                    return null;
                });

                // Process task completion
                request.thenAccept(inflationPoolData -> {
                    // Update text area
                    inflationPoolDataTextArea.setText(inflationPoolData);

                    // Cancel applicationBusy state
                    Platform.runLater(() -> setBusyState(false));
                });
            }
        });
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Shows an error dialog
     *
     * @param message
     */
    private void showError(final String message) {
        final Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
        alert.show();
    }

    /**
     * Shows an information dialog
     *
     * @param message
     */
    private void showInfo(final String message) {
        final Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
        alert.show();
    }

    /**
     * Set the application's busy state. While busy the application will set the progress bar to an infinite spin
     * and disable all stateful buttons. When exiting the busy state everything is restored.
     *
     * @param newState
     */
    private void setBusyState(boolean newState) {
        if (applicationBusy.get() != newState) {
            applicationBusy.set(newState);
            progressBar.setProgress((newState ? -1 : 0));

            // Enable or disable all statefulButtons added to the list based on the new applicationBusy state
            for (Button button : statefulButtons) {
                button.setDisable(newState);
            }
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

