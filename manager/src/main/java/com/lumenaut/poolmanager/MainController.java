package com.lumenaut.poolmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.lumenaut.poolmanager.DataFormats.VoterDataEntry;
import com.lumenaut.poolmanager.DataFormats.VotersData;
import com.lumenaut.poolmanager.gateways.FederationGateway;
import com.lumenaut.poolmanager.gateways.HorizonGateway;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;
import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.UIUtils.showError;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 10/01/2018 - 4:38 PM
 */
public class MainController {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FXML BINDINGS

    ////////////////////////////////////////////////////////
    // UI

    @FXML
    private AnchorPane primaryStage;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Rectangle selectedNetworkRect;

    ////////////////////////////////////////////////////////
    // BUTTONS

    @FXML
    private MenuItem settingsBtn;

    @FXML
    private MenuItem closeBtn;

    @FXML
    private Button getFederationDataBtn;

    @FXML
    private Button getHorizonDataBtn;

    @FXML
    private Button refreshPoolBalanceBtn;

    @FXML
    private Button buildTransactionBtn;

    ////////////////////////////////////////////////////////
    // TEXT FIELDS

    @FXML
    private TextField poolAddressTextField;

    ////////////////////////////////////////////////////////
    // TEXT AREAS
    @FXML
    private TextArea inflationPoolDataTextArea;

    ////////////////////////////////////////////////////////
    // LABELS

    @FXML
    private Label poolDataVotersLabel;

    @FXML
    private Label poolDataTotalVotesLabel;

    @FXML
    private Label poolDataBalanceLabel;

    @FXML
    private Label selectedNetworkLabel;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Busy signal
    private final AtomicBoolean applicationBusy = new AtomicBoolean(false);

    // Buttons to disable when the application is busy
    private final List<Button> statefulButtons = new ArrayList<>();

    // Horizon gateway instance
    private HorizonGateway horizonGateway;

    // Current voters data
    private JsonNode currentVotersData;
    private BigDecimal currentPoolBalance;

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
    //region METHODS

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Load application settings
        try {
            Settings.loadSettings();
        } catch (IOException e) {
            showError("Error initializing application settings: " + e.getMessage());
        }

        // Set the default pool address if we have it in the settings
        if (SETTING_INFLATION_POOL_ADDRESS != null && !SETTING_INFLATION_POOL_ADDRESS.isEmpty()) {
            poolAddressTextField.setText(SETTING_INFLATION_POOL_ADDRESS);
        }

        // Update visual clues for the selected network
        switch (SETTING_OPERATIONS_NETWORK) {
            case "TEST":
                selectedNetworkRect.setFill(Paint.valueOf("#64EE64"));
                selectedNetworkLabel.setText("TEST NETWORK");

                break;
            case "LIVE":
                selectedNetworkRect.setFill(Paint.valueOf("#EE4B52"));
                selectedNetworkLabel.setText("LIVE NETWORK");

                break;
        }

        // Init tooltips
        getFederationDataBtn.setTooltip(new Tooltip("Retrieve voters data and pool balance using Fed.network.\n !!! No donations data will be available !!!"));
        getHorizonDataBtn.setTooltip(new Tooltip("Retrieve voters data, pool balance and donations data using\n the specified Horizon database connection."));
        refreshPoolBalanceBtn.setTooltip(new Tooltip("Will update the pool balance using the SDF horizon servers.\n Based on the configured network (LIVE/TEST)"));

        // Add all buttons that should react to the application "busy" state
        statefulButtons.add(getFederationDataBtn);
        statefulButtons.add(getHorizonDataBtn);
        statefulButtons.add(refreshPoolBalanceBtn);
        statefulButtons.add(buildTransactionBtn);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // MENU BUTTONS HANDLERS

        // Close
        closeBtn.setOnAction(event -> Platform.exit());

        // Settings
        settingsBtn.setOnAction(event -> openSettingsWindow());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // BUTTON HANDLERS

