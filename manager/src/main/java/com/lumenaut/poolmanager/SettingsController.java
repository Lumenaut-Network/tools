package com.lumenaut.poolmanager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private ChoiceBox<String> activeNetworkChoiceBox;

    @FXML
    private TextField defaultPoolAddressTextField;

    @FXML
    public TextField defaultMemoTextField;

    @FXML
    public TextField defaultFeeTextField;

    @FXML
    public TextField defaultDonationsStringTextField;

    @FXML
    public TextField horizonTest;

    @FXML
    public TextField horizonLive;

    @FXML
    public TextField horizonDbLiveHost;

    @FXML
    public TextField horizonDbLivePort;

    @FXML
    public TextField horizonDbLiveUser;

    @FXML
    public PasswordField horizonDbLivePass;

    @FXML
    public TextField horizonDbTestHost;

    @FXML
    public TextField horizonDbTestPort;

    @FXML
    public TextField horizonDbTestUser;

    @FXML
    public PasswordField horizonDbTestPass;

    @FXML
    private CheckBox checkboxParallelChannels;

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
        defaultPoolAddressTextField.setTooltip(new Tooltip("A default address to set when you open up the pool manager.\nIf you always use the tool on the same pool this will save you some time!"));

        defaultMemoTextField.setText(SETTING_MEMO);
        defaultMemoTextField.setTooltip(new Tooltip("The MEMO text that will be attached to all transactions."));
        defaultDonationsStringTextField.setText(SETTING_DONATION_DATANAME_PREFIX);
        defaultDonationsStringTextField.setTooltip(new Tooltip("The prefix of the data name field that will be recognized as a donation"));

        defaultFeeTextField.setText(String.valueOf(SETTING_FEE));

        // Horizon DB
        horizonTest.setText(SETTING_HORIZON_TEST_NETWORK);
        horizonLive.setText(SETTING_HORIZON_LIVE_NETWORK);
        horizonDbTestHost.setText(SETTING_HORIZON_DB_TEST_ADDRESS);
        horizonDbTestPort.setText(SETTING_HORIZON_DB_TEST_PORT);
        horizonDbTestUser.setText(SETTING_HORIZON_DB_TEST_USER);
        horizonDbTestPass.setText(SETTING_HORIZON_DB_TEST_PASS);
        horizonDbLiveHost.setText(SETTING_HORIZON_DB_LIVE_ADDRESS);
        horizonDbLivePort.setText(SETTING_HORIZON_DB_LIVE_PORT);
        horizonDbLiveUser.setText(SETTING_HORIZON_DB_LIVE_USER);
        horizonDbLivePass.setText(SETTING_HORIZON_DB_LIVE_PASS);

        // Channels
        checkboxParallelChannels.setSelected(SETTING_PARALLEL_CHANNELS_ENABLED);

        // Handle save & closing
        cancelBtn.setOnAction(event -> cancelBtn.getScene().getWindow().hide());
        saveBtn.setOnAction(event -> saveSettings());

        // Handle pay button color based on the network choice
        activeNetworkChoiceBox.setOnAction(event -> {
            final String selectedValue = activeNetworkChoiceBox.getValue();

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

        // Handle checkbox toggle for the parallel channels
        checkboxParallelChannels.setOnAction(event -> SETTING_PARALLEL_CHANNELS_ENABLED = checkboxParallelChannels.isSelected());
    }

    /**
     * Save current settings
     */
    private void saveSettings() {
        // Check default pool address
        if (!XLMUtils.isPublicKeyValidFormat(defaultPoolAddressTextField.getText())) {
            showError("The default pool address is not valid, make sure you have specified a valid PUBLIC KEY");

            return;
        }

        // Update settings from the textfields
        SETTING_OPERATIONS_NETWORK = activeNetworkChoiceBox.getValue();
        SETTING_INFLATION_POOL_ADDRESS = defaultPoolAddressTextField.getText();
        SETTING_MEMO = defaultMemoTextField.getText();
        SETTING_DONATION_DATANAME_PREFIX = defaultDonationsStringTextField.getText();

        final long currentFee = Long.parseLong(defaultFeeTextField.getText());
        if (currentFee < 100) {
            SETTING_FEE = 100;
            defaultFeeTextField.setText("100");
        } else {
            SETTING_FEE = currentFee;
        }

        // Horizon DB
        SETTING_HORIZON_TEST_NETWORK = horizonTest.getText();
        SETTING_HORIZON_LIVE_NETWORK = horizonLive.getText();
        SETTING_HORIZON_DB_TEST_ADDRESS = horizonDbTestHost.getText();
        SETTING_HORIZON_DB_TEST_PORT = horizonDbTestPort.getText();
        SETTING_HORIZON_DB_TEST_USER = horizonDbTestUser.getText();
        SETTING_HORIZON_DB_TEST_PASS = horizonDbTestPass.getText();
        SETTING_HORIZON_DB_LIVE_ADDRESS = horizonDbLiveHost.getText();
        SETTING_HORIZON_DB_LIVE_PORT = horizonDbLivePort.getText();
        SETTING_HORIZON_DB_LIVE_USER = horizonDbLiveUser.getText();
        SETTING_HORIZON_DB_LIVE_PASS = horizonDbLivePass.getText();

        // Channels
        SETTING_PARALLEL_CHANNELS_ENABLED = checkboxParallelChannels.isSelected();

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

