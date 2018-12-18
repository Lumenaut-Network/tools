import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @Author Luca Vignaroli
 * @Date 16/12/2018 - 12:19 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 */
public class AccountManager {

    // Verbose logging
    private static boolean LOG_VERBOSE = true;

    // Stellar Network
    public static final String HTTPS_HORIZON_TESTNET_STELLAR_ORG = "https://horizon.stellar.org/";
    public static final String HTTPS_FRIENDBOT_URL = "https://friendbot.stellar.org/?addr=%s";

    // Http client instance
    public static final ThreadLocal<OkHttpClient> HTTP_CLIENT = ThreadLocal.withInitial(() -> {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        return builder.retryOnConnectionFailure(true).connectTimeout(120L, TimeUnit.SECONDS).readTimeout(480L, TimeUnit.SECONDS).build();
    });

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Fund the specified account, creates it if not yet existing
     *
     * @param keyPair
     * @return
     */
    public static boolean createAndFundAccount(KeyPair keyPair) {
        final String friendbotUrl = String.format(HTTPS_FRIENDBOT_URL, keyPair.getAccountId());
        final Request request = new Builder().url(friendbotUrl).build();
        try (final Response response = HTTP_CLIENT.get().newCall(request).execute()) {
            // Successfully funded
            if (response.isSuccessful()) {
                System.out.println("[SUCCESS] Account created [" + keyPair.getAccountId() + "] and funded with 10000 XLM");

                return true;
            }

            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Failed to fund account: " + keyPair.getAccountId() + " [" + response.code() + " " + response.message() + "]");
            }

            return false;
        } catch (IOException e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Failed to fund account: " + keyPair.getAccountId() + " [" + e.getMessage() + "]");
            }

