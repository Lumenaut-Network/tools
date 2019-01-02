package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.*;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.SubmitTransactionResponse.Extras.ResultCodes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.lumenaut.poolmanager.DataFormats.*;
import static com.lumenaut.poolmanager.Services.EXECUTOR;
import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.TransactionsController.FILE_DATE_FORMATTER;
import static com.lumenaut.poolmanager.TransactionsController.TRANSACTIONS_RESULT_JSON_SUFFIX;
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

    // Timing
    public String processingFolderPath;

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

            // Init start date
            processingFolderPath = transactionPlan.getOut();

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
            final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? SETTING_HORIZON_LIVE_NETWORK : SETTING_HORIZON_TEST_NETWORK);

            // Build key pairs
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
            transactionResult.setExecutedOperations(new AtomicInteger(0));
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
                            transactionResult.getExecutedOperations().getAndSet(operationsCount);

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
                    } catch (Exception e) {
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
                        transactionResult.getExecutedOperations().getAndSet(operationsCount);

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
            }

            return true;
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

    /**
     * Multi threaded transaction processing
     */
    private void parallelProcessing() {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // INIT
        final int availableChannels = StellarGateway.getChannelAccounts().size();

        // Check if we have enough channels
        if (availableChannels < 2) {
            appendMessage("[ERROR] You need to setup at least 2 valid payment channels to process parallel transactions");
        }

        // Init network to be used
        switch (SETTING_OPERATIONS_NETWORK) {
            case "LIVE":
                Network.usePublicNetwork();
                break;
            case "TEST":
                Network.useTestNetwork();
                break;
        }

        // Build server object and key pairs
        final KeyPair source;
        final KeyPair signer;
        try {
            source = KeyPair.fromAccountId(poolAddress);
            signer = KeyPair.fromSecretSeed(signingKey);
        } catch (Throwable e) {
            appendMessage("[ERROR] invalid account ID or signing keys: " + signingKey);

            return;
        }

        // Build overall result
        final TransactionResult finalResults = new TransactionResult();
        finalResults.setExecutedOperations(new AtomicInteger(0));
        finalResults.setEntries(new LinkedList<>());
        finalResults.setUuid(transactionPlan.getUuid());

        // Operations counters
        final int totalEntries = transactionPlan.getEntries().size();
        final int totalBatches = totalEntries / SETTING_OPERATIONS_PER_TRANSACTION_BATCH + (totalEntries % SETTING_OPERATIONS_PER_TRANSACTION_BATCH > 0 ? 1 : 0);
        final int maxBatchesPerChannel = totalBatches / availableChannels + (totalBatches % availableChannels > 0 ? 1 : 0);

        // Payment counters
        final AtomicLong paidTotal = new AtomicLong(0L);
        final AtomicLong totalFees = new AtomicLong(0L);
        final AtomicLong totalPayment = new AtomicLong(0L);
        final AtomicLong remainingPayment = new AtomicLong(transactionPlan.getTotalPayment());

        // Channels data
        final ArrayList<String> channelAccounts = StellarGateway.getChannelAccounts();
        final ArrayList<String> channelKeys = StellarGateway.getChannelKeys();

        // Channels queues init
        final SpscAtomicArrayQueue<TransactionResult>[] channelsQueues = new SpscAtomicArrayQueue[availableChannels];
        for (int i = 0; i < availableChannels; i++) {
            channelsQueues[i] = new SpscAtomicArrayQueue<>(maxBatchesPerChannel);
        }

        // Channels progress init
        final AtomicInteger[] channelsProgress = new AtomicInteger[availableChannels];
        for (int i = 0; i < availableChannels; i++) {
            channelsProgress[i] = new AtomicInteger(0);
        }

        // Channels errors init
        final AtomicBoolean[] channelsErrors = new AtomicBoolean[availableChannels];
        for (int i = 0; i < availableChannels; i++) {
            channelsErrors[i] = new AtomicBoolean(false);
        }

        // Channel idleFlag state init
        final AtomicBoolean[] channelsIdle = new AtomicBoolean[availableChannels];
        for (int i = 0; i < availableChannels; i++) {
            channelsIdle[i] = new AtomicBoolean(false);
        }

        // Empty channels list
        final ArrayList<Integer> emptyChannels = new ArrayList<>();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // BATCH ENTRIES AND FILL CHANNELS QUEUES

        // Update result total entries planned
        finalResults.setPlannedOperations(totalEntries);

        // Update progress bar
        updateProgressBar(totalEntries, 0);

        // Build temporary transaction result (batch) buffer
        final TransactionResult tmpBatchBuffer = new TransactionResult();
        tmpBatchBuffer.setEntries(new LinkedList<>());

        // Start processing
        final AtomicInteger currentChannelIndex = new AtomicInteger(0);
        final AtomicInteger operationsCount = new AtomicInteger(0);
        for (TransactionPlanEntry entry : transactionPlan.getEntries()) {
            // Create new entry for the temporary result (which we're using as a buffer for batches)
            final TransactionResultEntry transactionResultEntry = new TransactionResultEntry();
            transactionResultEntry.setDestination(entry.getDestination());
            transactionResultEntry.setRecordedBalance(entry.getRecordedBalance());
            transactionResultEntry.setAmount(entry.getAmount());
            transactionResultEntry.setDonation(entry.getDonation());

            // Append to the temporary buffer
            tmpBatchBuffer.getEntries().add(transactionResultEntry);

            // Another one bites the dust..
            operationsCount.getAndIncrement();

            // If the batch is full, execute it
            if (operationsCount.get() % SETTING_OPERATIONS_PER_TRANSACTION_BATCH == 0) {
                // Create channel batch
                final TransactionResult channelBatchResult = new TransactionResult();

                // Initialize the executed operations counter (unused), just in case we need to serialize this structure
                channelBatchResult.setExecutedOperations(new AtomicInteger(0));
                channelBatchResult.setEntries(new LinkedList<>());

                // Copy the entries from the tmp batch buffer
                tmpBatchBuffer.getEntries().forEach(transactionEntry -> channelBatchResult.getEntries().add(transactionEntry));

                // Append to the next available channel
                channelsQueues[currentChannelIndex.getAndIncrement()].offer(channelBatchResult);

                // Loop back if we moved past the last channel
                if (currentChannelIndex.get() == availableChannels) {
                    currentChannelIndex.getAndSet(0);
                }

                // Start new tmp batch
                tmpBatchBuffer.getEntries().clear();
            }
        }

        // If we have leftovers, append them to one last batch
        if (tmpBatchBuffer.getEntries().size() > 0) {
            // Create channel batch
            final TransactionResult channelBatchResult = new TransactionResult();

            // Initialize the executed operations counter (unused), just in case we need to serialize this structure)
            channelBatchResult.setExecutedOperations(new AtomicInteger(0));
            channelBatchResult.setEntries(new LinkedList<>());

            // Copy the entries from the tmp batch buffer
            tmpBatchBuffer.getEntries().forEach(transactionEntry -> channelBatchResult.getEntries().add(transactionEntry));

            // Append to the current channel if it has enough space
            channelsQueues[currentChannelIndex.get()].offer(channelBatchResult);

            // Final cleanup
            tmpBatchBuffer.getEntries().clear();
        }

        // Start time profiling
        final long startTime = System.currentTimeMillis();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // CREATE TASKS FOR EACH CHANNEL AND EXECUTE THEM
        for (int i = 0; i < availableChannels; i++) {
            // Skip channels that have no work to do
            if (channelsQueues[i] != null && channelsQueues[i].size() > 0) {
                // Configure task
                final ParallelTransactionTaskConfig config = new ParallelTransactionTaskConfig(finalResults);
                config.paidTotal = paidTotal;
                config.totalFees = totalFees;
                config.totalPayment = totalPayment;
                config.remainingPayment = remainingPayment;
                config.sourceAccount = source;
                config.sourceAccountMasterKey = signer;
                config.channelIndex = i;
                config.channelAccount = channelAccounts.get(i);
                config.channelAccountKey = channelKeys.get(i);
                config.progress = channelsProgress[i];
                config.errorFlag = channelsErrors[i];
                config.idleFlag = channelsIdle[i];
                config.errorMessages = new ArrayList<>();
                config.batchQueue = channelsQueues[i];
                config.outputPath = processingFolderPath;

                // Create and run
                final ParallelTransactionTask task = new ParallelTransactionTask(config);
                EXECUTOR.execute(task);
            } else {
                // Append to empty list
                emptyChannels.add(i);

                // Set as completed
                channelsProgress[i].getAndSet(100);
            }
        }

        // Monitor the progress in a dedicated thread
        new Thread(() -> {
            boolean processing = true;

            while (processing) {
                // Check for completion
                boolean completed = true;
                for (int i = 0; i < availableChannels; i++) {
                    if (channelsProgress[i].get() != 100) {
                        completed = false;
                    }
                }

                // Update progress
                Platform.runLater(() -> {
                    // Refresh progress state
                    processingOutputTextArea.clear();

                    // Header
                    processingOutputTextArea.appendText("CHANNEL[#] | BATCH QUEUE | CHANNEL PROGRESS | CHANNEL PROGRESS % | STATUS\n\n");

                    // Update each channel
                    for (int i = 0; i < availableChannels; i++) {
                        final int currentProgress = channelsProgress[i].get();
                        final boolean error = channelsErrors[i].get();
                        final SpscAtomicArrayQueue<TransactionResult> channelQueue = channelsQueues[i];

                        // Channel status row start
                        processingOutputTextArea.appendText("Channel[" + (i < 10 ? "0" : "") + i + "] [" + (channelQueue.size() < 10 ? "0" : "") + channelQueue.size() + "] [");

                        if (emptyChannels.contains(i)) {
                            // Empty channel, fill the progress bar
                            for (int j = 0; j < 100; j++) {
                                processingOutputTextArea.appendText("-");
                            }

                            // State
                            processingOutputTextArea.appendText("][ " + (currentProgress < 10 ? "0" : "") + currentProgress + "%] [UNUSED]\n");
                        } else if (channelsIdle[i].get()) {
                            // Completed
                            for (int j = 0; j < currentProgress; j++) {
                                processingOutputTextArea.appendText("#");
                            }

                            // Remaining
                            for (int j = 0; j < 100 - currentProgress; j++) {
                                processingOutputTextArea.appendText(".");
                            }

                            // State
                            processingOutputTextArea.appendText("] [" + (currentProgress < 10 ? "0" : "") + currentProgress + "%] [WAITING]" + (error ? "[ERRORS]" : "") + "\n");
                        } else if (channelsProgress[i].get() < 100) {
                            // Completed
                            for (int j = 0; j < currentProgress; j++) {
                                processingOutputTextArea.appendText("#");
                            }

                            // Remaining
                            for (int j = 0; j < 100 - currentProgress; j++) {
                                processingOutputTextArea.appendText(".");
                            }

                            // State
                            processingOutputTextArea.appendText("] [" + (currentProgress < 10 ? "0" : "") + currentProgress + "%] [PROCESSING]" + (error ? "[ERRORS]" : "") + "\n");
                        } else {
                            // Completed
                            for (int j = 0; j < currentProgress; j++) {
                                processingOutputTextArea.appendText("#");
                            }

                            // State
                            processingOutputTextArea.appendText("] [" + (currentProgress < 10 ? "0" : "") + currentProgress + "%] [COMPLETED]" + (error ? "[ERRORS]" : "") + "\n");
                        }
                    }
                });

                // Exit if we completed
                if (completed) {
                    // Flag completion, this thread will exit
                    processing = false;

                    // Execution time
                    final long stopTime = System.currentTimeMillis();
                    final long elapsedTime = stopTime - startTime;

                    // Update the paid label in the transaction planner
                    final int totalTransactionsPaid = operationsCount.get();
                    Platform.runLater(() -> executedTransactionsLabel.setText(String.valueOf(totalTransactionsPaid)));

                    // Save the transaction results
                    long elapsedHours = TimeUnit.MILLISECONDS.toHours(elapsedTime) % 24;
                    long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60;
                    long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60;

                    synchronized (finalResults) {
                        // Update result outcome
                        if (operationsCount.get() == totalEntries && remainingPayment.get() == 0) {
                            finalResults.setResultOutcome("SUCCESSFULLY EXECUTED in " + elapsedHours + "h " + elapsedMinutes + "m " + elapsedSeconds + "s");
                        } else {
                            finalResults.setResultOutcome("PARTIALLY EXECUTED in " + elapsedHours + "h " + elapsedMinutes + "m " + elapsedSeconds + "s");
                        }

                        saveTransactionResult(finalResults, paidTotal.get(), totalFees.get(), totalPayment.get(), remainingPayment.get(), false);
                    }

                    // Check if errors occurred during the transaction process
                    boolean errorsOccurred = false;
                    for (int i = 0; i < availableChannels; i++) {
                        if (channelsErrors[i].get()) {
                            errorsOccurred = true;
                        }
                    }

                    // Status update
                    if (!errorsOccurred) {
                        Platform.runLater(() -> {
                            // Append final message
                            appendMessage("\n[FINISHED] Process completed in (" + elapsedHours + "h " + elapsedMinutes + "m " + elapsedSeconds + "s )\n");

                            // Fill the progress bar and colorize it
                            processingProgressBar.setProgress(1);
                            processingProgressBar.getStyleClass().removeAll();
                            processingProgressBar.getStyleClass().add("green-bar");

                            // Update transaction planner UI
                            executeTransactionBtn.setText("EXECUTED");
                            executeTransactionBtn.setDisable(true);
                            rebuildTransactionPlanBtn.setDisable(true);

                            // Enable close button
                            closeBtn.setDisable(false);
                        });
                    } else {
                        Platform.runLater(() -> {
                            // Append final message
                            appendMessage("\n[FINISHED] Process completed with some errors in (" + elapsedHours + "h " + elapsedMinutes + "m " + elapsedSeconds + "s )\n");

                            // Fill the progress bar and colorize it
                            processingProgressBar.setProgress(1);
                            processingProgressBar.getStyleClass().removeAll();
                            processingProgressBar.getStyleClass().add("red-bar");

                            executeTransactionBtn.setText("EXECUTED WITH ERRORS");
                            executeTransactionBtn.setTooltip(new Tooltip("The transaction executed with errors, you might want to use the transaction results as an exclusions list and build a new transaction plan!"));
                            executeTransactionBtn.setDisable(true);
                            rebuildTransactionPlanBtn.setDisable(false);

                            // Enable close button
                            closeBtn.setDisable(false);
                        });
                    }
                } else {
                    // Defer new update in a second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
        final String destinationFolder = processingFolderPath;
        final String destinationFileName = FILE_DATE_FORMATTER.format(new Date()) + "_" + result.getUuid() + "_" + TRANSACTIONS_RESULT_JSON_SUFFIX;
        final File destinationDir = new File(destinationFolder);
        boolean destinationReady;
        if (!destinationDir.exists()) {
            destinationReady = destinationDir.mkdirs();
        } else {
            destinationReady = true;
        }

        // Save to file
        if (destinationReady) {
            // Save to file
            final String outPutFilePath = destinationFolder + "/" + destinationFileName;
            try (
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outPutFilePath), StandardCharsets.UTF_8);
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

