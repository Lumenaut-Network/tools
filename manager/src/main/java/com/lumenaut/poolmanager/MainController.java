package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.VoterDataEntry;
import com.lumenaut.poolmanager.DataFormats.VotersData;
import com.lumenaut.poolmanager.gateways.HorizonGateway;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private Button getHorizonDataBtn;

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
    private VotersData currentVotersData;

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
        getHorizonDataBtn.setTooltip(new Tooltip("Retrieve voters data, pool balance and donations data using\n the specified Horizon database connection."));

        // Add all buttons that should react to the application "busy" state
        statefulButtons.add(getHorizonDataBtn);
        statefulButtons.add(buildTransactionBtn);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // MENU BUTTONS HANDLERS

        // Close
        closeBtn.setOnAction(event -> Platform.exit());

        // Settings
        settingsBtn.setOnAction(event -> openSettingsWindow());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // BUTTON HANDLERS

        getHorizonDataBtn.setOnAction(event -> fetchHorizonData());
        buildTransactionBtn.setOnAction(event -> {
            if (currentVotersData != null && currentVotersData.getEntries().size() > 0 && currentVotersData.getBalance() > 0L) {
                openTransactionBuilderWindow();
            } else {
                showError("You must first fetch the voters data");
            }
        });
    }

    /**
     * Updates the pool data counters
     */
    private void refreshPoolCounters() {
        if (currentVotersData != null && currentVotersData.getEntries().size() > 0) {
            poolDataVotersLabel.setText(String.valueOf(currentVotersData.getEntries().size()));

            long totalVotes = 0L;
            for (VoterDataEntry voter : currentVotersData.getEntries()) {
                totalVotes += voter.getBalance();
            }

            poolDataTotalVotesLabel.setText(XLMUtils.formatBalance(totalVotes) + " XLM");
            poolDataBalanceLabel.setText(XLMUtils.formatBalanceFullPrecision(currentVotersData.getBalance()));
        } else {
            showError("Cannot compute pool data, voters data is empty");
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
            if (currentVotersData != null) {
                currentVotersData.reset();
            }

            resetPoolCounters();

            // Disable buttons and start spinner
            setBusyState(true);

            // Notify user
            inflationPoolDataTextArea.clear();

            // Build and submit async task
            final CompletableFuture<VotersData> request = CompletableFuture.supplyAsync(() -> {
                final VotersData votersData;
                try {
                    // Initialize payment channels if required
                    if (SETTING_PARALLEL_CHANNELS_ENABLED) {
                        Platform.runLater(() -> inflationPoolDataTextArea.appendText("Verifying payment channels...\n"));
                        StellarGateway.initParallelSubmission(inflationPoolDataTextArea);

                        // Check if the channels were initialized
                        if (StellarGateway.getChannelAccounts() == null) {
                            Platform.runLater(() -> inflationPoolDataTextArea.appendText("FAILED: No channels were initialized for parallel submission!\n\n"));

                            // Reset current voters data
                            currentVotersData = null;
                            return null;
                        }

                        // Check if at least 1 channel was initialized
                        final int validChannelsNum = StellarGateway.getChannelAccounts().size();
                        if (validChannelsNum > 0) {
                            Platform.runLater(() -> inflationPoolDataTextArea.appendText("SUCCESS: Initialized [" + validChannelsNum + "] valid channels for parallel submission!\n\n"));
                        } else {
                            Platform.runLater(() -> inflationPoolDataTextArea.appendText("FAILED: No channels were initialized for parallel submission!\n\n"));

                            // Reset current voters data
                            currentVotersData = null;
                            return null;
                        }
                    }

                    // Notify user that data fetch is starting
                    Platform.runLater(() -> inflationPoolDataTextArea.appendText("Fetching data..."));

                    // Fetch
                    votersData = horizonGateway.getVotersData(poolAddress);

                    // Update the current voters data
                    currentVotersData = votersData;
                } catch (Exception ignored) {
                    // Fetch has failed
                    currentVotersData = null;
                }

                // Return
                return currentVotersData;
            });

            // Process task completion
            request.thenAccept(votersData -> {
                // Stop if we're meant to use payment channels but none have been successfully initialized
                if (SETTING_PARALLEL_CHANNELS_ENABLED && (StellarGateway.getChannelAccounts() == null || StellarGateway.getChannelAccounts().size() == 0)) {
                    Platform.runLater(() -> {
                        // Re-enable buttons and update counters
                        setBusyState(false);
                        resetPoolCounters();

                        // Manually disable the transaction build button, we have no data!
                        buildTransactionBtn.setDisable(true);

                        // Notify user
                        showError("None of the payment channels provided in the config can be used, either disable parallel submissions or provide valid channels");
                    });

                    return;
                }


                // Check if we have voters data
                if (votersData == null) {
                    Platform.runLater(() -> {
                        // Re-enable buttons and update counters
                        setBusyState(false);
                        resetPoolCounters();

                        // Manually disable the transaction build button, we have no data!
                        buildTransactionBtn.setDisable(true);

                        // Notify user
                        showError("The horizon database does not contain any data for the specified address");
                    });

                    return;
                }

                // Cancel applicationBusy state
                Platform.runLater(() -> {
                    // Re-enable buttons and update counters
                    setBusyState(false);
                    refreshPoolCounters();

                    // Notify user
                    inflationPoolDataTextArea.appendText(" DONE!\nREADY TO BUILD!");
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
            transactionsController.currentPoolBalance = XLMUtils.stroopToXLM(currentVotersData.getBalance());
            transactionsController.primaryStage = primaryStage;
            transactionsController.poolAddressTextField = poolAddressTextField;
            transactionsController.window = transactionsBuilderStage;

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

