package com.lumenaut.poolmanager.gateways;

import static com.lumenaut.poolmanager.Settings.SETTING_OPERATIONS_NETWORK;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;
import org.stellar.sdk.Network;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import com.lumenaut.poolmanager.DataFormats;

import javafx.scene.control.Label;

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

	// Horizon networks
	private static final String HORIZON_TEST_NETWORK = "https://horizon-testnet.stellar.org";
	private static final String HORIZON_LIVE_NETWORK = "https://horizon.stellar.org";

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
	 * Execute the transactions with the given transactionPlan
	 *
	 * @param transactionPlan
	 * @param amountPaid
	 * @param secretSeed
	 * @param memo
	 * @return
	 */
	public static boolean executeTransactions(DataFormats.TransactionPlan transactionPlan, Label amountPaid, String secretSeed, String memo) throws IOException {
		if (transactionPlan != null) {
			DataFormats.TransactionResult transactionResult = new DataFormats.TransactionResult();
			long totalPaid = 0;

			transactionResult.setUuid(transactionPlan.getUuid());

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
			KeyPair source = KeyPair.fromSecretSeed(secretSeed);

			int operationsCount = 0;

			for (DataFormats.TransactionPlanEntry entry : transactionPlan.getEntries()) {
				if (operationsCount < 100) {
					DataFormats.TransactionResultEntry transactionResultEntry = new DataFormats.TransactionResultEntry();
					transactionResultEntry.setDestination(entry.getDestination());
					transactionResultEntry.setAmount(entry.getAmount());
					transactionResult.addEntry(transactionResultEntry);
					operationsCount++;
				} else {
					if (executeTransactionBatch(server, source, transactionResult, memo)) {
						totalPaid += writeJsonTransaction(transactionResult);
						amountPaid.setText("" + totalPaid);

						operationsCount = 0;
						transactionResult = new DataFormats.TransactionResult();
						transactionResult.setUuid(transactionPlan.getUuid());
					} else {
						return false;
					}
				}
			}

			if (!transactionResult.getEntries().isEmpty() && executeTransactionBatch(server, source, transactionResult, memo)) {
				totalPaid += writeJsonTransaction(transactionResult);
				amountPaid.setText("" + totalPaid);
			} else {
				System.out.println("Transaction batch failed.");
				return false;
			}

			return true;
		} else {
			return false;
		}
	}

	private static long writeJsonTransaction(DataFormats.TransactionResult transactionResult) {
		long totalPaid = 0;

		for (DataFormats.TransactionResultEntry transactionEntry : transactionResult.getEntries()) {
			transactionResult.addEntry(transactionEntry);
			totalPaid += transactionEntry.getAmount();
		}

		try {
			File transactionJson = File.createTempFile("transactionJson_", ".JSON");
			DataFormats.OBJECT_MAPPER.writeValue(transactionJson, DataFormats.OBJECT_MAPPER.valueToTree(transactionResult));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return totalPaid;
	}

	private static boolean executeTransactionBatch(Server server, KeyPair source, DataFormats.TransactionResult transactionResult, String memo) throws IOException {
		if (verifyAccounts(server, transactionResult)) {
			try {
				AccountResponse sourceAccount = server.accounts().account(source);

				Transaction.Builder transactionBuilder = new Transaction.Builder(sourceAccount);

				long timestamp = System.currentTimeMillis();
				for (DataFormats.TransactionResultEntry entry : transactionResult.getEntries()) {
					entry.setTimestamp(timestamp);
					transactionBuilder.addOperation(new PaymentOperation.Builder(KeyPair.fromAccountId(entry.getDestination()), new AssetTypeNative(), entry.getAmount().toString()).build());
				}
				transactionBuilder.addMemo(Memo.text(memo));

				Transaction transaction = transactionBuilder.build();
				transaction.sign(source);
				SubmitTransactionResponse response = server.submitTransaction(transaction);

				if (!response.isSuccess()) {
					System.out.println("Transaction batch failed.");
					System.out.println(response);
				}

				return response.isSuccess();
			} catch (Throwable e) {
				System.out.println("Unable to execute transaction batch: " + e.getMessage());
				e.printStackTrace();

				throw e;
			}
		}
		return false;
	}

	private static boolean verifyAccounts(Server server, DataFormats.TransactionResult transactionResult) {
		for (DataFormats.TransactionResultEntry entry : transactionResult.getEntries()) {
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

