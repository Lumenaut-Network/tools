package com.lumenaut.poolmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.UUID;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;

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
        rebuildTransactionPlanBtn.setOnAction(event -> {
            if (buildTransactionPlan()) {
                try {
                    // Update plan text area
                    transactionPlanTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(transactionPlan));

                    // Compute total payment required
                    long toBePaidTotal = 0;
                    for (long amount : votesAndPayments.values()) {
                        toBePaidTotal += amount;
                    }

                    // Round to the lumen
                    BigDecimal toBePaidFormatted = new BigDecimal(toBePaidTotal);
                    toBePaidFormatted.setScale(7, RoundingMode.CEILING);
                    toBePaidFormatted = toBePaidFormatted.divide(new BigDecimal(10000000), RoundingMode.CEILING);

                    // Update planned transactions total
                    plannedTransactionsLabel.setText(String.valueOf(votesAndBalances.size()));

                    // Update total amount to pay
                    toBePaidLabel.setText("~" + toBePaidFormatted.toString() + " XLM");
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
        if (currentPoolBalance == null || currentVotersData == null) {
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

            // Total payout
            long cumulativeVotersBalances = 0L;
            for (long voterBalance : votesAndBalances.values()) {
                cumulativeVotersBalances += voterBalance;
            }

            // Reset voters and payments data
            if (votesAndPayments == null) {
                votesAndPayments = new HashMap<>();
            } else {
                votesAndPayments.clear();
            }

            // Populate it
            for (HashMap.Entry<String, Long> voter : votesAndBalances.entrySet()) {
                votesAndPayments.put(voter.getKey(), computeVoterPayout(totalVotes, voter.getValue(), cumulativeVotersBalances));
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
     * Compute the amount this account should be sent
     *
     * @param totalVotes
     * @param voterBalance
     * @param totalAmountToPay
     * @return
     */
    private long computeVoterPayout(final int totalVotes, final long voterBalance, final long totalAmountToPay) {
        return 10000000L;
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

