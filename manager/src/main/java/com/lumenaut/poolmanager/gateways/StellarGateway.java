package com.lumenaut.poolmanager.gateways;

import com.lumenaut.poolmanager.DataFormats.TransactionBatchResponse;
import com.lumenaut.poolmanager.DataFormats.TransactionResult;
import com.lumenaut.poolmanager.DataFormats.TransactionResultEntry;
import com.lumenaut.poolmanager.Settings;
import com.lumenaut.poolmanager.XLMUtils;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.stellar.sdk.*;
import org.stellar.sdk.Transaction.Builder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import static com.lumenaut.poolmanager.Settings.*;

/**
 * @Author Luca Vignaroli, Ludovic Fastré
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

            // Channel 1
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_1 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_1.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_1 != null && !SETTING_PARALLEL_CHANNEL_KEY_1.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [1] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_1, SETTING_PARALLEL_CHANNEL_KEY_1, outputTextArea)) {
                    channelAccounts.add(0, SETTING_PARALLEL_CHANNEL_ADDRESS_1);
                    channelKeys.add(0, SETTING_PARALLEL_CHANNEL_KEY_1);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [1] not specified, skipping!\n"));
            }

            // Channel 2
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_2 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_2.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_2 != null && !SETTING_PARALLEL_CHANNEL_KEY_2.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [2] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_2, SETTING_PARALLEL_CHANNEL_KEY_2, outputTextArea)) {
                    channelAccounts.add(1, SETTING_PARALLEL_CHANNEL_ADDRESS_2);
                    channelKeys.add(1, SETTING_PARALLEL_CHANNEL_KEY_2);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [2] not specified, skipping!\n"));
            }

            // Channel 3
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_3 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_3.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_3 != null && !SETTING_PARALLEL_CHANNEL_KEY_3.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [3] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_3, SETTING_PARALLEL_CHANNEL_KEY_3, outputTextArea)) {
                    channelAccounts.add(2, SETTING_PARALLEL_CHANNEL_ADDRESS_3);
                    channelKeys.add(2, SETTING_PARALLEL_CHANNEL_KEY_3);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [3] not specified, skipping!\n"));
            }

            // Channel 4
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_4 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_4.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_4 != null && !SETTING_PARALLEL_CHANNEL_KEY_4.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [4] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_4, SETTING_PARALLEL_CHANNEL_KEY_4, outputTextArea)) {
                    channelAccounts.add(3, SETTING_PARALLEL_CHANNEL_ADDRESS_4);
                    channelKeys.add(3, SETTING_PARALLEL_CHANNEL_KEY_4);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [4] not specified, skipping!\n"));
            }

            // Channel 5
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_5 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_5.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_5 != null && !SETTING_PARALLEL_CHANNEL_KEY_5.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [5] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_5, SETTING_PARALLEL_CHANNEL_KEY_5, outputTextArea)) {
                    channelAccounts.add(4, SETTING_PARALLEL_CHANNEL_ADDRESS_5);
                    channelKeys.add(4, SETTING_PARALLEL_CHANNEL_KEY_5);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [5] not specified, skipping!\n"));
            }

            // Channel 6
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_6 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_6.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_6 != null && !SETTING_PARALLEL_CHANNEL_KEY_6.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [6] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_6, SETTING_PARALLEL_CHANNEL_KEY_6, outputTextArea)) {
                    channelAccounts.add(5, SETTING_PARALLEL_CHANNEL_ADDRESS_6);
                    channelKeys.add(5, SETTING_PARALLEL_CHANNEL_KEY_6);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [6] not specified, skipping!\n"));
            }

            // Channel 7
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_7 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_7.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_7 != null && !SETTING_PARALLEL_CHANNEL_KEY_7.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [7] init... "));

                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_7, SETTING_PARALLEL_CHANNEL_KEY_7, outputTextArea)) {
                    channelAccounts.add(6, SETTING_PARALLEL_CHANNEL_ADDRESS_7);
                    channelKeys.add(6, SETTING_PARALLEL_CHANNEL_KEY_7);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [7] not specified, skipping!\n"));
            }

            // Channel 8
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_8 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_8.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_8 != null && !SETTING_PARALLEL_CHANNEL_KEY_8.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [8] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_8, SETTING_PARALLEL_CHANNEL_KEY_8, outputTextArea)) {
                    channelAccounts.add(7, SETTING_PARALLEL_CHANNEL_ADDRESS_8);
                    channelKeys.add(7, SETTING_PARALLEL_CHANNEL_KEY_8);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [8] not specified, skipping!\n"));
            }

            // Channel 9
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_9 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_9.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_9 != null && !SETTING_PARALLEL_CHANNEL_KEY_9.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [9] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_9, SETTING_PARALLEL_CHANNEL_KEY_9, outputTextArea)) {
                    channelAccounts.add(8, SETTING_PARALLEL_CHANNEL_ADDRESS_9);
                    channelKeys.add(8, SETTING_PARALLEL_CHANNEL_KEY_9);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [9] not specified, skipping!\n"));
            }

            // Channel 10
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_10 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_10.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_10 != null && !SETTING_PARALLEL_CHANNEL_KEY_10.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [10] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_10, SETTING_PARALLEL_CHANNEL_KEY_10, outputTextArea)) {
                    channelAccounts.add(9, SETTING_PARALLEL_CHANNEL_ADDRESS_10);
                    channelKeys.add(9, SETTING_PARALLEL_CHANNEL_KEY_10);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [10] not specified, skipping!\n"));
            }

            // Channel 11
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_11 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_11.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_11 != null && !SETTING_PARALLEL_CHANNEL_KEY_11.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [11] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_11, SETTING_PARALLEL_CHANNEL_KEY_11, outputTextArea)) {
                    channelAccounts.add(10, SETTING_PARALLEL_CHANNEL_ADDRESS_11);
                    channelKeys.add(10, SETTING_PARALLEL_CHANNEL_KEY_11);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [11] not specified, skipping!\n"));
            }

            // Channel 12
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_12 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_12.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_12 != null && !SETTING_PARALLEL_CHANNEL_KEY_12.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [12] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_12, SETTING_PARALLEL_CHANNEL_KEY_12, outputTextArea)) {
                    channelAccounts.add(11, SETTING_PARALLEL_CHANNEL_ADDRESS_12);
                    channelKeys.add(11, SETTING_PARALLEL_CHANNEL_KEY_12);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [12] not specified, skipping!\n"));
            }

            // Channel 13
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_13 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_13.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_13 != null && !SETTING_PARALLEL_CHANNEL_KEY_13.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [13] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_13, SETTING_PARALLEL_CHANNEL_KEY_13, outputTextArea)) {
                    channelAccounts.add(12, SETTING_PARALLEL_CHANNEL_ADDRESS_13);
                    channelKeys.add(12, SETTING_PARALLEL_CHANNEL_KEY_13);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [13] not specified, skipping!\n"));
            }

            // Channel 14
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_14 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_14.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_14 != null && !SETTING_PARALLEL_CHANNEL_KEY_14.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [14] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_14, SETTING_PARALLEL_CHANNEL_KEY_14, outputTextArea)) {
                    channelAccounts.add(13, SETTING_PARALLEL_CHANNEL_ADDRESS_14);
                    channelKeys.add(13, SETTING_PARALLEL_CHANNEL_KEY_14);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [14] not specified, skipping!\n"));
            }

            // Channel 15
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_15 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_15.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_15 != null && !SETTING_PARALLEL_CHANNEL_KEY_15.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [15] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_15, SETTING_PARALLEL_CHANNEL_KEY_15, outputTextArea)) {
                    channelAccounts.add(14, SETTING_PARALLEL_CHANNEL_ADDRESS_15);
                    channelKeys.add(14, SETTING_PARALLEL_CHANNEL_KEY_15);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [15] not specified, skipping!\n"));
            }

            // Channel 16
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_16 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_16.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_16 != null && !SETTING_PARALLEL_CHANNEL_KEY_16.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [16] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_16, SETTING_PARALLEL_CHANNEL_KEY_16, outputTextArea)) {
                    channelAccounts.add(15, SETTING_PARALLEL_CHANNEL_ADDRESS_16);
                    channelKeys.add(15, SETTING_PARALLEL_CHANNEL_KEY_16);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [16] not specified, skipping!\n"));
            }

            // Channel 17
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_17 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_17.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_17 != null && !SETTING_PARALLEL_CHANNEL_KEY_17.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [17] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_17, SETTING_PARALLEL_CHANNEL_KEY_17, outputTextArea)) {
                    channelAccounts.add(16, SETTING_PARALLEL_CHANNEL_ADDRESS_17);
                    channelKeys.add(16, SETTING_PARALLEL_CHANNEL_KEY_17);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [17] not specified, skipping!\n"));
            }

            // Channel 18
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_18 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_18.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_18 != null && !SETTING_PARALLEL_CHANNEL_KEY_18.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [18] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_18, SETTING_PARALLEL_CHANNEL_KEY_18, outputTextArea)) {
                    channelAccounts.add(17, SETTING_PARALLEL_CHANNEL_ADDRESS_18);
                    channelKeys.add(17, SETTING_PARALLEL_CHANNEL_KEY_18);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [18] not specified, skipping!\n"));
            }

            // Channel 19
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_19 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_19.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_19 != null && !SETTING_PARALLEL_CHANNEL_KEY_19.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [19] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_19, SETTING_PARALLEL_CHANNEL_KEY_19, outputTextArea)) {
                    channelAccounts.add(18, SETTING_PARALLEL_CHANNEL_ADDRESS_19);
                    channelKeys.add(18, SETTING_PARALLEL_CHANNEL_KEY_19);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [19] not specified, skipping!\n"));
            }

            // Channel 20
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_20 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_20.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_20 != null && !SETTING_PARALLEL_CHANNEL_KEY_20.isEmpty()) {

                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [20] init... "));

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_20, SETTING_PARALLEL_CHANNEL_KEY_20, outputTextArea)) {
                    channelAccounts.add(19, SETTING_PARALLEL_CHANNEL_ADDRESS_20);
                    channelKeys.add(19, SETTING_PARALLEL_CHANNEL_KEY_20);
                }
            } else {
                // Verify channel and balance
                Platform.runLater(() -> outputTextArea.appendText("Channel [20] not specified, skipping!\n"));
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
        // Select the operations network
        final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);

        // Create the key pair from the secret key, so we can check if it matches the channel address
        final KeyPair pair = KeyPair.fromSecretSeed(channelKey);

        try {
            final AccountResponse account = server.accounts().account(pair);

            // Check address
            if (!pair.getAccountId().equals(channelAddress)) {
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
	 * Fetch the specified account balance
	 *
     * @param address
	 * @return
	 */
    public static BigDecimal getBalance(final String address) throws IOException {
		// Select the operations network
		final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);

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

	/**
	 * Execute the given transactions in a single batch
	 *
	 * @param server
	 * @param source
	 * @param transactionResult
	 * @return
	 * @throws IOException
	 */
    public static TransactionBatchResponse executeTransactionBatch(final Server server, final KeyPair source, final KeyPair[] signers, final TransactionResult transactionResult) throws IOException {
		// Prepare response object
		final TransactionBatchResponse response = new TransactionBatchResponse();

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

		// Prepare a new transaction builder
		final AccountResponse sourceAccount = server.accounts().account(source);
		final Builder transactionBuilder = new Transaction.Builder(sourceAccount);

		// Add memo to the transaction
		transactionBuilder.addMemo(Memo.text(Settings.SETTING_MEMO));

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
		final SubmitTransactionResponse transactionResponse = server.submitTransaction(transaction);
		if (transactionResponse.isSuccess()) {
			// Transaction batch was successful
			response.success = true;
		} else {
			// Transaction batch failed
			response.success = false;
			response.transactionResponse = transactionResponse;
			response.errorMessages.add("The transaction response from the horizon network reported an unsuccessful outcome");
		}

		return response;
	}

    /**
     * Execute the given transactions in a single batch from the specified channel
     *
     * @param server
     * @param sourceAccount
     * @param transactionResult
     * @return
     * @throws IOException
     */
    public static TransactionBatchResponse executeChannelTransactionBatch(final Server server, final KeyPair sourceAccount, final KeyPair channelAccount, final KeyPair[] signers, final TransactionResult transactionResult) throws IOException {
        // Prepare response object
        final TransactionBatchResponse response = new TransactionBatchResponse();

        // Refuse batches with more than 100 operations
        if (transactionResult.getEntries().size() > SETTING_OPERATIONS_PER_TRANSACTION_BATCH) {
            response.success = false;
            response.errorMessages.add("Refusing to execute a transaction batch with more than [" + SETTING_OPERATIONS_PER_TRANSACTION_BATCH + "] entries. The batch contains [" + transactionResult.getEntries().size() + "] entries");

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

        // Prepare a new transaction builder for the channel
        final AccountResponse channelAccountResponse = server.accounts().account(channelAccount);
        final Builder transactionBuilder = new Transaction.Builder(channelAccountResponse);

        // Add memo to the transaction
        transactionBuilder.addMemo(Memo.text(Settings.SETTING_MEMO));

        // Process all entries
        for (TransactionResultEntry entry : transactionResult.getEntries()) {
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
        final SubmitTransactionResponse transactionResponse = server.submitTransaction(transaction);
        if (transactionResponse.isSuccess()) {
            // Transaction batch was successful
            response.success = true;
            response.transactionResponse = transactionResponse;
        } else {
            // Transaction batch failed
            response.success = false;
            response.transactionResponse = transactionResponse;
            response.errorMessages.add("The transaction response from the horizon network reported an unsuccessful outcome");
        }

        return response;
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

	//endregion
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

