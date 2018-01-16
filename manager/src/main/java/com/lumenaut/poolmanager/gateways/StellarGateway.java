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

import static com.lumenaut.poolmanager.Settings.*;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 12/01/2018 - 3:40 PM
 * <p>
 * Stateless class, exposes quick methods to accomplish operations on the stellar network currently selected in the
 * application (LIVE/TEST)
 */
public class StellarGateway {
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//region FIELDS

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