            return false;
        }
    }

    /**
     * Execute a payment
     *
     * @param sourceAccountSigningKey
     * @param destAccountId
     * @param xlmAmount
     * @return
     */
    public static boolean makePayment(final String sourceAccountSigningKey, final String destAccountId, final String xlmAmount) {
        final KeyPair source = KeyPair.fromSecretSeed(sourceAccountSigningKey);
        final KeyPair destination = KeyPair.fromAccountId(destAccountId);

        Network.useTestNetwork();
        final Server server = new Server(HTTPS_HORIZON_TESTNET_STELLAR_ORG);

        // Register the client within the server
        server.setSubmitHttpClient(HTTP_CLIENT.get());

        // Get accounts data
        AccountResponse sourceAccount;
        try {
            // Check that the destination account exists
            server.accounts().account(destination);

            // If there was no error, load up-to-date information on your account.
            sourceAccount = server.accounts().account(source);
        } catch (Exception e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Could not transfer funds from Account [" + source.getAccountId() + "] to the pool: " + e.getMessage());
            }

            return false;
        }

        // Start building the transaction.
        Transaction transaction = new Transaction.Builder(sourceAccount)
                                  .addOperation(new PaymentOperation.Builder(destination, new AssetTypeNative(), xlmAmount).build())
                                  .addMemo(Memo.text("Pool Funding"))
                                  .build();

        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        // And finally, send it off to Stellar!
        SubmitTransactionResponse response;
        try {
            response = server.submitTransaction(transaction);
            if (response.isSuccess()) {
                System.out.println("[SUCCESS] Account [" + source.getAccountId() + "] transfered " + xlmAmount + " XLM to the pool");

                return true;
            } else {
                if (LOG_VERBOSE) {
                    System.err.println("[FAILURE] Could not transfer funds from Account [" + source.getAccountId() + "] to the pool: " + response.getExtras().getResultCodes().getTransactionResultCode());
                }

                return false;
            }
        } catch (Exception e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Could not transfer funds from Account [" + source.getAccountId() + "] to the pool: " + e.getMessage());
            }

            return false;
        }
    }

    /**
     * Vote the speficied inflation poll address for the given acocunt
     *
     * @param sourceAccountSigningKey
     * @param inflationPoolAddress
     */
    public static boolean setInflationDestination(final String sourceAccountSigningKey, final String inflationPoolAddress) {
        final KeyPair source = KeyPair.fromSecretSeed(sourceAccountSigningKey);
        final KeyPair inflationDest = KeyPair.fromAccountId(inflationPoolAddress);

        Network.useTestNetwork();
        final Server server = new Server(HTTPS_HORIZON_TESTNET_STELLAR_ORG);

        // Register the client within the server
        server.setSubmitHttpClient(HTTP_CLIENT.get());

        // Get accounts data
        AccountResponse sourceAccount;
        try {
            // Check that the destination account exists
            server.accounts().account(inflationDest);

            // If there was no error, load up-to-date information on your account.
            sourceAccount = server.accounts().account(source);
        } catch (Exception e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Could not set inflation destination for account [" + source.getAccountId() + "]: " + e.getMessage());
            }

            return false;
        }

        // Start building the transaction.
        Transaction transaction = new Transaction.Builder(sourceAccount)
                                  .addOperation(new SetOptionsOperation.Builder().setInflationDestination(inflationDest).build())
                                  .addMemo(Memo.text("Inflation Change"))
                                  .build();

        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        // And finally, send it off to Stellar!
        SubmitTransactionResponse response;
        try {
            response = server.submitTransaction(transaction);
            if (response.isSuccess()) {
                System.out.println("[SUCCESS] Account [" + source.getAccountId() + "] set inflation destination to the pool");

                return true;
            } else {
                if (LOG_VERBOSE) {
                    System.err.println("[FAILURE] Could not set inflation destination for account [" + source.getAccountId() + "]: " + response.getExtras().getResultCodes().getTransactionResultCode());
                }

                return false;
            }
        } catch (Exception e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Could not set inflation destination for account [" + source.getAccountId() + "]: " + e.getMessage());
            }

            return false;
        }
    }

    /**
     * Merge the specified account with the target one
     *
     * @param sourceAccountSigningKey
     * @param targetAccountId
     * @return
     */
    public static boolean merge(final String sourceAccountSigningKey, final String targetAccountId) {
        final KeyPair source = KeyPair.fromSecretSeed(sourceAccountSigningKey);
        final KeyPair target = KeyPair.fromAccountId(targetAccountId);

        Network.useTestNetwork();
        final Server server = new Server(HTTPS_HORIZON_TESTNET_STELLAR_ORG);

        // Register the client within the server
        server.setSubmitHttpClient(HTTP_CLIENT.get());

        // Get accounts data
        AccountResponse sourceAccount;
        try {
            // Check that the destination account exists
            server.accounts().account(target);

            // If there was no error, load up-to-date information on the source account.
            sourceAccount = server.accounts().account(source);
        } catch (Exception e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Could not merge account [" + source.getAccountId() + "]: " + e.getMessage());
            }

            return false;
        }

        // Start building the transaction.
        Transaction transaction = new Transaction.Builder(sourceAccount)
                                  .addOperation(new AccountMergeOperation.Builder(target).setSourceAccount(source).build())
                                  .addMemo(Memo.text("Inflation Fund"))
                                  .build();

        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(source);

        // And finally, send it off to Stellar!
        SubmitTransactionResponse response;
        try {
            response = server.submitTransaction(transaction);
            if (response.isSuccess()) {
                System.out.println("[SUCCESS] Account [" + source.getAccountId() + "] merged");

                return true;
            } else {
                if (LOG_VERBOSE) {
                    System.err.println("[FAILURE] Could not merge account [" + source.getAccountId() + "]: " + response.getExtras().getResultCodes().getTransactionResultCode());
                }

                return false;
            }
        } catch (Exception e) {
            if (LOG_VERBOSE) {
                System.err.println("[FAILURE] Could not merge account [" + source.getAccountId() + "]: " + e.getMessage());
            }

            return false;
        }
    }

    /**
     * Dispose of all resources used
     */
    public static void dispose() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
