package com.lumenaut.poolmanager.gateways;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumenaut.poolmanager.DataFormats;
import javafx.scene.control.Label;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.lumenaut.poolmanager.Settings.SETTING_OPERATIONS_NETWORK;

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
	 * Execute the transactions with the given json
	 *
	 * @param transactions
	 * @param amountPaid
	 * @param secretSeed
	 * @param memo
	 * @return
	 */
	public boolean executeTransactions(JsonNode transactions, Label amountPaid, String secretSeed, String memo) {
		if (transactions != null) {

			long totalPaid = 0;

			String uuid = transactions.get("uuid").asText();

			ObjectNode result = DataFormats.OBJECT_MAPPER.createObjectNode();
			result.put("uuid", uuid);
			ArrayNode nodeEntries = DataFormats.OBJECT_MAPPER.createArrayNode();

			Server server = new Server(SETTING_OPERATIONS_NETWORK.equals("LIVE") ? HORIZON_LIVE_NETWORK : HORIZON_TEST_NETWORK);
			KeyPair source = KeyPair.fromSecretSeed(secretSeed);

			int operationsCount = 0;
			Map<KeyPair, String> destinationBatch = new HashMap<>();

			for (JsonNode entry : transactions.get("entries")) {
				if (operationsCount < 100) {
					String amount = entry.get("amount").asText();
					String account = entry.get("destination").asText();
					destinationBatch.put(KeyPair.fromAccountId(account), amount);
					operationsCount++;
				} else {
					if (executeTransactionBatch(server, source, destinationBatch, memo)) {
						totalPaid += writeJsonTransaction(destinationBatch, nodeEntries, result);
						amountPaid.setText(""+totalPaid);

						operationsCount = 0;
						result = DataFormats.OBJECT_MAPPER.createObjectNode();
						result.put("uuid", uuid);
						nodeEntries = DataFormats.OBJECT_MAPPER.createArrayNode();
						destinationBatch.clear();
					} else {
						System.out.println("Transaction batch failed.");
						return false;
					}
				}
			}

			if (!destinationBatch.isEmpty() && executeTransactionBatch(server, source, destinationBatch, memo)) {
				totalPaid += writeJsonTransaction(destinationBatch, nodeEntries, result);
				amountPaid.setText(""+totalPaid);
			} else {
				System.out.println("Transaction batch failed.");
				return false;
			}

			return true;
		} else {
			return false;
		}
	}

	private long writeJsonTransaction(Map<KeyPair, String> destinationBatch, ArrayNode nodeEntries, ObjectNode result) {
		long totalPaid = 0;

		long timestamp = System.currentTimeMillis();
		for (Map.Entry<KeyPair, String> destination : destinationBatch.entrySet()) {
			ObjectNode transactionEntry = DataFormats.OBJECT_MAPPER.createObjectNode();
			transactionEntry.put("timestamp", timestamp);
			transactionEntry.put("amount", destination.getValue());
			transactionEntry.put("destination", destination.getKey().getAccountId());
			nodeEntries.add(transactionEntry);

			totalPaid += Double.parseDouble(destination.getValue());
		}
		result.set("entries", nodeEntries);

		try {
			File transactionJson = File.createTempFile("transactionJson_", ".JSON");
			DataFormats.OBJECT_MAPPER.writeValue(transactionJson, result);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return totalPaid;
	}

	private boolean executeTransactionBatch(Server server, KeyPair source, Map<KeyPair, String> destinationBatch, String memo) {
		if (verifyAccounts(server, destinationBatch)) {
			try {
				AccountResponse sourceAccount = server.accounts().account(source);

				Transaction.Builder transactionBuilder = new Transaction.Builder(sourceAccount);
				for (Map.Entry<KeyPair, String> entry : destinationBatch.entrySet()) {
					transactionBuilder.addOperation(new PaymentOperation.Builder(entry.getKey(), new AssetTypeNative(), entry.getValue()).build());
				}
				transactionBuilder.addMemo(Memo.text(memo));

				Transaction transaction = transactionBuilder.build();
				transaction.sign(source);
				SubmitTransactionResponse response = server.submitTransaction(transaction);

				return response.isSuccess();
			} catch (IOException e) {
				System.out.println("Source account not found");
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean verifyAccounts(Server server, Map<KeyPair, String> destinationBatch) {
		for (Map.Entry<KeyPair, String> entry : destinationBatch.entrySet()) {
			try {
				server.accounts().account(entry.getKey());
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	//endregion
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

