package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.TransactionResult;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.stellar.sdk.KeyPair;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Config class
 */
public class ParallelTransactionTaskConfig {
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

