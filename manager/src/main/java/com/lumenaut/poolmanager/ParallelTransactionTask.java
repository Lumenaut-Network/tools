package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.TransactionBatchResponse;
import com.lumenaut.poolmanager.DataFormats.TransactionResult;
import com.lumenaut.poolmanager.gateways.StellarGateway;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.lumenaut.poolmanager.Settings.HORIZON_LIVE_NETWORK;
import static com.lumenaut.poolmanager.Settings.HORIZON_TEST_NETWORK;
import static com.lumenaut.poolmanager.Settings.SETTING_OPERATIONS_NETWORK;

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

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region SUBCLASSES

    /**
     * Config class
     */
    public static class ParallelTransactionTaskConfig {
        // Out
        public TransactionResult finalResults;
        public AtomicLong paidTotal;
        public AtomicLong totalFees;
        public AtomicLong totalPayment;
        public AtomicLong remainingPayment;

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
        final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);

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

            // Bail on null or empty batch
            if (batch == null || batch.getEntries().size() == 0) {
                // This batch was empty but we need to update progress anyway
                currentBatch++;

                // Update progress
                config.progress.getAndSet(Math.round(currentBatch * 100 / totalBatches));

                continue;
            }

            try {
                final TransactionBatchResponse batchResponse = StellarGateway.executeChannelTransactionBatch(server, config.sourceAccount, channelAccount, signers, batch);
                if (!batchResponse.success) {
                    // Append error and update error state
                    config.error.getAndSet(true);
                    config.errorMessage = batchResponse.errorMessages;
                }

                // Increment batch counter and update progress
                currentBatch++;

                // Update progress
                config.progress.getAndSet(Math.round(currentBatch * 100 / totalBatches));
            } catch (IOException e) {
                // Append error and update error state
                config.error.getAndSet(true);
                config.errorMessage.add(e.getMessage());

                // This batch failed but we need to update progress anyway
                currentBatch++;

                // Update progress
                config.progress.getAndSet(Math.round(currentBatch * 100 / totalBatches));
            }
        }

        // The thread has finished processing all batches, make sure the progress reports completion
        config.progress.getAndSet(100);
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHOD OVERRIDES

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

