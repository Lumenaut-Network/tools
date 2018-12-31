package com.lumenaut.poolmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumenaut.poolmanager.DataFormats.TransactionBatchResponse;
import com.lumenaut.poolmanager.DataFormats.TransactionResult;
import com.lumenaut.poolmanager.DataFormats.TransactionResultEntry;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import javafx.application.Platform;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

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
            config.errorFlag.getAndSet(true);
            config.errorMessages.add("This channel had no operation batches to process");

            // Set progress to completion and exit this thread
            config.progress.getAndSet(100);

            return;
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

        // Build server object
        final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? SETTING_HORIZON_LIVE_NETWORK : SETTING_HORIZON_TEST_NETWORK);

        // Create KeyPair for the channel
        final KeyPair channelAccount = KeyPair.fromAccountId(config.channelAccount);

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
                    final TransactionBatchResponse batchResponse = StellarGateway.executeChannelTransactionBatch(server, channelAccount, config.sourceAccount, signers, batch, config.idleFlag);
                    if (!batchResponse.success) {
                        // Append error and update error state
                        config.errorFlag.getAndSet(true);
                        config.errorMessages = batchResponse.errorMessages;
                        config.warningMessages = batchResponse.warningMessages;
                    } else {
                        // Append warnings (if any)
                        config.warningMessages = batchResponse.warningMessages;

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
                    }

                    // Save the response
                    saveTransactionResponse(batchResponse);
                } else {
                    // This should really NEVER happen, it means the there's a BIG issue with our queues or the way we're using them
                    Platform.runLater(() -> showError("Channel [" + config.channelIndex + "] has skipped a transaction batch, the batch object fetched from the queue was NULL!"));
                }

                // Increment batch counter and update progress
                currentBatch++;

                // Update progress
                config.progress.getAndSet(currentBatch * 100 / totalBatches);
            } catch (Throwable e) {
                // Append error and update error state
                config.errorFlag.getAndSet(true);
                config.errorMessages.add(e.getMessage());

                // This batch failed but we need to update progress anyway
                currentBatch++;

                // Update progress
                config.progress.getAndSet(currentBatch * 100 / totalBatches);

                // Create fake transaction response so we can save it
                final TransactionBatchResponse batchResponse = new TransactionBatchResponse();
                batchResponse.success = false;
                batchResponse.errorMessages.add(e.getMessage());

                // Save the response
                saveTransactionResponse(batchResponse);
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
     * Save the complete state of a transaction response.
     *
     * @param batchResponse
     */
    private void saveTransactionResponse(final TransactionBatchResponse batchResponse) {
        // Create JSON structure
        final ObjectNode rootNode = mapper.createObjectNode();

        // Append batch result state
        rootNode.put("success", batchResponse.success);

        // Populate errors result, if any are present
        final ArrayNode errorMessages = mapper.createArrayNode();
        if (batchResponse.errorMessages != null) {
            for (String errorMessage : batchResponse.errorMessages) {
                errorMessages.add(errorMessage);
            }
        }
        rootNode.set("errorMessages", errorMessages);

        // Populate warnings result, if any are present
        final ArrayNode warningMessages = mapper.createArrayNode();
        if (batchResponse.warningMessages != null) {
            for (String warningMessage : batchResponse.warningMessages) {
                warningMessages.add(warningMessage);
            }
        }
        rootNode.set("warningMessages", warningMessages);

        // Append transaction data
        if (batchResponse.transactionResponse != null) {
            // Gather data, if available
            String envelopeXdr = null;
            String resultXdr = null;
            Long ledger = null;

            try {
                envelopeXdr = batchResponse.transactionResponse.getEnvelopeXdr();
                resultXdr = batchResponse.transactionResponse.getResultXdr();
                ledger = batchResponse.transactionResponse.getLedger();
            } catch (Throwable ignored) {
            }

            rootNode.put("ledger", ledger);
            rootNode.put("envelopeXdr", envelopeXdr);
            rootNode.put("resultXdr", resultXdr);

            // If the transaction failed try to get the extra data for it
            // !!! IMPORTANT !!! Any of the response data fields can be null, attempt secure extraction
            if (!batchResponse.transactionResponse.isSuccess()) {
                String transactionResultCode = "";
                try {
                    transactionResultCode = batchResponse.transactionResponse.getExtras().getResultCodes().getTransactionResultCode();
                } catch (Throwable ignored) {
                }

                // Append transaction result code
                rootNode.put("transactionResultCode", transactionResultCode);

                // Append operations result codes
                ArrayList<String> operationsResultCodes = null;
                try {
                    operationsResultCodes = batchResponse.transactionResponse.getExtras().getResultCodes().getOperationsResultCodes();
                } catch (Throwable ignored) {

                }

                // Populate operations result codes, if any are present
                final ArrayNode operationsResults = mapper.createArrayNode();
                if (operationsResultCodes != null) {
                    for (String operationResultCode : operationsResultCodes) {
                        operationsResults.add(operationResultCode);
                    }
                }
                rootNode.set("operationsResultCodes", operationsResults);
            }
        }

        // Create folder if missing
        final String destinationFolder = config.outputPath + "/transactions";
        final String destinationFileName = FILE_DATE_FORMATTER.format(new Date()) + "_" + UUID.randomUUID().toString() + "_" + (batchResponse.success ? TRANSACTION_SUCCESSFUL_JSON_SUFFIX : TRANSACTION_ERROR_JSON_SUFFIX);
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

