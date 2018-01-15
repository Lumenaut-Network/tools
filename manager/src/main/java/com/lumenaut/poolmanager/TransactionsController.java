package com.lumenaut.poolmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.lumenaut.poolmanager.DataFormats.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.lumenaut.poolmanager.DataFormats.*;
import static com.lumenaut.poolmanager.Settings.ROUNDING_MODE;
import static com.lumenaut.poolmanager.Settings.SETTING_OPERATIONS_NETWORK;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 12/01/2018 - 6:46 PM
 */
public class TransactionsController {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FXML BINDINGS

    ////////////////////////////////////////////////////////
    // UI

    @FXML
    private AnchorPane transactionPlannerStage;

    @FXML
    private TextArea transactionPlanTextArea;

    @FXML
    private TextArea exclusionsTextArea;

    @FXML
    private TextArea reroutingTextArea;

    @FXML
    private TextField signingKeyTextField;

    @FXML
    private TextField inflationAmountTextField;

    ////////////////////////////////////////////////////////
    // BUTTONS

    @FXML
    private Button rebuildTransactionPlanBtn;

    @FXML
    private Button saveTransactionPlanBtn;

    @FXML
    private Button saveExclusionsBtn;

    @FXML
    private Button saveReroutingBtn;

    @FXML
    private Button executeTransactionBtn;

    ////////////////////////////////////////////////////////
    // LABELS

    @FXML
    private Label excludedTransactionsLabel;

    @FXML
    private Label reroutedTransactionsLabel;

    @FXML
    private Label plannedTransactionsLabel;

    @FXML
    private Label executedTransactionsLabel;

    @FXML
    private Label toBePaidLabel;

    @FXML
    private Label paidLabel;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // File names
    public static final String DATA_EXCLUSIONS_JSON_PATH = "data/exclusions.json";
    public static final String DATA_REROUTING_JSON_PATH = "data/rerouting.json";
    public static final String TRANSACTION_PLAN_JSON_SUFFIX = "transaction_plan.json";
    public static final DateFormat FILE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_");

    // Data bindings from the MainController
    public JsonNode currentVotersData;
    public BigDecimal currentPoolBalance;
    public AnchorPane primaryStage;

    // Data
    private HashMap<String, Long> votesAndBalances;
    private HashMap<String, Long> votesAndPayments;

    private TransactionPlan transactionPlan;
    private ExclusionData exclusionDataData;
    private ReroutingData reroutingData;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    public TransactionsController() {

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
        // Update visual clues for the selected network
        switch (SETTING_OPERATIONS_NETWORK) {
            case "TEST":
                executeTransactionBtn.getStyleClass().removeAll("redBg");
                executeTransactionBtn.getStyleClass().add("greenBg");

                break;
            case "LIVE":
                executeTransactionBtn.getStyleClass().removeAll("greenBg");
                executeTransactionBtn.getStyleClass().add("redBg");

                break;
        }

        // Load default data for exclusions and rerouting
        loadExclusionsData();
        loadReroutingData();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TEXTFIELD HANDLERS

        inflationAmountTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                validateAmountToPay();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // BUTTON HANDLERS

        rebuildTransactionPlanBtn.setOnAction(event -> {
            // Don't even attempt to build the plan if the amount to pay is not valid
            if (!validateAmountToPay()) {
                resetPlanUI();

                return;
            }

            // Try to make a new plan
            if (buildTransactionPlan()) {
                updatePlanUIandActivateExecution();
            } else {
                resetPlanUI();
            }
        });

        executeTransactionBtn.setOnAction(event -> {
            // Check signature
            final String signingKey = signingKeyTextField.getText();

            // Check if we have a signign key
            if (signingKey.isEmpty()) {
                showError("You must specify the signing key for the transactions");
            } else {
                // Save current transactions plan, abort if it fails
                if (!saveTransactionPlan(true)) {
                    return;
                }

                // Start the payment process
                try {
                    // Create new stage
                    final Stage stage = new Stage();
                    final FXMLLoader fxmlLoader = new FXMLLoader();
                    fxmlLoader.setLocation(getClass().getResource("/inflationManagerProcessing.fxml"));
                    final AnchorPane transactionsProcessingFrame = fxmlLoader.load();
                    final ProcessingController processingController = fxmlLoader.getController();

                    // Bind references in the settings controller
                    processingController.primaryStage = primaryStage;
                    processingController.transactionPlan = transactionPlan;
                    processingController.signingKey = signingKey;

                    // Initialize the transactions stage and show it
                    stage.setTitle("Transactions progress");
                    stage.setScene(new Scene(transactionsProcessingFrame));
                    stage.getIcons().add(new Image(Main.class.getResourceAsStream("/inflationManager.png")));
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.initOwner(transactionPlannerStage.getScene().getWindow());
                    stage.initStyle(StageStyle.UNDECORATED);
                    stage.setWidth(primaryStage.getWidth() + 80);
                    stage.setHeight(primaryStage.getHeight() + 50);

                    // Open
                    stage.show();
                } catch (IOException e) {
                    showError(e.getMessage());
                }
            }
        });

        saveExclusionsBtn.setOnAction(event -> saveExclusionsData());
        saveReroutingBtn.setOnAction(event -> saveReroutingData());
        saveTransactionPlanBtn.setOnAction(event -> saveTransactionPlan(false));
    }

