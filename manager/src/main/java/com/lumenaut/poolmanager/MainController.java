package com.lumenaut.poolmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.lumenaut.poolmanager.DataFormats.InflationDataEntry;
import com.lumenaut.poolmanager.DataFormats.InflationData;
import com.lumenaut.poolmanager.gateways.FederationGateway;
import com.lumenaut.poolmanager.gateways.HorizonGateway;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;
import static com.lumenaut.poolmanager.Settings.*;

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

    /**
     * Updates the pool data counters
     */
    private void refreshPoolCounters() {
        final String inflationPoolData = inflationPoolDataTextArea.getText();
        if (!inflationPoolData.isEmpty()) {
            try {
                final InflationData inflationData = OBJECT_MAPPER.readValue(inflationPoolData, InflationData.class);
                poolDataVotersLabel.setText(String.valueOf(inflationData.getEntries().size()));

                Long totalVotes = 0L;
                for (InflationDataEntry voter : inflationData.getEntries()) {
                    totalVotes += voter.getBalance();
                }

                poolDataTotalVotesLabel.setText(XLMUtils.formatBalanceFullPrecision(totalVotes) + " XLM");
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
                    final JsonNode voters = horizonGateway.getVoters(poolAddress);

                    // Update the current voters data
                    currentVotersData = voters;

                    // Format and return
                    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(voters);
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
                Platform.runLater(() -> {
                    setBusyState(false);
                    refreshPoolCounters();
                    fetchPoolBalanceViaHorizon();
                });
            });
        }
    }

    /**
     * Fetch data from the federation network
     */
    private void fetchFedNetworkData() {
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
                Platform.runLater(() -> {
                    setBusyState(false);
                    refreshPoolCounters();
                    fetchPoolBalanceViaStellar();
                });
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
     * Fetch the pool balance and update the counter
     */
    private void fetchPoolBalanceViaHorizon() {
        // Check that the horizon gateway is connected
        if (!horizonGateway.isConnected()) {
            try {
                horizonGateway.connect();
            } catch (SQLException e) {
                showError("Cannot fetch pool balance, unable to establish horizon database connection: " + e.getMessage());

                return;
            }
        }

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
                    final BigDecimal balance = horizonGateway.getBalance(poolAddress);

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
            settingsStage.setHeight(400);
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

            // Initialize the transactions stage and show it
            transactionsBuilderStage.setTitle("Transactions Builder");
            transactionsBuilderStage.setScene(new Scene(transactionsBuilderFrame));
            transactionsBuilderStage.getIcons().add(new Image(Main.class.getResourceAsStream("/inflationManager.png")));
            transactionsBuilderStage.initModality(Modality.WINDOW_MODAL);
            transactionsBuilderStage.initOwner(primaryStage.getScene().getWindow());
            transactionsBuilderStage.setWidth(918);
            transactionsBuilderStage.setHeight(700);
            transactionsBuilderStage.setMinWidth(918);
            transactionsBuilderStage.setMinHeight(700);
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
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        } else {
            // Reconnect, the settings might have changed
            try {
                horizonGateway.reconnect();
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }

        // Instance ready
        return true;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

