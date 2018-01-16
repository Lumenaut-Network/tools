package com.lumenaut.poolmanager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.UIUtils.showError;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 11/01/2018 - 4:58 PM
 */
public class SettingsController {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FXML BINDINGS

    ////////////////////////////////////////////////////////
    // BUTTONS
    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;

    ////////////////////////////////////////////////////////
    // TEXT FIELDS
    @FXML
    private ChoiceBox activeNetworkChoiceBox;

    @FXML
    private TextField defaultPoolAddressTextField;

    @FXML
    private TextField defaultFedNetworkInflationUrlTextField;

    @FXML
    public TextField defaultMemoTextField;

    @FXML
    public TextField defaultFeeTextField;

    @FXML
    private TextField horizonDbAddress;

    @FXML
    private TextField horizonDbPort;

    @FXML
    private TextField horizonDbUser;

    @FXML
    private TextField horizonDbPass;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Component references from other scenes
    public Rectangle selectedNetworkRect;
    public Label selectedNetworkLabel;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    public SettingsController() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Initialize the network choice box values
        activeNetworkChoiceBox.setItems(FXCollections.observableArrayList("TEST", "LIVE"));

        // Limit fee textfield to numeric input only
        defaultFeeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                defaultFeeTextField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // Limit the memo to 28 characters
        defaultMemoTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            if (defaultMemoTextField.getText().length() > 28) {
                String s = defaultMemoTextField.getText().substring(0, 28);
                defaultMemoTextField.setText(s);
            }
        });

        // Load current settings in the UI components
        activeNetworkChoiceBox.setValue(SETTING_OPERATIONS_NETWORK);
        defaultPoolAddressTextField.setText(SETTING_INFLATION_POOL_ADDRESS);
        defaultFedNetworkInflationUrlTextField.setText(SETTING_FEDERATION_NETWORK_INFLATION_URL);
        defaultMemoTextField.setText(SETTING_MEMO);
        defaultFeeTextField.setText(String.valueOf(SETTING_FEE));
        horizonDbAddress.setText(SETTING_HORIZON_DB_ADDRESS);
        horizonDbPort.setText(SETTING_HORIZON_DB_PORT);
        horizonDbUser.setText(SETTING_HORIZON_DB_USER);
        horizonDbPass.setText(SETTING_HORIZON_DB_PASS);

        // Handle save & closing
        cancelBtn.setOnAction(event -> cancelBtn.getScene().getWindow().hide());
        saveBtn.setOnAction(event -> saveSettings());

        // Handle pay button color based on the network choice
        activeNetworkChoiceBox.setOnAction(event -> {
            final String selectedValue = activeNetworkChoiceBox.getValue().toString();

            if (selectedNetworkRect != null && selectedNetworkLabel != null) {
                switch (selectedValue) {
                    case "TEST":
                        selectedNetworkRect.setFill(Paint.valueOf("#64EE64"));
                        selectedNetworkLabel.setText("TEST NETWORK");
                        SETTING_OPERATIONS_NETWORK = "TEST";

                        break;
                    case "LIVE":
                        selectedNetworkRect.setFill(Paint.valueOf("#EE4B52"));
                        selectedNetworkLabel.setText("LIVE NETWORK");
                        SETTING_OPERATIONS_NETWORK = "LIVE";

                        break;
                }
            }
        });
    }

    /**
     * Save current settings
     */
    private void saveSettings() {
        // Update settings from the textfields
        SETTING_OPERATIONS_NETWORK = activeNetworkChoiceBox.getValue().toString();
        SETTING_INFLATION_POOL_ADDRESS = defaultPoolAddressTextField.getText();
        SETTING_FEDERATION_NETWORK_INFLATION_URL = defaultFedNetworkInflationUrlTextField.getText();
        SETTING_MEMO = defaultMemoTextField.getText();

        final long currentFee = Long.parseLong(defaultFeeTextField.getText());
        if (currentFee < 100) {
            SETTING_FEE = 100;
            defaultFeeTextField.setText("100");
        } else {
            SETTING_FEE = currentFee;
        }


        SETTING_HORIZON_DB_ADDRESS = horizonDbAddress.getText();
        SETTING_HORIZON_DB_PORT = horizonDbPort.getText();
        SETTING_HORIZON_DB_USER = horizonDbUser.getText();
        SETTING_HORIZON_DB_PASS = horizonDbPass.getText();

        // Trigger settings save
        try {
            Settings.saveSettings();
        } catch (IOException e) {
            showError(e.getMessage());
        }

        // Close the settings window
        saveBtn.getScene().getWindow().hide();
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

