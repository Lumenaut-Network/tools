package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.TransactionPlan;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 15/01/2018 - 5:19 PM
 */
public class ProcessingController {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FXML BINDINGS

    ////////////////////////////////////////////////////////
    // UI

    @FXML
    private TextArea processingOutputTextArea;

    @FXML
    public Button startBtn;

    @FXML
    private Button closeBtn;

    @FXML
    private ProgressBar processingProgressBar;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Transaction plan
    public TransactionPlan transactionPlan;
    public String signingKey;
    public AnchorPane primaryStage;

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
    public ProcessingController() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region INTERFACES IMPLEMENTATIONS

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHOD OVERRIDES

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * If this controller is instantiated it means that we have a transaction plan validated and ready to be processed
     */
    @FXML
    private void initialize() {

        // !!!TEMPORARY!!! will only be enabled when the operation completes
        closeBtn.setDisable(false);

        ////////////////////////////////////////////////////////////////
        // BUTTON HANDLERS

        startBtn.setOnAction(event -> startProcessingTransactions());
        closeBtn.setOnAction(event -> {
            closeBtn.getScene().getWindow().hide();
        });

    }

    /**
     * Start processing the transaction plan
     */
    private void startProcessingTransactions() {
        if (transactionPlan == null || signingKey == null || signingKey.isEmpty()) {
            processingOutputTextArea.setText("Cannot execute, transaction plan data or signing key are missing, please go back and rebuild the transaction plan");

            // Disable start button and enable the close button
            startBtn.setDisable(true);
            closeBtn.setDisable(false);
        } else {
            // Disable start button
            startBtn.setDisable(true);
        }
    }

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

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

