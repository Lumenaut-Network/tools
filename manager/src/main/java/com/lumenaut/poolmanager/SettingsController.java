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
    private TextField horizonDbAddress;

    @FXML
    private TextField horizonDbPort;

    @FXML
    private TextField horizonDbUser;

    @FXML
    private TextField horizonDbPass;

    @FXML
    private CheckBox checkboxParallelChannels;

    @FXML
    private TextField chan1AddressField;

    @FXML
    private TextField chan1KeyField;

    @FXML
    private TextField chan2AddressField;

    @FXML
    private TextField chan2KeyField;

    @FXML
    private TextField chan3AddressField;

    @FXML
    private TextField chan3KeyField;

    @FXML
    private TextField chan4AddressField;

    @FXML
    private TextField chan4KeyField;

    @FXML
    private TextField chan5AddressField;

    @FXML
    private TextField chan5KeyField;

    @FXML
    private TextField chan6AddressField;

    @FXML
    private TextField chan6KeyField;

    @FXML
    private TextField chan7AddressField;

    @FXML
    private TextField chan7KeyField;

    @FXML
    private TextField chan8AddressField;

    @FXML
    private TextField chan8KeyField;

    @FXML
    private TextField chan9AddressField;

    @FXML
    private TextField chan9KeyField;

    @FXML
    private TextField chan10AddressField;

    @FXML
    private TextField chan10KeyField;

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
        horizonDbAddress.setText(SETTING_HORIZON_DB_ADDRESS);
        horizonDbPort.setText(SETTING_HORIZON_DB_PORT);
        horizonDbUser.setText(SETTING_HORIZON_DB_USER);
        horizonDbPass.setText(SETTING_HORIZON_DB_PASS);

        // Channels
        checkboxParallelChannels.setSelected(SETTING_PARALLEL_CHANNELS_ENABLED);
        chan1AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_1);
        chan1KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_1);
        chan2AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_2);
        chan2KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_2);
        chan3AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_3);
        chan3KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_3);
        chan4AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_4);
        chan4KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_4);
        chan5AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_5);
        chan5KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_5);
        chan6AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_6);
        chan6KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_6);
        chan7AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_7);
        chan7KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_7);
        chan8AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_8);
        chan8KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_8);
        chan9AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_9);
        chan9KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_9);
        chan10AddressField.setText(SETTING_PARALLEL_CHANNEL_ADDRESS_10);
        chan10KeyField.setText(SETTING_PARALLEL_CHANNEL_KEY_10);

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
        SETTING_HORIZON_DB_ADDRESS = horizonDbAddress.getText();
        SETTING_HORIZON_DB_PORT = horizonDbPort.getText();
        SETTING_HORIZON_DB_USER = horizonDbUser.getText();
        SETTING_HORIZON_DB_PASS = horizonDbPass.getText();

        // Channels
        SETTING_PARALLEL_CHANNELS_ENABLED = checkboxParallelChannels.isSelected();
        SETTING_PARALLEL_CHANNEL_ADDRESS_1 = chan1AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_1 = chan1KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_2 = chan2AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_2 = chan2KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_3 = chan3AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_3 = chan3KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_4 = chan4AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_4 = chan4KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_5 = chan5AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_5 = chan5KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_6 = chan6AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_6 = chan6KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_7 = chan7AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_7 = chan7KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_8 = chan8AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_8 = chan8KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_9 = chan9AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_9 = chan9KeyField.getText();
        SETTING_PARALLEL_CHANNEL_ADDRESS_10 = chan10AddressField.getText();
        SETTING_PARALLEL_CHANNEL_KEY_10 = chan10KeyField.getText();

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

