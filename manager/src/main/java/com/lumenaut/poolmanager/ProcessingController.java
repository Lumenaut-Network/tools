package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.*;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import static com.lumenaut.poolmanager.Settings.*;

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
    public AnchorPane primaryStage;

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

        // !!!TEMPORARY!!! will only be enabled when the operation completes
        closeBtn.setDisable(false);

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
            final CompletableFuture<Boolean> processing = CompletableFuture.supplyAsync(() -> {
                if (transactionPlan != null) {
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
                    Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);

                    // Create KeyPair from the signing key
                    KeyPair source = KeyPair.fromSecretSeed(signingKey);

                    // Build overall result
                    final TransactionResult transactionResult = new TransactionResult();
                    transactionResult.setEntries(new LinkedList<>());
                    transactionResult.setUuid(transactionPlan.getUuid());

                    // Build tmp batch buffer
                    final TransactionResult tmpBatchResult = new TransactionResult();
                    tmpBatchResult.setEntries(new LinkedList<>());

                    ////////////////////////////////////////////////////////////////////////////////////////////////////
                    // BATCH ENTRIES

                    // Init progress tracking
                    final int totalEntries = transactionPlan.getEntries().size();
                    final int totalBatches = totalEntries / OPERATIONS_PER_TRANSACTION_BATCH + (totalEntries % OPERATIONS_PER_TRANSACTION_BATCH > 0 ? 1 : 0);

                    // Update progress bar
                    updateProgressBar(totalEntries, 0);

                    // Start processing
                    int processedEntries = 0;
                    int operationsCount = 0;
                    int batchCount = 0;
                    for (TransactionPlanEntry entry : transactionPlan.getEntries()) {
                        if (operationsCount < OPERATIONS_PER_TRANSACTION_BATCH) {
                            // Create new entry for the temporary result (which we're using as a buffer for batches)
                            final TransactionResultEntry transactionResultEntry = new TransactionResultEntry();
                            transactionResultEntry.setDestination(entry.getDestination());
                            transactionResultEntry.setAmount(entry.getAmount());

                            // Append to the temporary buffer
                            tmpBatchResult.getEntries().add(transactionResultEntry);

                            // Another one bites the dust..
                            operationsCount++;
                        } else {
                            // The batch is full, time to execute
                            try {
                                final TransactionBatchResponse batchResponse = StellarGateway.executeTransactionBatch(server, source, tmpBatchResult);
                                if (batchResponse.success) {
                                    // Append completed batch to the final result
                                    transactionResult.getEntries().addAll(tmpBatchResult.getEntries());

                                    // Clear the tmp buffer
                                    tmpBatchResult.getEntries().clear();

                                    // Update counters
                                    processedEntries += operationsCount;
                                    operationsCount = 0;
                                    batchCount++;

                                    // Update progress bar
                                    updateProgressBar(totalEntries, processedEntries);

                                    // Append progress
                                    appendMessage("Batch [" + batchCount + " of " + totalBatches + "] Submitted successfully");
                                } else {
                                    // Push out the error to the console
                                    printBatchError(batchResponse);

                                    return false;
                                }
                            } catch (IOException e) {
                                printExceptionError(e);
                            }
                        }
                    }

                    // Process last batch, if it has any entries
                    if (!tmpBatchResult.getEntries().isEmpty()) {
                        try {
                            final TransactionBatchResponse batchResponse = StellarGateway.executeTransactionBatch(server, source, tmpBatchResult);
                            if (batchResponse.success) {
                                // Add to the progress tracker
                                processedEntries += operationsCount;
                                batchCount++;

                                // Update progress bar
                                updateProgressBar(totalEntries, processedEntries);

                                // Append progress
                                appendMessage("Batch [" + batchCount + " of " + totalBatches + "] Submitted successfully");
                            } else {
                                // Push out the error to the console
                                printBatchError(batchResponse);

                                return false;
                            }
                        } catch (IOException e) {
                            printExceptionError(e);
                        }
                    }

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
                    processingOutputTextArea.appendText("[FINISHED] Process completed successfully\n");
                } else {
                    processingOutputTextArea.appendText("[FINISHED] Process finished with ERRORS\n\n");
                }

                scrollToEnd();

                // Enable close button
                closeBtn.setDisable(false);
            });
        }
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
                processingOutputTextArea.appendText("Transaction result code: " + batchResponse.transactionResponse.getExtras().getResultCodes().getTransactionResultCode() + "\n");

                for (String operationResultCode : batchResponse.transactionResponse.getExtras().getResultCodes().getOperationsResultCodes()) {
                    processingOutputTextArea.appendText("Operation result code: " + operationResultCode + "\n");
                }

                processingOutputTextArea.appendText("Result XDR: " + batchResponse.transactionResponse.getResultXdr() + "\n");
                processingOutputTextArea.appendText("Envelop XDR: " + batchResponse.transactionResponse.getEnvelopeXdr() + "\n");
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
     * Print exception error
     *
     * @param throwable
     */
    private void printExceptionError(final Throwable throwable) {
        Platform.runLater(() -> {
            // Append custom error messages
            processingOutputTextArea.appendText(throwable.getMessage() + "\n");

            // Print our stack trace
            final StackTraceElement[] trace = throwable.getStackTrace();
            for (StackTraceElement traceElement : trace) {
                processingOutputTextArea.appendText("\tat " + traceElement + "\n");
            }

            scrollToEnd();
        });
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

