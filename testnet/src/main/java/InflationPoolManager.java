import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.stellar.sdk.KeyPair;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author Luca Vignaroli
 * @Date 16/12/2018 - 4:52 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 */
public class InflationPoolManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // JSON Object mapper
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Submission Threadpool
    private static final ThreadPoolExecutor EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    // Formats
    public static final DateFormat FILE_DATE_FORMATTER = new SimpleDateFormat("HH'h'mm'm'ss's'");

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region DATA STRUCTURES

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InflationPool {
        private Account pool;
        private List<Account> channels;
        private List<Account> voters;

        public Account getPool() {
            return pool;
        }

        public void setPool(Account pool) {
            this.pool = pool;
        }

        public List<Account> getChannels() {
            return channels;
        }

        public void setChannels(List<Account> channels) {
            this.channels = channels;
        }

        public List<Account> getVoters() {
            return voters;
        }

        public void setVoters(List<Account> voters) {
            this.voters = voters;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account {
        private String accountId;
        private String accountKey;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountKey() {
            return accountKey;
        }

        public void setAccountKey(String accountKey) {
            this.accountKey = accountKey;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Creates a new account and funds it
     */
    public static void createNewPool(final int numVoters, final int numChannels) {
        // Create new pool
        System.out.println("Creating pool account.. ");
        final KeyPair poolKeys = createAndFundAccount();
        if (poolKeys == null){
            return;
        }

        // Pool data
        final InflationPool pool = new InflationPool();

        // Add the pool
        final Account poolAccount = new Account();
        poolAccount.setAccountId(poolKeys.getAccountId());
        poolAccount.setAccountKey(new String(poolKeys.getSecretSeed()));
        pool.setPool(poolAccount);
        System.out.println("[DONE] Pool account has been created\n");

        // Create voters
        System.out.println("Creating voter accounts.. ");
        final ConcurrentLinkedQueue<Future<?>> votersFutures = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<Account> votersList = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < numVoters; i++) {
            queueAccountCreation(votersFutures, votersList);
        }

        // Wait for all the voter accounts to be created
        boolean votersCreated = false;
        while (!votersCreated) {
            // Poll for completion in one second
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check if all requests are done
            boolean allDone = true;
            for(Future<?> future : votersFutures){
                boolean isDone = future.isDone();
                if (isDone) {
                    votersFutures.remove(future);
                }

                allDone &= isDone;
            }

            if (allDone) {
                votersCreated = true;
            } else {
                System.out.println("Waiting for voters creation to complete: " + votersList.size() + "/" + numVoters);
            }
        }

        // Add voters to the pool
        final List<Account> voters = new ArrayList<>(votersList);
        pool.setVoters(voters);
        System.out.println("[DONE] Voter accounts have been created\n");

        // Create channels
        System.out.println("Creating channel accounts.. ");
        final ConcurrentLinkedQueue<Future<?>> channelsFutures = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<Account> channelsList = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < numChannels; i++) {
            queueAccountCreation(channelsFutures, channelsList);
        }

        // Wait for all the channel accounts to be created
        boolean channelsCreated = false;
        while (!channelsCreated) {
            // Poll for completion in one second
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check if all requests are done
            boolean allDone = true;
            for(Future<?> future : channelsFutures){
                boolean isDone = future.isDone();
                if (isDone) {
                    channelsFutures.remove(future);
                }

                allDone &= isDone;
            }

            if (allDone) {
                channelsCreated = true;
            } else {
                System.out.println("Waiting for channels creation to complete: " + channelsList.size() + "/" + numChannels);
            }
        }

        // Add channels to the pool
        final List<Account> channels = new ArrayList<>(channelsList);
        pool.setChannels(channels);
        System.out.println("[DONE] Creating channel accounts\n");


        // Move funds from the voters to the pool
        System.out.println("Transfering funds from voter accounts to the pool..");
        for (Account voter : pool.getVoters()) {
            AccountManager.makePayment(voter.getAccountKey(), poolAccount.getAccountId(), "9990");
        }
        System.out.println("[DONE] Transfering funds to the pool\n");

        // Set inflation on all voter accounts
        System.out.println("Setting inflation on all voter accounts..");
        for (Account voter : pool.getVoters()) {
            AccountManager.setInflationDestination(voter.getAccountKey(), poolAccount.getAccountId());
        }
        System.out.println("[DONE] Setting inflation destination\n");

        // Save to file
        System.out.println("Writing new testnet pool data to file...");
        final String outPutFilePath = "Testnet_Inflation_Pool-" + FILE_DATE_FORMATTER.format(new Date()) + ".json";
        try (
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outPutFilePath), StandardCharsets.UTF_8);
        BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(pool));
        } catch (IOException e) {
            System.err.println("[FAILURE] Could not write pool data to the disk: " + e.getMessage());
        }
        System.out.println("[SUCCESS] Testnet Pool is ready for use\n");

        // Shutdown the threadpool
        EXECUTOR.shutdownNow();
    }

    /**
     * Enqueue an account creation request
     *
     * @param futures
     * @param votersList
     */
    private static void queueAccountCreation(ConcurrentLinkedQueue<Future<?>> futures, ConcurrentLinkedQueue<Account> votersList) {
        final Future<?> future = EXECUTOR.submit(() -> {
            final KeyPair voterKeys = createAndFundAccount();
            if (voterKeys == null) {
                // This request failed, queue another one to replace it
                queueAccountCreation(futures, votersList);

                return;
            }

            // Append to the voters' list
            final Account voterAccount = new Account();
            voterAccount.setAccountId(voterKeys.getAccountId());
            voterAccount.setAccountKey(new String(voterKeys.getSecretSeed()));
            votersList.add(voterAccount);
        });

        // Append to the futures
        futures.add(future);
    }

    /**
     * Creates a new inflation pool, including voting accounts
     */
    private static KeyPair createAndFundAccount() {
        final KeyPair poolAccount = KeyPair.random();
        if (AccountManager.createAndFundAccount(poolAccount)) {
            return poolAccount;
        }

        return null;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
