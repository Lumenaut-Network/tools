package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.*;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.SubmitTransactionResponse.Extras.ResultCodes;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static com.lumenaut.poolmanager.DataFormats.*;
import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.TransactionsController.*;
import static com.lumenaut.poolmanager.UIUtils.showError;
import static com.lumenaut.poolmanager.UIUtils.showInfo;

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
    public String poolAddress;

    // References to other stages
    public AnchorPane primaryStage;
    public Button executeTransactionBtn;
    public Label executedTransactionsLabel;
    public Button rebuildTransactionPlanBtn;

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

        ////////////////////////////////////////////////////////////////
        // BUTTON HANDLERS

        startBtn.setOnAction(event -> startProcessingTransactions());
        closeBtn.setOnAction(event -> closeBtn.getScene().getWindow().hide());
    }

    /**
     * Start processing the transaction plan
     */
    private void startProcessingTransactions() {
        if (transactionPlan == null || signingKey == null || signingKey.isEmpty()) {
            // Notify
            processingOutputTextArea.setText("[ERROR] Transaction plan data or signing key are missing, please go back and rebuild the transaction plan!\n");

            // Disable start button and enable the close button
            startBtn.setDisable(true);
            closeBtn.setDisable(false);
        } else {
            processingOutputTextArea.setText("[RUNNING] Processing transactions\n");

            // Disable all buttons
            startBtn.setDisable(true);
            closeBtn.setDisable(true);

            // Start async processing
            if (!SETTING_PARALLEL_CHANNELS_ENABLED) {
                serialProcessing();
            } else {
                parallelProcessing();
            }
        }
    }

    /**
     * Single threaded transaction processing
     */
    private void serialProcessing() {
        final CompletableFuture<Boolean> processing = CompletableFuture.supplyAsync(() -> {
            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // INIT

            // Init network to be used
            switch (SETTING_OPERATIONS_NETWORK) {
                case "LIVE":
                    Network.usePublicNetwork();
                    break;
                case "TEST":
                    Network.useTestNetwork();
                    break;
            }

            // Build server object
            final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);
            final KeyPair source;
            final KeyPair[] signers = new KeyPair[1];
            try {
                source = KeyPair.fromAccountId(poolAddress);
                signers[0] = KeyPair.fromSecretSeed(signingKey);
            } catch (Throwable e) {
                appendMessage("[ERROR] invalid signing key: " + signingKey);

                return false;
            }

            // Build overall result
            final TransactionResult transactionResult = new TransactionResult();
            transactionResult.setEntries(new LinkedList<>());
            transactionResult.setUuid(transactionPlan.getUuid());

            // Build tmp batch buffer
            final TransactionResult tmpBatchResult = new TransactionResult();
            tmpBatchResult.setEntries(new LinkedList<>());

            // Payment counters
            final AtomicLong paidTotal = new AtomicLong(0L);
            final AtomicLong totalFees = new AtomicLong(0L);
            final AtomicLong totalPayment = new AtomicLong(0L);
            final AtomicLong remainingPayment = new AtomicLong(transactionPlan.getTotalPayment());

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // BATCH ENTRIES

            // Init progress tracking
            final int totalEntries = transactionPlan.getEntries().size();
            final int totalBatches = totalEntries / SETTING_OPERATIONS_PER_TRANSACTION_BATCH + (totalEntries % SETTING_OPERATIONS_PER_TRANSACTION_BATCH > 0 ? 1 : 0);

            // Update result total entries planned
            transactionResult.setPlannedOperations(totalEntries);

            // Update progress bar
            updateProgressBar(totalEntries, 0);

            // Start processing
            int operationsCount = 0;
            int batchCount = 0;
            for (TransactionPlanEntry entry : transactionPlan.getEntries()) {
                // Create new entry for the temporary result (which we're using as a buffer for batches)
                final TransactionResultEntry transactionResultEntry = new TransactionResultEntry();
                transactionResultEntry.setDestination(entry.getDestination());
                transactionResultEntry.setRecordedBalance(entry.getRecordedBalance());
                transactionResultEntry.setAmount(entry.getAmount());
                transactionResultEntry.setDonation(entry.getDonation());

                // Append to the temporary buffer
                tmpBatchResult.getEntries().add(transactionResultEntry);

                // Another one bites the dust..
                operationsCount++;

                // If the batch is full, execute it
                if (operationsCount % SETTING_OPERATIONS_PER_TRANSACTION_BATCH == 0) {
                    // The batch is full, time to execute
                    try {
                        final TransactionBatchResponse batchResponse = StellarGateway.executeTransactionBatch(server, source, signers, tmpBatchResult);
                        if (batchResponse.success) {
                            // Update payment counters
                            for (TransactionResultEntry resultEntry : tmpBatchResult.getEntries()) {
                                paidTotal.getAndAdd(resultEntry.getAmount());
                                totalFees.getAndAdd(SETTING_FEE);
                                totalPayment.getAndAdd(resultEntry.getAmount() + SETTING_FEE);
                                remainingPayment.getAndAdd(-1 * (resultEntry.getAmount() + SETTING_FEE));
                            }

                            // Append completed batch to the final result
                            transactionResult.getEntries().addAll(tmpBatchResult.getEntries());
                            transactionResult.setExecutedOperations(operationsCount);

                            // Clear the tmp buffer
                            tmpBatchResult.getEntries().clear();

                            // Update counters
                            batchCount++;

                            // Append progress
                            appendMessage("Batch [" + batchCount + " of " + totalBatches + "] of [" + operationsCount + "/" + totalEntries + "] operations: Submitted successfully");

                            // Update progress bar
                            updateProgressBar(totalEntries, operationsCount);
                        } else {
                            // Clear the tmp buffer
                            tmpBatchResult.getEntries().clear();

                            // Update result outcome
                            transactionResult.setResultOutcome(operationsCount > 0 ? "PARTIALLY EXECUTED" : "NOT EXECUTED");

                            // Push out the error to the console
                            appendMessage("Batch [" + batchCount + " of " + totalBatches + "] of [" + operationsCount + "/" + totalEntries + "] operations: FAILED");
                            printBatchError(batchResponse);

                            // Save the transaction results
                            saveTransactionResult(transactionResult, paidTotal.get(), totalFees.get(), totalPayment.get(), remainingPayment.get(), false);

                            return false;
                        }
                    } catch (IOException e) {
                        // Update batch counter, if the exception triggered it would have not been updated
                        batchCount++;

                        // Clear the tmp buffer
                        tmpBatchResult.getEntries().clear();

                        // Append progress
                        appendMessage("Batch [" + batchCount + " of " + totalBatches + "] of [" + operationsCount + "/" + totalEntries + "] operations: FAILED");

                        // Update result outcome
                        transactionResult.setResultOutcome(operationsCount > 0 ? "PARTIALLY EXECUTED" : "NOT EXECUTED");

                        // Save the transaction results
                        saveTransactionResult(transactionResult, paidTotal.get(), totalFees.get(), totalPayment.get(), remainingPayment.get(), false);

                        return false;
                    }
                }
            }

            // Process last batch, if it has any entries
            if (!tmpBatchResult.getEntries().isEmpty()) {
                try {
                    final TransactionBatchResponse batchResponse = StellarGateway.executeTransactionBatch(server, source, signers, tmpBatchResult);
                    if (batchResponse.success) {
                        // Update payment counters
                        for (TransactionResultEntry resultEntry : tmpBatchResult.getEntries()) {
                            paidTotal.getAndAdd(resultEntry.getAmount());
                            totalFees.getAndAdd(SETTING_FEE);
                            totalPayment.getAndAdd(resultEntry.getAmount() + SETTING_FEE);
                            remainingPayment.getAndAdd(-1 * (resultEntry.getAmount() + SETTING_FEE));
                        }

                        // Append completed batch to the final result
                        transactionResult.getEntries().addAll(tmpBatchResult.getEntries());
                        transactionResult.setExecutedOperations(operationsCount);

                        // Add to the progress tracker
                        batchCount++;

                        // Append progress
                        appendMessage("Batch [" + batchCount + " of " + totalBatches + "] of [" + operationsCount + "/" + totalEntries + "] operations: Submitted successfully");

                        // Update progress bar
                        updateProgressBar(totalEntries, operationsCount);
                    } else {
                        // Update result outcome
                        transactionResult.setResultOutcome(operationsCount > 0 ? "PARTIALLY EXECUTED" : "NOT EXECUTED");

                        // Push out the error to the console
                        appendMessage("Batch [" + batchCount + " of " + totalBatches + "] of [" + operationsCount + "/" + totalEntries + "] operations: FAILED");
                        printBatchError(batchResponse);

                        // Save the transaction results
                        saveTransactionResult(transactionResult, paidTotal.get(), totalFees.get(), totalPayment.get(), remainingPayment.get(), false);

                        return false;
                    }
                } catch (Exception e) {
                    // Update batch counter, if the exception triggered it would have not been updated
                    batchCount++;

                    // Update result outcome
                    transactionResult.setResultOutcome(operationsCount > 0 ? "PARTIALLY EXECUTED" : "NOT EXECUTED");

                    // Append progress
                    appendMessage("Batch [" + batchCount + " of " + totalBatches + "] of [" + operationsCount + "/" + totalEntries + "] operations: FAILED");

                    // Save the transaction results
                    saveTransactionResult(transactionResult, paidTotal.get(), totalFees.get(), totalPayment.get(), remainingPayment.get(), false);

                    return false;
                }

                // Update the paid label in the transaction planner
                final int totalTransactionsPaid = operationsCount;
                Platform.runLater(() -> executedTransactionsLabel.setText(String.valueOf(totalTransactionsPaid)));

                // Update result outcome
                if (operationsCount == totalEntries) {
                    transactionResult.setResultOutcome("SUCCESSFULLY EXECUTED");
                } else {
                    transactionResult.setResultOutcome("EXECUTION ERROR, NOT ALL OPERATIONS EXECUTED");
                }

                // Save the transaction results
                saveTransactionResult(transactionResult, paidTotal.get(), totalFees.get(), totalPayment.get(), remainingPayment.get(), false);

                // Sleep for a few ms to allow the progress bar and message to update
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }

                return true;
            } else {
                processingOutputTextArea.appendText("[ERROR] Empty transaction plan\n");
                scrollToEnd();

                return false;
            }
        });

        // Completion handler
        processing.thenAccept(success -> {
            if (success) {
                Platform.runLater(() -> {
                    // Append final message
                    appendMessage("[FINISHED] Process completed successfully\n");

                    // Fill the progress bar and colorize it
                    processingProgressBar.setProgress(1);
                    processingProgressBar.getStyleClass().removeAll();
                    processingProgressBar.getStyleClass().add("green-bar");

                    // Update transaction planner UI
                    executeTransactionBtn.setText("EXECUTED");
                    executeTransactionBtn.setDisable(true);
                    rebuildTransactionPlanBtn.setDisable(true);
                });
            } else {
                Platform.runLater(() -> {
                    // Append final message
                    appendMessage("[FINISHED] Process finished with ERRORS\n");

                    // Fill the progress bar and colorize it
                    processingProgressBar.setProgress(1);
                    processingProgressBar.getStyleClass().removeAll();
                    processingProgressBar.getStyleClass().add("red-bar");

                    executeTransactionBtn.setText("EXECUTED WITH ERRORS");
                    executeTransactionBtn.setTooltip(new Tooltip("The transaction executed with errors, you might want to use the transaction results as an exclusions list and build a new transaction plan!"));
                    executeTransactionBtn.setDisable(true);
                    rebuildTransactionPlanBtn.setDisable(false);
                });
            }

            scrollToEnd();

            // Enable close button
            closeBtn.setDisable(false);
        });
    }

    private void parallelProcessing() {

    }

    /**
     * Print batch error
     *
     * @param batchResponse
     */
    private void printBatchError(final TransactionBatchResponse batchResponse) {
        Platform.runLater(() -> {
            // Append custom error messages
            processingOutputTextArea.appendText("Transaction batch failed to execute\n");
            for (String error : batchResponse.errorMessages) {
                processingOutputTextArea.appendText(error + "\n");
            }

            // Append transaction response if available
            if (batchResponse.transactionResponse != null) {
                final ResultCodes resultCodes = batchResponse.transactionResponse.getExtras().getResultCodes();

                if (resultCodes != null) {
                    processingOutputTextArea.appendText("Transaction result code: " + resultCodes.getTransactionResultCode() + "\n");

                    for (String operationResultCode : resultCodes.getOperationsResultCodes()) {
                        processingOutputTextArea.appendText("Operation result code: " + operationResultCode + "\n");
                    }

                    processingOutputTextArea.appendText("Result XDR: " + batchResponse.transactionResponse.getResultXdr() + "\n");
                    processingOutputTextArea.appendText("Envelop XDR: " + batchResponse.transactionResponse.getEnvelopeXdr() + "\n");
                } else {
                    processingOutputTextArea.appendText("The transaction result response not provided by the system." + "\n");
                }
            }

            scrollToEnd();
        });
    }

    /**
     * Append a new message to the output
     *
     * @param message
     */
    private void appendMessage(final String message) {
        Platform.runLater(() -> {
            processingOutputTextArea.appendText(message + "\n");
            scrollToEnd();
        });
    }

    /**
     * Scroll to the end of the output
     */
    private void scrollToEnd() {
        processingOutputTextArea.positionCaret(processingOutputTextArea.getText().length());
    }

    /**
     * Update the progress bar based
     *
     * @param total
     * @param progress
     */
    private void updateProgressBar(final int total, final int progress) {
        Platform.runLater(() -> {
            int currentProgress = (progress * 100) / total;
            if (currentProgress == 0) {
                // Progress is still undefined
                currentProgress = -1;
            }

            processingProgressBar.setProgress(currentProgress / 100f);
        });
    }

    /**
     * Saves the specified transaction result
     */
    private boolean saveTransactionResult(final TransactionResult result, final long paidTotal, final long totalFees, final long totalPayment, final long remainingPayment, final boolean quietMode) {
        // Read the current contents of the text area
        if (result == null) {
            showError("Cannot save transaction result, the result is empty");

            return false;
        }

        // Update totals
        result.setTotalPayout(XLMUtils.formatBalanceFullPrecision(paidTotal) + " XLM");
        result.setRemainingPayment(XLMUtils.formatBalanceFullPrecision(remainingPayment) + " XLM");
        result.setTotalFees(XLMUtils.formatBalanceFullPrecision(totalFees) + " XLM");
        result.setTotalPayment(XLMUtils.formatBalanceFullPrecision(totalPayment) + " XLM");

        // Try to decode them to see if they are in a valid format
        final String jsonResult;
        try {
            jsonResult = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (IOException e) {
            showError("Transaction result format error: " + e.getMessage());

            return false;
        }

        // Create folder if missing
        final String destinationFolder = "data/" + FOLDER_DATE_FORMATTER.format(new Date());
        final String destinationFileName = FILE_DATE_FORMATTER.format(new Date()) + "_" + result.getUuid() + "_" + TRANSACTION_RESULT_JSON_SUFFIX;
        final File destinationDir = new File(destinationFolder);
        boolean destinationReady;
        if (!destinationDir.exists()) {
            destinationReady = destinationDir.mkdir();
        } else {
            destinationReady = true;
        }

        // Save to file
        if (destinationReady) {
            // Save to file
            final String outPutFilePath = destinationFolder + "/" + destinationFileName;
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outPutFilePath), "UTF-8");
                 BufferedWriter bufWriter = new BufferedWriter(writer)
            ) {
                bufWriter.write(jsonResult);
            } catch (IOException e) {
                showError("Cannot write transaction result file [" + outPutFilePath + "]: " + e.getMessage());

                return false;
            }

            // Show info
            if (!quietMode) {
                showInfo("The current transaction result has been saved in the following file: " + outPutFilePath);
            }
        }

        return true;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

