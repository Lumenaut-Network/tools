package com.lumenaut.poolmanager.gateways;

import com.lumenaut.poolmanager.DataFormats;
import com.lumenaut.poolmanager.DataFormats.*;
import com.lumenaut.poolmanager.Settings;
import com.lumenaut.poolmanager.XLMUtils;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.stellar.sdk.*;
import org.stellar.sdk.Transaction.Builder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.SubmitTransactionTimeoutResponseException;
import org.stellar.sdk.responses.SubmitTransactionUnknownResponseException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lumenaut.poolmanager.Settings.*;

/**
 * @Author Luca Vignaroli, Ludovic Fastr�
 * @Email luca@burning.it, ludovic.fastre@yahoo.fr
 * @Date 12/01/2018 - 3:40 PM
 * <p>
 * Stateless class, exposes quick methods to accomplish operations on the stellar network currently selected in the
 * application (LIVE/TEST)
 */
public class StellarGateway {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Minimum balance for which a channel is considered safe for transactions (1.5 XLM)
    public static final int MINIMUM_CHANNEL_BALANCE = 15000000;

    // How many times to ignore a failure due to "tx_bad_seq" and instead resubmit the transaction.
    // Bad sequence responses from Horizon to successfully executed transactions have been found to be common
    // during the last few months, this is currently considered bogus until the issue stops presenting itself.
    // After this amount of retries has been expended the transaction will be considered failed. The way we generate
    // sequence numbers leaves no room for sequencing issues (each payment channel regenerates the account object before
    // proceeding with the transaction, and that comes with a fresh and valid sequence number).
    public static final int MAX_RESUBMISSIONS_PER_TX_BAD_SEQ = 5;

    // When a transaction fails because of a timeout error from the horizon response
    // wait this much before resubmitting it (milliseconds). A transaction timeout will be resubmitted ad infinitum
    // until either success or failure is reported by Horizon
    public static final int TRANSACTION_RESUBMISSION_DELAY = 5000;

    // Each transaction will become invalid after the specified time since its first submission
    public static final long TRANSACTION_VALIDITY_TIMEOUT_SECONDS = 60 * 20;

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    private static ArrayList<String> channelAccounts;
    private static ArrayList<String> channelKeys;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region ACCESSORS

    public static ArrayList<String> getChannelAccounts() {
        return channelAccounts;
    }

