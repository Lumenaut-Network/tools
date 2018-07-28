package com.lumenaut.poolmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumenaut.poolmanager.DataFormats.TransactionBatchResponse;
import com.lumenaut.poolmanager.DataFormats.TransactionResult;
import com.lumenaut.poolmanager.DataFormats.TransactionResultEntry;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;
import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.TransactionsController.*;
import static com.lumenaut.poolmanager.UIUtils.showError;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 20/07/2018 - 3:26 PM
 */
public class ParallelTransactionTask implements Runnable {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Config
    private final ParallelTransactionTaskConfig config;

    // JSON
    private final ObjectMapper mapper = new ObjectMapper();

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region SUBCLASSES

    /**
     * Config class
     */
    public static class ParallelTransactionTaskConfig {
        // Out
        public final TransactionResult finalResults;
        public AtomicLong paidTotal;
        public AtomicLong totalFees;
        public AtomicLong totalPayment;
        public AtomicLong remainingPayment;
        public String outputPath;

        // Signing
        public KeyPair sourceAccount;
        public KeyPair sourceAccountMasterKey;
        public int channelIndex;
        public String channelAccount;
        public String channelAccountKey;
        public AtomicInteger progress;
        public SpscAtomicArrayQueue<TransactionResult> batchQueue;
        public AtomicBoolean error;
        public ArrayList<String> errorMessage;

        /**
         * Constructor
         *
         * @param finalResults The results object that collects the overall operations data
         */
        public ParallelTransactionTaskConfig(final TransactionResult finalResults) {
            this.finalResults = finalResults;
        }
    }

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
    public ParallelTransactionTask(final ParallelTransactionTaskConfig config) {
        this.config = config;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region INTERFACES IMPLEMENTATIONS

    @Override
    public void run() {
        // Check if we have batches to run
        if (config.batchQueue.peek() == null) {
            // We're done, nothing to do here
            config.error.getAndSet(true);
            config.errorMessage.add("This channel had no operation batches to process");

            // Set progress to completion and exit this thread
            config.progress.getAndSet(100);

            return;
        }

        // Build server object
        final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? SETTING_HORIZON_LIVE_NETWORK : SETTING_HORIZON_TEST_NETWORK);

        // Create KeyPair for the channel
        final KeyPair channelAccount = KeyPair.fromAccountId(config.channelAccount);

        // Build an AccountResponse object for the channel (used to fetch sequence numbers, can be reused)
        final AccountResponse channelAccountResponse;
        try {
            channelAccountResponse = server.accounts().account(channelAccount);
        } catch (IOException e) {
            config.error.getAndSet(true);
            config.errorMessage.add("Unable to create AccountResponse object for the channel account: " + config.channelAccount);
            config.errorMessage.add(e.getMessage());

            // Set progress to completion and exit this task
            config.progress.getAndSet(100);

            return;
        }

        // Bundle signers for the transaction
        final KeyPair[] signers = new KeyPair[2];
        signers[0] = config.sourceAccountMasterKey;                     // Pool signature
        signers[1] = KeyPair.fromSecretSeed(config.channelAccountKey);  // Channel signature

        // Init progress counters
        final int totalBatches = config.batchQueue.size();
        int currentBatch = 0;

        // Check if we have batches to process
        while (config.batchQueue.peek() != null) {
            final TransactionResult batch = config.batchQueue.poll();
            try {
                if (batch != null) {
                    final TransactionBatchResponse batchResponse = StellarGateway.executeChannelTransactionBatch(server, channelAccountResponse, config.sourceAccount, signers, batch);
                    if (!batchResponse.success) {
                        // Append error and update error state
                        config.error.getAndSet(true);
                        config.errorMessage = batchResponse.errorMessages;

                        // Save the response
                        saveTransactionResponse(batch.getUuid(), batchResponse.transactionResponse);
                    } else {
                        // Update payment counters
                        for (TransactionResultEntry resultEntry : batch.getEntries()) {
                            config.paidTotal.getAndAdd(resultEntry.getAmount());
                            config.totalFees.getAndAdd(SETTING_FEE);
                            config.totalPayment.getAndAdd(resultEntry.getAmount() + SETTING_FEE);
                            config.remainingPayment.getAndAdd(-1 * (resultEntry.getAmount() + SETTING_FEE));
                        }

                        // Append completed batch to the final result
                        synchronized (config.finalResults) {
                            config.finalResults.getEntries().addAll(batch.getEntries());
                            config.finalResults.getExecutedOperations().getAndAdd(batch.getEntries().size());
                        }

                        // Save the response
                        saveTransactionResponse(batch.getUuid(), batchResponse.transactionResponse);
                    }
                } else {
                    // This should really NEVER happen, it means the there's a BIG issue with our queues or the way we're using them
                    Platform.runLater(() -> showError("Channel [" + config.channelIndex + "] has skipped a transaction batch, the batch object fetched from the queue was NULL!"));
                }

                // Increment batch counter and update progress
                currentBatch++;

                // Update progress
                config.progress.getAndSet(currentBatch * 100 / totalBatches);
            } catch (IOException e) {
                // Append error and update error state
                config.error.getAndSet(true);
                config.errorMessage.add(e.getMessage());

                // This batch failed but we need to update progress anyway
                currentBatch++;

                // Update progress
                config.progress.getAndSet(currentBatch * 100 / totalBatches);
            }
        }

        // The thread has finished processing all batches, make sure the progress reports completion
        config.progress.getAndSet(100);
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Save the complete state of a transaction response
     *
     * @param transactionResponse
     */
    private void saveTransactionResponse(final String uuid, final SubmitTransactionResponse transactionResponse) {
        // Read the current contents of the text area
        if (transactionResponse == null) {
            return;
        }

        // Gather data
        String envelopeXdr = transactionResponse.getEnvelopeXdr();
        String resultXdr = transactionResponse.getResultXdr();
        Long ledger = transactionResponse.getLedger();

        // Create JSON structure
        final ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("ledger", ledger);
        rootNode.put("envelopeXdr", envelopeXdr);
        rootNode.put("resultXdr", resultXdr);

        if (!transactionResponse.isSuccess()) {
            // Append transaction result code
            rootNode.put("transactionResultCode", transactionResponse.getExtras().getResultCodes().getTransactionResultCode());

            // Append operations result codes
            final ArrayList<String> operationsResultCodes = transactionResponse.getExtras().getResultCodes().getOperationsResultCodes();

            if (operationsResultCodes != null) {
                final ArrayNode operationsResults = mapper.createArrayNode();
                for (String operationResultCode : operationsResultCodes) {
                    operationsResults.add(operationResultCode);
                }
                rootNode.set("operationsResultCodes", operationsResults);
            }
        }

        // Create folder if missing
        final String destinationFolder = config.outputPath + "/transactions";
        final String destinationFileName = FILE_DATE_FORMATTER.format(new Date()) + "_" + UUID.randomUUID().toString() + "_" + (transactionResponse.isSuccess() ? TRANSACTION_SUCCESSFUL_JSON_SUFFIX : TRANSACTION_ERROR_JSON_SUFFIX);
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
                bufWriter.write(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
            } catch (IOException e) {
                showError("Cannot write transaction result file [" + outPutFilePath + "]: " + e.getMessage());
            }
        }

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