        getFederationDataBtn.setOnAction(event -> fetchFedNetworkData());
        getHorizonDataBtn.setOnAction(event -> fetchHorizonData());
        refreshPoolBalanceBtn.setOnAction(event -> fetchPoolBalanceViaStellar());
        buildTransactionBtn.setOnAction(event -> {
            if (currentVotersData != null && currentPoolBalance != null) {
                openTransactionBuilderWindow();
            } else {
                showError("You must first fetch the voters data and the pool balance");
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // DATA HANDLERS

        // Prevent direct input in the pool data text area
        inflationPoolDataTextArea.setOnKeyTyped(Event::consume);

        // Filter key presses to only allow CTRL+A/C/V through
        inflationPoolDataTextArea.setOnKeyPressed(event -> {
            if (!event.isControlDown()) {
                if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                    // Let Backspace and delete clear the whole textarea
                    inflationPoolDataTextArea.setText("");
                } else {
                    event.consume();
                }
            } else if (event.getCode() != KeyCode.C && event.getCode() != KeyCode.V && event.getCode() != KeyCode.A) {
                event.consume();
            }
        });

        // Detect changes in the text area, if the JSON is valid, enable the transaction builder button
        inflationPoolDataTextArea.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                // The data has changed and it's not empty, process it
                handleVotersDataChange(newValue);
            } else {
                // Reset data labels and disable the transaction builder button
                buildTransactionBtn.setDisable(true);
                poolDataBalanceLabel.setText("0 XLM");
                poolDataVotersLabel.setText("0");
                poolDataTotalVotesLabel.setText("0");
            }
        });
    }

    /**
     * Handles changes in the voters data textarea
     *
     * @param newValue
     */
    private void handleVotersDataChange(final String newValue) {
        Platform.runLater(() -> {
            // Start spinning
            setBusyState(true);

            try {
                final VotersData votersData = OBJECT_MAPPER.readValue(newValue, VotersData.class);
                if (votersData != null) {
                    // Only update the runtime data if we have both balance and inflation destination entries.
                    // If these two are missing it means that the data has been fetched via fed.network, otherwise
                    // the data either comes from our horizon network implementation or has been pasted from a snapshot
                    if (votersData.getInflationdest() != null && !votersData.getInflationdest().isEmpty() && votersData.getBalance() != 0L) {
                        // The transaction data has parsed, enable transaction button
                        buildTransactionBtn.setDisable(false);

                        // Update inflation destination
                        poolAddressTextField.setText(votersData.getInflationdest());

                        // Update the balance label
                        currentPoolBalance = XLMUtils.stroopToXLM(votersData.getBalance());
                        poolDataBalanceLabel.setText(XLMUtils.formatBalance(votersData.getBalance()) + " XLM");

                        // Update the current voters data
                        currentVotersData = OBJECT_MAPPER.valueToTree(votersData);

                        // Update the pool counters (voters number and votes)
                        refreshPoolCounters();
                    }

                    // Stop spinning
                    setBusyState(false);
                } else {
                    // Stop spinning
                    setBusyState(false);

                    // Reset data labels and disable the transaction builder button
                    buildTransactionBtn.setDisable(true);
                    poolDataBalanceLabel.setText("0 XLM");
                    poolDataVotersLabel.setText("0");
                    poolDataTotalVotesLabel.setText("0");
                    showError("Error parsing voters data response, unable to map object data");
                }
            } catch (IOException e) {
                // Stop spinning
                setBusyState(false);

                // Reset data labels and disable the transaction builder button
                buildTransactionBtn.setDisable(true);
                poolDataBalanceLabel.setText("0 XLM");
                poolDataVotersLabel.setText("0");
                poolDataTotalVotesLabel.setText("0");
                showError("Error parsing voters data response: " + e.getMessage());
            }
        });
    }

    /**
     * Updates the pool data counters
     */
    private void refreshPoolCounters() {
        final String inflationPoolData = inflationPoolDataTextArea.getText();
        if (inflationPoolData != null && !inflationPoolData.isEmpty() && !inflationPoolData.equals("null")) {
            try {
                final VotersData inflationData = OBJECT_MAPPER.readValue(inflationPoolData, VotersData.class);
                poolDataVotersLabel.setText(String.valueOf(inflationData.getEntries().size()));

                Long totalVotes = 0L;
                for (VoterDataEntry voter : inflationData.getEntries()) {
                    totalVotes += voter.getBalance();
                }

                poolDataTotalVotesLabel.setText(XLMUtils.formatBalance(totalVotes) + " XLM");
            } catch (IOException e) {
                showError("Cannot compute pool data: " + e.getMessage());
            }
        }
    }

    /**
     * Resets the pool data counters
     */
    private void resetPoolCounters() {
        poolDataVotersLabel.setText("0");
        poolDataTotalVotesLabel.setText("0 XLM");
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

    /**
     * Fetch data from horizon
     */
    private void fetchHorizonData() {
        // Initialize horizon manager
        if (!initHorizonDatabaseConnection()) {
            return;
        }


        // Get the current network of the horizon node
        boolean horizonTestNetwork;
        try {
            horizonTestNetwork = horizonGateway.isTestNetwork();
        } catch (Exception e) {
            showError("Cannot determine whether the horizon node is on the test network: " + e.getMessage());
            return;
        }

        // Check if the networks match
        switch (SETTING_OPERATIONS_NETWORK) {
            case "TEST":
                if (!horizonTestNetwork) {
                    showError("The horizon node specified seems to be connected to the LIVE network, you are working on the TEST network. Change your settings and try again.");
                    return;
                }

                break;
            case "LIVE":
                if (horizonTestNetwork) {
                    showError("The horizon node specified seems to be connected to the TEST network, you are working on the LIVE network. Change your settings and try again.");
                    return;
                }

                break;
        }

        // Get the target pool key
        final String poolAddress = poolAddressTextField.getText();

        // Check if we have an address
        if (poolAddress == null || poolAddress.isEmpty()) {
            showError("You must specify the inflation pool's address below");
        } else {
            // Clear existing data
            inflationPoolDataTextArea.clear();
            poolDataBalanceLabel.setText("0 XLM");
            currentVotersData = null;
            resetPoolCounters();

            // Start spinning
            setBusyState(true);

            // Build and submit async task
            final CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> {
                try {
                    // Fetch the voters from the federation network
                    final JsonNode votersData = horizonGateway.getVotersData(poolAddress);

                    // Update the current voters data
                    currentVotersData = votersData;

                    // Format and return
                    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(votersData);
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
                if (inflationPoolData == null || inflationPoolData.isEmpty() || inflationPoolData.equals("null")) {
                    showError("The horizon database does not contain any data for the specified address");

                    Platform.runLater(() -> {
                        setBusyState(false);
                        resetPoolCounters();
                    });

                    return;
                }

                // Cancel applicationBusy state
                Platform.runLater(() -> {
                    // Update with the pool data
                    inflationPoolDataTextArea.setText(inflationPoolData);

                    setBusyState(false);
                    refreshPoolCounters();
                });
            });
        }
    }

    /**
     * Fetch data from the federation network
     */
    private void fetchFedNetworkData() {
        if (SETTING_OPERATIONS_NETWORK.equals("TEST")) {
            showError("Cannot fetch inflation pool's data from " + SETTING_FEDERATION_NETWORK_INFLATION_URL + " while on the TEST network!");
            return;
        }

        // Get the target pool key
        final String poolAddress = poolAddressTextField.getText();

        // Check if we have an address
        if (poolAddress == null || poolAddress.isEmpty()) {
            showError("You must specify the inflation pool's address below");
        } else {
            // Clear existing data
            inflationPoolDataTextArea.clear();
            poolDataBalanceLabel.setText("0 XLM");
            currentVotersData = null;
            resetPoolCounters();

            // Start spinning
            setBusyState(true);

            // Build and submit async task
            final CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> {
                try {
                    // Fetch the voters from the federation network
                    final JsonNode voters = FederationGateway.getVoters(poolAddress);

                    // Update the current voters data
                    currentVotersData = voters;

                    // Format and return
                    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(voters);
                } catch (Exception e) {
                    // Cancel applicationBusy state and show error
                    Platform.runLater(() -> {
                        setBusyState(false);
                        showError("Unable to retrieve data for the pool address you specified (" + poolAddress + "):\n\n" + e.getMessage() +
                                  "\n\nPlease check that the address exists in the same network as your settings!");
                    });
                }

                return null;
            });

            // Process task completion
            request.thenAccept(inflationPoolData -> {
                if (inflationPoolData != null) {
                    // Update text area
                    inflationPoolDataTextArea.setText(inflationPoolData);

                    // Cancel applicationBusy state
                    Platform.runLater(() -> {
                        refreshPoolCounters();
                        fetchPoolBalanceViaStellar(); // This will cancel the busy state we initiated once it completes
                    });
                }
            });
        }
    }

    /**
     * Fetch the pool balance and update the counter
     */
    private void fetchPoolBalanceViaStellar() {
        // Get the target pool key
        final String poolAddress = poolAddressTextField.getText();

        // Check if we have an address
        if (poolAddress == null || poolAddress.isEmpty()) {
            showError("You must specify the inflation pool's address below");
        } else {
            // Reset balance
            poolDataBalanceLabel.setText("0 XLM");
            currentPoolBalance = null;

            // Start spinning
            setBusyState(true);

            // Build and submit async task
            final CompletableFuture<BigDecimal> request = CompletableFuture.supplyAsync(() -> {
                try {
                    final BigDecimal balance = StellarGateway.getBalance(poolAddress);

                    // Update current balance
                    currentPoolBalance = balance;

                    return balance;
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        // Return to normal operation
                        setBusyState(false);

                        // Show the error
                        showError(e.getMessage());
                    });
                }

                return null;
            });

            // Process task completion
            request.thenAccept(poolBalance -> {
                // Cancel applicationBusy state
                Platform.runLater(() -> {
                    // Return to normal operation
                    setBusyState(false);

                    // Update label
                    poolDataBalanceLabel.setText(XLMUtils.formatBalance(poolBalance) + " XLM");
                });
            });
        }
    }

    /**
     * Open the settings panel
     */
    private void openSettingsWindow() {
        // Build root
        try {
            // Create new stage
            final Stage settingsStage = new Stage();
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/inflationManagerSettings.fxml"));
            final AnchorPane settingsFrame = fxmlLoader.load();
            final SettingsController settingsController = fxmlLoader.getController();

            // Bind references in the settings controller
            settingsController.selectedNetworkRect = selectedNetworkRect;
            settingsController.selectedNetworkLabel = selectedNetworkLabel;

            // Initialize the settings stage and show it
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(settingsFrame));
            settingsStage.getIcons().add(new Image(Main.class.getResourceAsStream("/inflationManager.png")));
            settingsStage.initModality(Modality.WINDOW_MODAL);
            settingsStage.initOwner(primaryStage.getScene().getWindow());
            settingsStage.setWidth(700);
            settingsStage.setHeight(600);
            settingsStage.setResizable(false);
            settingsStage.show();
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Open the settings panel
     */
    private void openTransactionBuilderWindow() {
        // Build root
        try {
            // Create new stage
            final Stage transactionsBuilderStage = new Stage();
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/inflationManagerTransactions.fxml"));
            final AnchorPane transactionsBuilderFrame = fxmlLoader.load();
            final TransactionsController transactionsController = fxmlLoader.getController();

            // Bind references in the settings controller
            transactionsController.currentVotersData = currentVotersData;
            transactionsController.currentPoolBalance = currentPoolBalance;
            transactionsController.primaryStage = primaryStage;
            transactionsController.poolAddressTextField = poolAddressTextField;

            // Initialize the transactions stage and show it
            transactionsBuilderStage.setTitle("Transactions Builder");
            transactionsBuilderStage.setScene(new Scene(transactionsBuilderFrame));
            transactionsBuilderStage.getIcons().add(new Image(Main.class.getResourceAsStream("/inflationManager.png")));
            transactionsBuilderStage.initModality(Modality.WINDOW_MODAL);
            transactionsBuilderStage.initOwner(primaryStage.getScene().getWindow());
            transactionsBuilderStage.setWidth(918);
            transactionsBuilderStage.setHeight(600);
            transactionsBuilderStage.setMinWidth(918);
            transactionsBuilderStage.setMinHeight(600);
            transactionsBuilderStage.show();
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Initialize the horizon manager instance for database operations
     */
    private boolean initHorizonDatabaseConnection() {
        // Check if we have all the settings required
        if (SETTING_HORIZON_DB_ADDRESS.isEmpty() ||
            SETTING_HORIZON_DB_PORT.isEmpty() ||
            SETTING_HORIZON_DB_USER.isEmpty() ||
            SETTING_HORIZON_DB_PASS.isEmpty()) {
            showError("You must specify all required connection settings in order to use a Horizon node database.");

            return false;
        }

        if (horizonGateway == null) {
            // Create a new horizon manager instance
            horizonGateway = new HorizonGateway();

            // Connect
            try {
                horizonGateway.connect();
            } catch (Exception e) {
                showError(e.getMessage());
            }
        } else {
            // Reconnect, the settings might have changed
            try {
                horizonGateway.reconnect();
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }

        // Instance ready
        return true;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