    public static ArrayList<String> getChannelKeys() {
        return channelKeys;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    private StellarGateway() {

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
     * Initialize parallel submission data
     */
    public static void initParallelSubmission(final TextArea outputTextArea) {
        if (SETTING_PARALLEL_CHANNELS_ENABLED) {
            // Reset channels
            channelAccounts = new ArrayList<>();
            channelKeys = new ArrayList<>();

            // Import channels from JSON
            PaymentChannels channelsData;
            try {
                channelsData = DataFormats.OBJECT_MAPPER.readValue(new File("data/payment-channels.json"), PaymentChannels.class);
            } catch (IOException e) {
                Platform.runLater(() -> outputTextArea.appendText("No valid payment channels found in config. Aborting... "));

                return;
            }

            final List<PaymentChannel> channels = channelsData.getChannels();

            // Check that we're not attempting to use more channels than the number of transactions which can
            // be managed by a single ledger
            if (channels.size() > 50) {
                Platform.runLater(() -> outputTextArea.appendText("Too many channels found in config, max number of usable channels is 50. Aborting... "));

                return;
            }

            // Verify found channels
            for (int i = 0; i < channels.size(); i++) {
                final PaymentChannel channel = channels.get(i);
                final String channelAddress = channel.getAddress();
                final String channelKey = channel.getKey();
                final String channelNumber = i < 10 ? "0" + i : String.valueOf(i);
                Platform.runLater(() -> outputTextArea.appendText("Channel [" + channelNumber + "] init... "));

                // Verify channel and balance
                if (verifyChannel(channelAddress, channelKey, outputTextArea)) {
                    channelAccounts.add(channelAddress);
                    channelKeys.add(channelKey);
                }
            }

            // Check for duplicates
            boolean duplicatesFound = false;
            for (int i = 0; i < channels.size(); i++) {
                for (int j = i + 1; j < channels.size(); j++) {
                    final PaymentChannel channel = channels.get(i);
                    final PaymentChannel channel2 = channels.get(j);

                    if (channel.address.equalsIgnoreCase(channel2.address)) {
                        duplicatesFound = true;
                        break;
                    }
                }

                if (duplicatesFound) {
                    break;
                }
            }

            // Invalidate initialized channels if duplicates were found
            if (duplicatesFound) {
                channelAccounts = null;
                channelKeys = null;

                // Notify user
                Platform.runLater(() -> outputTextArea.appendText("ERROR: Duplicate payment channels found, please check your settings. Aborting...\n"));
            }
        } else {
            channelAccounts = null;
            channelKeys = null;
        }
    }

    /**
     * Verifies that the channel address and private keys are valid, and that the channel has at least 2 XLM of balance
     * to process transactions
     *
     * @param channelAddress
     * @param channelKey
     * @return
     */
    private static boolean verifyChannel(final String channelAddress, final String channelKey, final TextArea outputTextArea) {
        // Fail immediately if the channel data is invalid
        if (channelAddress == null || channelAddress.isEmpty() || channelKey == null || channelKey.isEmpty()) {
            Platform.runLater(() -> outputTextArea.appendText("FAILED [Invalid channel address/key]\n"));

            return false;
        }

        // Select the operations network
        final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? SETTING_HORIZON_LIVE_NETWORK : SETTING_HORIZON_TEST_NETWORK);

        try {
            // Create the key pair from the secret key, so we can check if it matches the channel address
            final KeyPair pair = KeyPair.fromSecretSeed(channelKey);

            final AccountResponse account = server.accounts().account(pair);

            // Check address
            if (!pair.getAccountId().equals(channelAddress)) {
                Platform.runLater(() -> outputTextArea.appendText("FAILED [Invalid key pair]\n"));

                return false;
            }

            // Fetch current balance
            BigDecimal totalBalance = new BigDecimal("0");
            for (AccountResponse.Balance balance : account.getBalances()) {
                totalBalance = totalBalance.add(new BigDecimal(balance.getBalance()));
            }

            final BigDecimal availableBalance = totalBalance;

            // Check balance
            if (XLMUtils.XLMToStroop(availableBalance) < MINIMUM_CHANNEL_BALANCE) {
                Platform.runLater(() -> outputTextArea.appendText("FAILED [Balance is insufficient: " + XLMUtils.formatBalanceFullPrecision(availableBalance) + " XLM]\n"));

                return false;
            } else {
                Platform.runLater(() -> outputTextArea.appendText("SUCCESS [Balance: " + XLMUtils.formatBalanceFullPrecision(availableBalance) + " XLM]\n"));

                return true;
            }
        } catch (Exception e) {
            Platform.runLater(() -> outputTextArea.appendText("FAILED [" + e.getMessage() + "]\n"));

            return false;
        }
    }

    /**
     * Execute the given transactions in a single batch
     *
     * @param server
     * @param source
     * @param transactionResult
     * @return
     * @throws IOException
     */
    public static TransactionBatchResponse executeSingleTransactionBatch(final Server server, final KeyPair source, final KeyPair[] signers, final TransactionResult transactionResult) {
        // Prepare response object
        final TransactionBatchResponse response = new TransactionBatchResponse();

        // Current batch retries
        final AtomicInteger retriesLeft = new AtomicInteger(MAX_RESUBMISSIONS_PER_TX_BAD_SEQ);

        // Refuse batches with more than 100 operations
        if (transactionResult.getEntries().size() > SETTING_OPERATIONS_PER_TRANSACTION_BATCH) {
            response.success = false;
            response.errorMessages.add("Refusing to execute a transaction batch with more than [" + SETTING_OPERATIONS_PER_TRANSACTION_BATCH + "] entries. The batch contains [" + transactionResult.getEntries().size() + "] entries");

            return response;
        }

        // Source must be provided
        if (source == null) {
            response.success = false;
            response.errorMessages.add("Refusing to execute the transaction batch. No sourceAccount was provided");

            return response;
        }

        // We need at least 1 sourceAccountMasterKey
        if (signers.length == 0) {
            response.success = false;
            response.errorMessages.add("Refusing to execute the transaction batch. No signers were provided");

            return response;
        }

        // Build an AccountResponse object, used to fetch sequence numbers
        final AccountResponse sourceAccountResponse;
        try {
            sourceAccountResponse = server.accounts().account(source);
        } catch (IOException e) {
            response.success = false;
            response.errorMessages.add("Unable to create AccountResponse object: " + source.getAccountId());
            response.errorMessages.add(e.getMessage());

            return response;
        }

        // Prepare a new transaction builder for the pool account
        final Builder transactionBuilder = new Transaction.Builder(sourceAccountResponse, SETTING_OPERATIONS_NETWORK.equals("LIVE") ? Network.PUBLIC : Network.TESTNET);

        // Add memo to the transaction
        transactionBuilder.addMemo(Memo.text(Settings.SETTING_MEMO));

        // Mandatory timeout settings
        try {
            transactionBuilder.setTimeout(TRANSACTION_VALIDITY_TIMEOUT_SECONDS);
        } catch (RuntimeException e) {
            response.success = false;
            response.errorMessages.add("[" + DATE_FORMATTER.format(new Date()) + "]-[ERROR] " + e.getMessage());

            return response;
        }

        // Process all entries
        for (TransactionResultEntry entry : transactionResult.getEntries()) {
            // Append operation
            // !!! IMPORTANT !!! the amount must be specified in XLM as a string in decimal format e.g. 10.0000001 -> 10 lumens, 1 stroop
            transactionBuilder.addOperation(new PaymentOperation.Builder(KeyPair.fromAccountId(entry.getDestination()), new AssetTypeNative(), XLMUtils.stroopToXLM(entry.getAmount()).toString()).build());

            // Update entry operation timestamp
            entry.setTimestamp(System.currentTimeMillis());
        }

        // Finalize the transaction
        final Transaction transaction = transactionBuilder.build();
        for (KeyPair signer : signers) {
            transaction.sign(signer);
        }

        // Submit
        SubmitTransactionResponse submissionResponse = null;
        boolean resub = false;
        while (submissionResponse == null) {
            try {
                // If we're resubmitting, give horizon some time to catch up
                if (resub) {
                    Thread.sleep(TRANSACTION_RESUBMISSION_DELAY);
                }

                // Reset resubmission flag otherwise every transaction since the first resub will be delayed by 10 seconds
                resub = false;

                // Attempt submission
                submissionResponse = server.submitTransaction(transaction);
                if (submissionResponse.isSuccess()) {
                    ////////////////////////////////////////////////////////////////////////////////////////////////////
                    // SUCCESS
                    response.success = true;
                    response.transactionResponse = submissionResponse;
                } else {
                    ////////////////////////////////////////////////////////////////////////////////////////////////////
                    // RESUB on failure due to tx_bad_seq >>> up to MAX_RESUBMISSIONS_PER_TX_BAD_SEQ <<<
                    if (isTxBadSeq(submissionResponse)) {
                        if (retriesLeft.decrementAndGet() >= 0) {
                            // Append to response
                            final String warn = "Resubmitting transaction in " + TRANSACTION_RESUBMISSION_DELAY / 1000 + " seconds because of: possibly bogus tx_bad_seq";

                            // Log to console
                            System.err.println("[WARNING] " + warn);

                            // Append to response
                            response.warningMessages.add(warn);

                            // Flag for resubmission
                            resub = true;

                            // Null the current response, so we remain on this transaction
                            submissionResponse = null;
                        } else {
                            ////////////////////////////////////////////////////////////////////////////////////////////
                            // FAIL
                            response.success = false;
                            response.transactionResponse = submissionResponse;
                            response.errorMessages.add("Transaction failed after max resubmissions attempts");
                        }
                    } else {
                        ////////////////////////////////////////////////////////////////////////////////////////////////
                        // FAIL
                        response.success = false;
                        response.transactionResponse = submissionResponse;
                        response.errorMessages.add("Transaction failed");
                    }
                }
            } catch (InterruptedException e) {
                // Transaction batch failed
                response.success = false;
                response.errorMessages.add("Channel Thread was interrupted: " + e.getMessage());

                return response;
            } catch (SubmitTransactionUnknownResponseException e) {
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // RESUB >>> Unexpected failure (timeout?)
                final String warn = "Resubmitting transaction in " + TRANSACTION_RESUBMISSION_DELAY / 1000 + ". Code: " + e.getCode() + ", Response Body" + e.getBody();

                // Append to response
                response.warningMessages.add(warn);

                // Flag as resubmission
                resub = true;
            } catch (SubmitTransactionTimeoutResponseException | IOException e) {
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // RESUB >>>  Unexpected failure (timeout?)
                final String warn = "Resubmitting transaction in " + TRANSACTION_RESUBMISSION_DELAY / 1000 + " seconds because of: " + e.getClass().getSimpleName() + " -> " + e.getMessage();

                // Append to response
                response.warningMessages.add(warn);

                // Flag as resubmission
                resub = true;
            }
        }

        return response;
    }

    /**
     * Execute the given transactions in a single batch from the specified channel
     *
     * @param server
     * @param channelAccount
     * @param signers
     * @param batch
     * @return
     * @throws IOException
     */
    public static TransactionBatchResponse executeParallelTransactionBatch(final Server server, final KeyPair channelAccount, final KeyPair sourceAccount, final KeyPair[] signers, final TransactionResult batch, final AtomicBoolean idleFlag, final int channelIndex) {
        // Prepare response object
        final TransactionBatchResponse response = new TransactionBatchResponse();

        // Current batch retries
        final AtomicInteger retriesLeft = new AtomicInteger(MAX_RESUBMISSIONS_PER_TX_BAD_SEQ);

        // Refuse batches with more than 100 operations
        if (batch.getEntries().size() > SETTING_OPERATIONS_PER_TRANSACTION_BATCH) {
            response.success = false;
            response.errorMessages.add("Refusing to execute a transaction batch with more than [" + SETTING_OPERATIONS_PER_TRANSACTION_BATCH + "] entries. The batch contains [" + batch.getEntries().size() + "] entries");

            return response;
        }

        // Source must be provided
        if (sourceAccount == null) {
            response.success = false;
            response.errorMessages.add("Refusing to execute the transaction batch. No sourceAccount was provided");

            return response;
        }

        // We need at least 1 sourceAccountMasterKey
        if (signers.length == 0) {
            response.success = false;
            response.errorMessages.add("Refusing to execute the transaction batch. No signers were provided");

            return response;
        }

        // Build an AccountResponse object for the channel, used to fetch sequence numbers
        final AccountResponse channelAccountResponse;
        try {
            channelAccountResponse = server.accounts().account(channelAccount);
        } catch (IOException e) {
            response.success = false;
            response.errorMessages.add("Unable to create AccountResponse object for the channel account: " + channelAccount.getAccountId());
            response.errorMessages.add(e.getMessage());

            return response;
        }

        // Prepare a new transaction builder for the channel
        final Builder transactionBuilder = new Transaction.Builder(channelAccountResponse, SETTING_OPERATIONS_NETWORK.equals("LIVE") ? Network.PUBLIC : Network.TESTNET);

        // Add memo to the transaction
        transactionBuilder.addMemo(Memo.text(Settings.SETTING_MEMO));

        // Mandatory settings
        try {
            transactionBuilder.setTimeout(TRANSACTION_VALIDITY_TIMEOUT_SECONDS);
            transactionBuilder.setOperationFee(SETTING_FEE);
        } catch (RuntimeException e) {
            response.success = false;
            response.errorMessages.add("[" + DATE_FORMATTER.format(new Date()) + "]-[ERROR] " + e.getMessage());

            return response;
        }

        // Process all entries
        for (TransactionResultEntry entry : batch.getEntries()) {
            // Append operation
            // !!! IMPORTANT !!! the amount must be specified in XLM as a string in decimal format e.g. 10.0000001 -> 10 lumens, 1 stroop
            transactionBuilder.addOperation(new PaymentOperation.Builder(KeyPair.fromAccountId(entry.getDestination()), new AssetTypeNative(), XLMUtils.stroopToXLM(entry.getAmount()).toString()).setSourceAccount(sourceAccount).build());

            // Update entry operation timestamp
            entry.setTimestamp(System.currentTimeMillis());
        }

        // Finalize the transaction
        final Transaction transaction = transactionBuilder.build();
        for (KeyPair signer : signers) {
            transaction.sign(signer);
        }

        // Submit
        SubmitTransactionResponse submissionResponse = null;
        boolean resub = false;
        while (submissionResponse == null) {
            try {
                // If we're resubmitting, give horizon some time to catch up
                if (resub) {
                    // Log to console
                    System.out.println("[" + DATE_FORMATTER.format(new Date()) + "]-[INFO] Channel [" + channelIndex + "] entering sleep mode for " + TRANSACTION_RESUBMISSION_DELAY / 1000 + " seconds");

                    idleFlag.set(true);
                    Thread.sleep(TRANSACTION_RESUBMISSION_DELAY);

                    // Log to console
                    System.out.println("[" + DATE_FORMATTER.format(new Date()) + "]-[INFO] Channel [" + channelIndex + "] resuming operations");
                }

                // Reset flags
                idleFlag.set(false);
                resub = false;

                // Attempt submission
                submissionResponse = server.submitTransaction(transaction);
                if (submissionResponse.isSuccess()) {
                    ////////////////////////////////////////////////////////////////////////////////////////////////////
                    // SUCCESS
                    response.success = true;
                    response.transactionResponse = submissionResponse;
                } else {
                    ////////////////////////////////////////////////////////////////////////////////////////////////////
                    // RESUB >>> tx_bad_seq (up to MAX_RESUBMISSIONS_PER_TX_BAD_SEQ)
                    if (isTxBadSeq(submissionResponse)) {
                        if (retriesLeft.decrementAndGet() >= 0) {
                            // Append to response
                            final String warn = "Resubmitting transaction in " + TRANSACTION_RESUBMISSION_DELAY / 1000 + " seconds because of: possibly bogus tx_bad_seq";

                            // Log to console
                            System.err.println("[" + DATE_FORMATTER.format(new Date()) + "]-[WARNING] Channel [" + channelIndex + "]: " + warn);

                            // Append to response
                            response.warningMessages.add(warn);

                            // Flag for resubmission
                            resub = true;

                            // Null the current transaction response, so we remain on this transaction
                            submissionResponse = null;
                        } else {
                            ////////////////////////////////////////////////////////////////////////////////////////////
                            // FAIL
                            response.success = false;
                            response.transactionResponse = submissionResponse;
                            response.errorMessages.add("Transaction failed: max resubmissions attempted");
                        }
                    } else {
                        ////////////////////////////////////////////////////////////////////////////////////////////////
                        // FAIL
                        response.success = false;
                        response.transactionResponse = submissionResponse;
                        response.errorMessages.add("Transaction failed");
                    }
                }
            } catch (InterruptedException e) {
                // Transaction batch failed
                response.success = false;
                response.errorMessages.add("[" + DATE_FORMATTER.format(new Date()) + "]-[ERROR] Channel [" + channelIndex + "]: Thread was interrupted: " + e.getMessage());

                return response;
            } catch (SubmitTransactionUnknownResponseException e) {
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // RESUB >>> Timeout
                final String warn = "Resubmitting transaction in " + TRANSACTION_RESUBMISSION_DELAY / 1000 + ". Code: " + e.getCode() + ", Response Body" + e.getBody();

                // Log to console
                System.err.println("[" + DATE_FORMATTER.format(new Date()) + "]-[WARNING] Channel [" + channelIndex + "]: " + warn);

                // Append to response
                response.warningMessages.add(warn);

                // Flag as resubmission
                resub = true;

                // Null the current transaction response, so we remain on this transaction
                submissionResponse = null;
            } catch (SubmitTransactionTimeoutResponseException | IOException e) {
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // RESUB >>> Timeout
                final String warn = "Resubmitting transaction in " + TRANSACTION_RESUBMISSION_DELAY / 1000 + " seconds because of: " + e.getClass().getSimpleName() + " -> " + e.getMessage();

                // Log to console
                System.err.println("[" + DATE_FORMATTER.format(new Date()) + "]-[WARNING] Channel [" + channelIndex + "]: " + warn);

                // Append to response
                response.warningMessages.add(warn);

                // Flag as resubmission
                resub = true;

                // Null the current transaction response, so we remain on this transaction
                submissionResponse = null;
            }
        }

        return response;
    }

    /**
     * Check if the specified transaction response contains a failure of type "tx_bad_seq"
     *
     * @param transactionResponse
     * @return
     */
    private static boolean isTxBadSeq(final SubmitTransactionResponse transactionResponse) {
        return transactionResponse.getExtras() != null &&
               transactionResponse.getExtras().getResultCodes() != null &&
               transactionResponse.getExtras().getResultCodes().getTransactionResultCode() != null &&
               transactionResponse.getExtras().getResultCodes().getTransactionResultCode().equals("tx_bad_seq");
    }

    /**
     * Verify all the accounts in the specified transactions plan
     *
     * @param server
     * @param transactionResult
     * @return
     */
    private static boolean verifyAccounts(final Server server, final TransactionResult transactionResult) {
        for (TransactionResultEntry entry : transactionResult.getEntries()) {
            try {
                server.accounts().account(KeyPair.fromAccountId(entry.getDestination()));
            } catch (IOException e) {
                System.out.println("Transaction batch: account failed verification: " + entry.getDestination());

                return false;
            }
        }

        System.out.println("Transaction batch: accounts verified");

        return true;
    }

    /**
     * Fetch the specified account balance
     *
     * @param address
     * @return
     */
    public static BigDecimal getBalance(final String address) throws IOException {
        // Select the operations network
        final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? SETTING_HORIZON_LIVE_NETWORK : SETTING_HORIZON_TEST_NETWORK);

        // Build a key pair for the account id specified
        final KeyPair pair = KeyPair.fromAccountId(address);

        // Retrieve balances
        final AccountResponse account = server.accounts().account(pair);

        // Sum all account balances
        BigDecimal total = new BigDecimal("0");
        for (AccountResponse.Balance balance : account.getBalances()) {
            total = total.add(new BigDecimal(balance.getBalance()));
        }

        return total;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

