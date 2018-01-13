package com.lumenaut.poolmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;
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

    // Data bindings from the MainController
    public JsonNode currentVotersData;
    public BigDecimal currentPoolBalance;

    // Transaction plan data
    private HashMap<String, Long> votesAndBalances;
    private HashMap<String, Long> votesAndPayments;
    private JsonNode transactionPlan;

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

        // TEXTFIELD HANDLERS
        inflationAmountTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                final String amountText = inflationAmountTextField.getText();
                if (!newValue && !amountText.isEmpty()) {
                    // Check if the amount has been expressed in a format we understand
                    if (!XLMUtils.isPositiveBalanceFormat(amountText) && !XLMUtils.isPositiveDecimalFormat(amountText)) {
                        showError("Invalid payment amount. Please make sure you enter a positive value in either decimal (1234.1234567) or long (1234567890123456789) format");
                    }
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // BUTTON HANDLERS

        rebuildTransactionPlanBtn.setOnAction(event -> {
            if (buildTransactionPlan()) {
                try {
                    // Update plan text area
                    transactionPlanTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(transactionPlan));

                    // Compute total payment required
                    long requiredTotalPaymentBalance = 0;
                    for (long amount : votesAndPayments.values()) {
                        requiredTotalPaymentBalance += amount;
                    }

                    // Update planned total operations
                    plannedTransactionsLabel.setText(String.valueOf(votesAndBalances.size()));

                    // Update total amount to pay
                    toBePaidLabel.setText(XLMUtils.formatBalanceFullPrecision(requiredTotalPaymentBalance) + " XLM");
                } catch (JsonProcessingException e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    /**
     * Build a new transactions plan
     */
    private boolean buildTransactionPlan() {
        if (inflationAmountTextField.getText().isEmpty()) {
            showError("Cannot build transaction plan without previously specifying the amount that you wish to be distributed. " +
                      "This should match the last inflation deposit, expressed in 1/10.000.000ths of XLM. " +
                      "If you only have the amount expressed in XLM don't forget to multiply that by 10.000.000");

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

            // Total votes
            final int totalVotes = votesAndBalances.size();

            // Total votes balance
            long cumulativeVotesBalance = 0L;
            for (long voterBalance : votesAndBalances.values()) {
                cumulativeVotesBalance += voterBalance;
            }

            // Amount to be paid
            long inflationAmount;
            try {
                inflationAmount = Long.parseLong(inflationAmountTextField.getText());
            } catch (NumberFormatException e) {
                showError("The value specified is not valid, min: " + Long.MIN_VALUE + " max: " + Long.MAX_VALUE);

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
                votesAndPayments.put(voter.getKey(), computeVoterPayout(inflationAmount, cumulativeVotesBalance, voterBalance));
            }

            // Generate UUID
            final String uuid = UUID.randomUUID().toString();

            // Prepare transaction plan JSON tree
            final ObjectNode transactionPlanRootNode = OBJECT_MAPPER.createObjectNode();
            final ArrayNode transactionPlanEntriesNode = OBJECT_MAPPER.createArrayNode();

            // Compute plan entries
            votesAndPayments.forEach((voterAccount, voterAmount) -> {
                // Create new entry node
                final ObjectNode transactionPlanEntry = OBJECT_MAPPER.createObjectNode();
                transactionPlanEntry.put("account", voterAccount);
                transactionPlanEntry.put("amount", voterAmount);

                // Append to the entries
                transactionPlanEntriesNode.add(transactionPlanEntry);
            });

            // Append uuid and entries to the plan
            transactionPlanRootNode.put("uuid", uuid);
            transactionPlanRootNode.set("entries", transactionPlanEntriesNode);

            // Update the transaction plan
            transactionPlan = transactionPlanRootNode;

            // success
            return true;
        }
    }

    /**
     * Compute the amount of inflation this account should be sent
     *
     * @param totalInflation
     * @param cumulativeVotersBalance
     * @param voterBalance
     * @return
     */
    private long computeVoterPayout(final long totalInflation, final long cumulativeVotersBalance, final long voterBalance) {
        return 1L;
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

