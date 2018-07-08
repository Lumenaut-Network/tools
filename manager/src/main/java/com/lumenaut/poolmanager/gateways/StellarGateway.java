package com.lumenaut.poolmanager.gateways;

import com.lumenaut.poolmanager.DataFormats.TransactionBatchResponse;
import com.lumenaut.poolmanager.DataFormats.TransactionResult;
import com.lumenaut.poolmanager.DataFormats.TransactionResultEntry;
import com.lumenaut.poolmanager.Settings;
import com.lumenaut.poolmanager.XLMUtils;
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

    // Minimum balance for which a channel is considered usable for transactions (1.5 XLM)
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
    public static void initParallelSubmission() {
        if (SETTING_PARALLEL_CHANNELS_ENABLED) {
            // Reset channels
            channelAccounts = new ArrayList<>();
            channelKeys = new ArrayList<>();

            // Channel 1
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_1 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_1.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_1 != null && !SETTING_PARALLEL_CHANNEL_KEY_1.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_1, SETTING_PARALLEL_CHANNEL_KEY_1)) {
                    channelAccounts.add(0, SETTING_PARALLEL_CHANNEL_ADDRESS_1);
                    channelKeys.add(0, SETTING_PARALLEL_CHANNEL_KEY_1);
                }
            }

            // Channel 2
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_2 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_2.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_2 != null && !SETTING_PARALLEL_CHANNEL_KEY_2.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_2, SETTING_PARALLEL_CHANNEL_KEY_2)) {
                    channelAccounts.add(1, SETTING_PARALLEL_CHANNEL_ADDRESS_2);
                    channelKeys.add(1, SETTING_PARALLEL_CHANNEL_KEY_2);
                }
            }

            // Channel 3
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_3 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_3.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_3 != null && !SETTING_PARALLEL_CHANNEL_KEY_3.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_3, SETTING_PARALLEL_CHANNEL_KEY_3)) {
                    channelAccounts.add(2, SETTING_PARALLEL_CHANNEL_ADDRESS_3);
                    channelKeys.add(2, SETTING_PARALLEL_CHANNEL_KEY_3);
                }
            }

            // Channel 4
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_4 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_4.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_4 != null && !SETTING_PARALLEL_CHANNEL_KEY_4.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_4, SETTING_PARALLEL_CHANNEL_KEY_4)) {
                    channelAccounts.add(3, SETTING_PARALLEL_CHANNEL_ADDRESS_4);
                    channelKeys.add(3, SETTING_PARALLEL_CHANNEL_KEY_4);
                }
            }

            // Channel 5
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_5 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_5.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_5 != null && !SETTING_PARALLEL_CHANNEL_KEY_5.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_5, SETTING_PARALLEL_CHANNEL_KEY_5)) {
                    channelAccounts.add(4, SETTING_PARALLEL_CHANNEL_ADDRESS_5);
                    channelKeys.add(4, SETTING_PARALLEL_CHANNEL_KEY_5);
                }
            }

            // Channel 6
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_6 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_6.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_6 != null && !SETTING_PARALLEL_CHANNEL_KEY_6.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_6, SETTING_PARALLEL_CHANNEL_KEY_6)) {
                    channelAccounts.add(5, SETTING_PARALLEL_CHANNEL_ADDRESS_6);
                    channelKeys.add(5, SETTING_PARALLEL_CHANNEL_KEY_6);
                }
            }

            // Channel 7
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_7 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_7.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_7 != null && !SETTING_PARALLEL_CHANNEL_KEY_7.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_7, SETTING_PARALLEL_CHANNEL_KEY_7)) {
                    channelAccounts.add(6, SETTING_PARALLEL_CHANNEL_ADDRESS_7);
                    channelKeys.add(6, SETTING_PARALLEL_CHANNEL_KEY_7);
                }
            }

            // Channel 8
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_8 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_8.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_8 != null && !SETTING_PARALLEL_CHANNEL_KEY_8.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_8, SETTING_PARALLEL_CHANNEL_KEY_8)) {
                    channelAccounts.add(7, SETTING_PARALLEL_CHANNEL_ADDRESS_8);
                    channelKeys.add(7, SETTING_PARALLEL_CHANNEL_KEY_8);
                }
            }

            // Channel 9
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_9 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_9.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_9 != null && !SETTING_PARALLEL_CHANNEL_KEY_9.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_9, SETTING_PARALLEL_CHANNEL_KEY_9)) {
                    channelAccounts.add(8, SETTING_PARALLEL_CHANNEL_ADDRESS_9);
                    channelKeys.add(8, SETTING_PARALLEL_CHANNEL_KEY_9);
                }
            }

            // Channel 10
            if (SETTING_PARALLEL_CHANNEL_ADDRESS_10 != null && !SETTING_PARALLEL_CHANNEL_ADDRESS_10.isEmpty() &&
                SETTING_PARALLEL_CHANNEL_KEY_10 != null && !SETTING_PARALLEL_CHANNEL_KEY_10.isEmpty()) {

                // Verify channel and balance
                if (verifyChannel(SETTING_PARALLEL_CHANNEL_ADDRESS_10, SETTING_PARALLEL_CHANNEL_KEY_10)) {
                    channelAccounts.add(9, SETTING_PARALLEL_CHANNEL_ADDRESS_10);
                    channelKeys.add(9, SETTING_PARALLEL_CHANNEL_KEY_10);
                }
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
    private static boolean verifyChannel(final String channelAddress, final String channelKey) {
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

            // Check balance
            if (XLMUtils.XLMToStroop(totalBalance) < MINIMUM_CHANNEL_BALANCE) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    /**
	 * Fetch the specified account balance
	 *
	 * @param accountId
	 * @return
	 */
	public static BigDecimal getBalance(final String accountId) throws IOException {
		// Select the operations network
		final Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);

		// Build a key pair for the account id specified
		final KeyPair pair = KeyPair.fromAccountId(accountId);

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
	public static TransactionBatchResponse executeTransactionBatch(final Server server, final KeyPair source, final TransactionResult transactionResult) throws IOException {
		// Prepare response object
		final TransactionBatchResponse response = new TransactionBatchResponse();

		// Refuse batches with more than 100 operations
		if (transactionResult.getEntries().size() > SETTING_OPERATIONS_PER_TRANSACTION_BATCH) {
			response.success = false;
			response.errorMessages.add("Refusing to execute a transaction batch with more than [" + SETTING_OPERATIONS_PER_TRANSACTION_BATCH + "] entries. The batch contains [" + transactionResult.getEntries().size() + "] entries");

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
		transaction.sign(source);

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