    /**
     * Updates the planner ui with the data from the currently planned transactions
     *
     * @throws JsonProcessingException
     */
    private void updatePlanUIandActivateExecution() {
        // Update plan text area
        try {
            transactionPlanTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(transactionPlan));
        } catch (JsonProcessingException e) {
            showError(e.getMessage());

            return;
        }

        // Update planned total operations
        plannedTransactionsLabel.setText(String.valueOf(transactionPlan.getEntries().size()));

        // Update total amount to pay
        toBePaidLabel.setText(XLMUtils.formatBalanceFullPrecision(transactionPlan.getTotalpayment()) + " XLM");

        // Update the rerouting and exclusion labels
        reroutedTransactionsLabel.setText(String.valueOf(transactionPlan.getRerouted()));
        excludedTransactionsLabel.setText(String.valueOf(transactionPlan.getExcluded()));

        // Activate execution
        executeTransactionBtn.setDisable(false);
    }

    /**
     * Reset the current transaction plan and all the counters
     */
    private void resetPlanUI() {
        plannedTransactionsLabel.setText("0");
        executedTransactionsLabel.setText("0");
        reroutedTransactionsLabel.setText("0");
        toBePaidLabel.setText("0 XLM");
        executeTransactionBtn.setDisable(true);
        transactionPlan = null;
    }

    /**
     * Build a new transactions plan
     */
    private boolean buildTransactionPlan() {
        if (inflationAmountTextField.getText().isEmpty()) {
            showError("Cannot build transaction plan without specifying the amount that you wish to be distributed. Please fill in the \"Inflation Amount\" field.");

            return false;
        } else if (currentPoolBalance == null || currentVotersData == null) {
            showError("Cannot build transaction plan, voters data or pool balance missing, close this transaction planner and fetch new data");

            return false;
        } else {
            // Reset votes data
            if (votesAndBalances == null) {
                votesAndBalances = new HashMap<>();
            } else {
                votesAndBalances.clear();
            }

            // Populate it
            final JsonNode votesEntries = currentVotersData.get("entries");
            if (votesEntries.isArray()) {
                for (final JsonNode entry : votesEntries) {
                    final Long balance = entry.get("balance").asLong();
                    final String account = entry.get("account").asText();

                    // Append to the votes list
                    votesAndBalances.put(account, balance);
                }
            } else {
                showError("Voters data format is invalid");

                return false;
            }

            // !!! IMPORTANT !!!
            // Total votes balance is always computed BEFORE exclusions
            long totalVotesAmount = 0L;
            for (long voterBalance : votesAndBalances.values()) {
                totalVotesAmount += voterBalance;
            }

            // Parse inflation amount
            final String inflationAmountString = inflationAmountTextField.getText();
            long inflationAmount;
            if (XLMUtils.isStroopFormat(inflationAmountString)) {
                inflationAmount = Long.parseLong(inflationAmountString);
            } else if (XLMUtils.isDecimalFormat(inflationAmountString)) {
                inflationAmount = XLMUtils.XLMToStroop(XLMUtils.decimalStringToXLM(inflationAmountString));
            } else {
                showError("Invalid inflation amount: " + inflationAmountString);

                return false;
            }

            // If the pool's balance is not enough to cover the payment, bail out
            if (inflationAmount > XLMUtils.XLMToStroop(currentPoolBalance)) {
                return false;
            }

            // Reset voters and payments data
            if (votesAndPayments == null) {
                votesAndPayments = new HashMap<>();
            } else {
                votesAndPayments.clear();
            }

            // Populate it
            for (HashMap.Entry<String, Long> voter : votesAndBalances.entrySet()) {
                final long voterBalance = voter.getValue();
                votesAndPayments.put(voter.getKey(), computeVoterPayout(inflationAmount, totalVotesAmount, voterBalance));
            }

            // Get exclusion data
            ExclusionData exclusionData;
            try {
                exclusionData = OBJECT_MAPPER.readValue(exclusionsTextArea.getText(), ExclusionData.class);
            } catch (IOException e) {
                showError("Cannot parse exclusion data: " + e.getMessage());

                return false;
            }

            // Get rerouting data
            ReroutingData reroutingData;
            try {
                reroutingData = OBJECT_MAPPER.readValue(reroutingTextArea.getText(), ReroutingData.class);
            } catch (IOException e) {
                showError("Cannot parse rerouting data: " + e.getMessage());

                return false;
            }

            // Generate UUID
            final String uuid = UUID.randomUUID().toString();

            // Build new transaction plan
            final TransactionPlan newPlan = new TransactionPlan();
            newPlan.setEntries(new LinkedList<>());
            newPlan.setUuid(uuid);

            // Init rerouting and exclusion counters
            final AtomicInteger rerouted = new AtomicInteger(0);
            final AtomicInteger excluded = new AtomicInteger(0);
            final AtomicLong totalPayments = new AtomicLong(0);

            // Set total votes balance
            newPlan.setTotalvotes(totalVotesAmount);

            // Compute plan entries
            votesAndPayments.forEach((voterAccount, voterAmount) -> {
                // Create new entry node
                final TransactionPlanEntry entry = new TransactionPlanEntry();
                entry.setAmount(voterAmount);
                entry.setDestination(voterAccount);

                // Reroute if needed
                for (ReroutingDataEntry reroutingDataEntry : reroutingData.getEntries()) {
                    if (voterAccount.equals(reroutingDataEntry.getAccount())) {
                        // Reroute
                        entry.setDestination(reroutingDataEntry.getReroute());
                        entry.setReroutedfrom(voterAccount);

                        // Update rerouted counter
                        rerouted.getAndIncrement();
                    }
                }

                // If this account (or its rerouted address) is in the exclusion list skip appending it
                for (ExclusionEntry exclusionEntry : exclusionData.getEntries()) {
                    if (exclusionEntry.getDestination().equals(entry.getReroutedfrom())) {
                        excluded.getAndIncrement();

                        // Return immediately without appending the entry
                        return;
                    }

                    if (exclusionEntry.getDestination().equals(entry.getDestination())) {
                        excluded.getAndIncrement();

                        // Return immediately without appending the entry
                        return;
                    }
                }

                // Update total payment
                totalPayments.getAndAdd(voterAmount);

                // Append to the entries
                newPlan.getEntries().add(entry);
            });

            // Update the rerouting and exclusion
            newPlan.setRerouted(rerouted.get());
            newPlan.setExcluded(excluded.get());
            newPlan.setTotalpayment(totalPayments.get());

            // Update the transaction plan
            transactionPlan = newPlan;

            // Plan ready
            return true;
        }
    }

    /**
     * Compute the amount of inflation this account should be sent
     *
     * @param inflationAmountToPay
     * @param totalVotesAmount
     * @param voterBalance
     * @return
     */
    private long computeVoterPayout(final long inflationAmountToPay, final long totalVotesAmount, final long voterBalance) {
        // Constants in the payout calculation
        final BigDecimal totalInflation = XLMUtils.stroopToXLM(inflationAmountToPay);
        final BigDecimal totalBalance = XLMUtils.stroopToXLM(totalVotesAmount);
        final BigDecimal grossVoterBalance = XLMUtils.stroopToXLM(voterBalance);
        final BigDecimal fee = XLMUtils.stroopToXLM(100 * 2);

        // Percent of the total balance represented by the voter
        final BigDecimal voterPercentOfTotalBalance = grossVoterBalance.divide(totalBalance, ROUNDING_MODE);

        // Amount to pay
        final BigDecimal payout = totalInflation.multiply(voterPercentOfTotalBalance).subtract(fee);

        // Convert back to stroops
        return XLMUtils.XLMToStroop(payout);
    }

    /**
     * Load the existing exclusions file
     */
    private boolean loadExclusionsData() {
        final StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_EXCLUSIONS_JSON_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
        } catch (IOException ignore) {
            return false;
        }

        // Update the runtime json object
        try {
            exclusionDataData = OBJECT_MAPPER.readValue(contents.toString(), ExclusionData.class);
            exclusionsTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(exclusionDataData));
        } catch (IOException e) {
            showError("Exclusions list format error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Saves the exclusions data currently present in the text area
     */
    private void saveExclusionsData() {
        // Read the current contents of the text area
        final String contents = exclusionsTextArea.getText();

        // Try to decode them to see if they are in a valid format
        try {
            OBJECT_MAPPER.readValue(contents, ExclusionData.class);
        } catch (IOException e) {
            showError("Exclusions list format error: " + e.getMessage());

            return;
        }

        // Save to file
        try (
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(DATA_EXCLUSIONS_JSON_PATH), "UTF-8");
        BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(contents);
        } catch (IOException e) {
            showError("Cannot write exclusions list file [" + System.getProperty("user.dir") + "/" + DATA_EXCLUSIONS_JSON_PATH + "]: " + e.getMessage());
        }
    }

    /**
     * Load the existing exclusions file
     */
    private boolean loadReroutingData() {
        final StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_REROUTING_JSON_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
        } catch (IOException ignore) {
            return false;
        }

        // Update the runtime json object
        try {
            reroutingData = OBJECT_MAPPER.readValue(contents.toString(), ReroutingData.class);
            reroutingTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(reroutingData));
        } catch (IOException e) {
            showError("Exclusions list format error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Saves the rerouting data currently present in the text area
     */
    private void saveReroutingData() {
        // Read the current contents of the text area
        final String contents = reroutingTextArea.getText();

        // Try to decode them to see if they are in a valid format
        try {
            OBJECT_MAPPER.readValue(contents, ReroutingData.class);
        } catch (IOException e) {
            showError("Rerouting list format error: " + e.getMessage());

            return;
        }

        // Save to file
        try (
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(DATA_REROUTING_JSON_PATH), "UTF-8");
        BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(contents);
        } catch (IOException e) {
            showError("Cannot write Rerouting list file [" + System.getProperty("user.dir") + "/" + DATA_REROUTING_JSON_PATH + "]: " + e.getMessage());
        }
    }

    /**
     * Saves the current transaction plan
     */
    private boolean saveTransactionPlan(boolean quietMode) {
        // Read the current contents of the text area
        final String contents = transactionPlanTextArea.getText();

        if (contents.isEmpty()) {
            showInfo("Nothing to save. Build a transaction plan first!");

            return false;
        }

        // Try to decode them to see if they are in a valid format
        try {
            OBJECT_MAPPER.readValue(contents, TransactionPlan.class);
        } catch (IOException e) {
            showError("Transaction plan format error: " + e.getMessage());

            return false;
        }

        // Save to file
        final String outPutFilePath = "data/" + FILE_DATE_FORMATTER.format(new Date()) + TRANSACTION_PLAN_JSON_SUFFIX;
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outPutFilePath), "UTF-8");
             BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(contents);
        } catch (IOException e) {
            showError("Cannot write transaction plan file [" + outPutFilePath + "]: " + e.getMessage());

            return false;
        }

        // Show info
        if (!quietMode) {
            showInfo("The current transaction plan has been saved in the following file: " + outPutFilePath);
        }

        return true;
    }

    /**
     * Validate the currently set amount to pay
     */
    private boolean validateAmountToPay() {
        final String amountText = inflationAmountTextField.getText();

        if (amountText != null && !amountText.isEmpty()) {
            // Check that the amount specified can be parsed, otherwise fail
            final String inflationAmountString = inflationAmountTextField.getText();
            long inflationAmount;
            if (XLMUtils.isPositiveStroopFormat(inflationAmountString)) {
                inflationAmount = Long.parseLong(inflationAmountString);
            } else if (XLMUtils.isPositiveDecimalFormat(inflationAmountString)) {
                inflationAmount = XLMUtils.XLMToStroop(XLMUtils.decimalStringToXLM(inflationAmountString));
            } else {
                showError("Invalid payment amount. Please make sure you enter a positive value in either decimal (1234.1234567) or long (1234567890123456789) format");

                return false;
            }

            // If the pool's balance is not enough to cover the payment, fail
            if (inflationAmount > XLMUtils.XLMToStroop(currentPoolBalance)) {
                showError("The pool does not have enough balance to pay the inflation amount you specified. "
                          + "Pool balance: " + XLMUtils.formatBalanceFullPrecision(currentPoolBalance) + " XLM, "
                          + "Inflation payment requires: " + XLMUtils.formatBalanceFullPrecision(inflationAmount) + " XLM");

                return false;
            }

            // All checks passed
            return true;
        }

        return true;
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

