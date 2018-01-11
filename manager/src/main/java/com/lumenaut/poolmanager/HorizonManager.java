package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.HorizonData.AccountCreationResponse;
import org.stellar.sdk.KeyPair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * @Author Burn
 * @Date 09/01/2018 - 5:45 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 * <p>
 * This class exposes simple methods to interact with a Horizon and Stellar Core database
 */
public class HorizonManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Horizon node
    private final String horizonNodeAddress;

    // Defaults to 15 seconds
    private int horizonConnectionTimeout = 15 * 1000;

    // Data
    private Connection horizonConn;
    private Connection coreConn;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region ACCESSORS

    public int getHorizonConnectionTimeout() {
        return horizonConnectionTimeout;
    }

    public void setHorizonConnectionTimeout(int horizonConnectionTimeout) {
        this.horizonConnectionTimeout = horizonConnectionTimeout;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     *
     * @param horizonNodeAddress
     */
    public HorizonManager(final String horizonNodeAddress, final String horizonDatabaseAddress, final String horizonDatabasePort) {
        // Bind the horizon node address for this manager instance
        this.horizonNodeAddress = horizonNodeAddress;

        // Build jdbc connection url
        final StringBuilder horizonNodeDatabaseUrl = new StringBuilder();
        if (horizonDatabaseAddress != null) {
            horizonNodeDatabaseUrl.append("jdbc:postgresql://").append(horizonDatabaseAddress);

            // Use custom port if specified
            if (horizonDatabasePort != null) {
                horizonNodeDatabaseUrl.append(":").append(horizonDatabasePort);
            }
        }

        // Establish connections
        final Properties props = new Properties();
        props.setProperty("user", "stellar");
        props.setProperty("password", "aADgFDGVWAk0Q6hF");
        try {
            // Core database connection
            coreConn = DriverManager.getConnection(horizonNodeDatabaseUrl.toString() + "/core", props);

            // Horizon database connection
            horizonConn = DriverManager.getConnection(horizonNodeDatabaseUrl.toString() + "/horizon", props);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
     * Get the votes currently cast to the specified pool
     *
     * @param votesTargetPublicKey The public key of the account receiving the inflation votes
     * @return A hashmap whose keys are the account ids of the voters and values represent their current balance
     * @throws SQLException
     */
    public HashMap<String, Long> getInflationVoters(final String votesTargetPublicKey) throws SQLException {
        // Prepared statement
        final PreparedStatement inflationStm = coreConn.prepareStatement("SELECT * FROM core.public.accounts WHERE inflationdest = ?");
        inflationStm.setString(1, votesTargetPublicKey);
        inflationStm.setFetchSize(50);  // Fetch in batches of 50 records

        // Extract results
        final ResultSet inflationRs = inflationStm.executeQuery();
        if (!inflationRs.isBeforeFirst()) {
            // No records found
            return null;
        } else {
            // Extract votes
            final HashMap<String, Long> votes = new HashMap<>();
            while (inflationRs.next()) {
                final String publicKey = inflationRs.getString("accountid");
                final long balance = inflationRs.getLong("balance");
                votes.put(publicKey, balance);
            }

            return votes;
        }
    }

    /**
     * Create a new account
     *
     * @return
     */
    public AccountCreationResponse createNewAccount() throws IOException {
        // Generate random keys
        final KeyPair keyPair = KeyPair.random();

        // Use Friendbot to create a new account with the newly generated public key
        final String friendBotQueryUrl = String.format(horizonNodeAddress + "/friendbot?addr=%s", keyPair.getAccountId());
        final URL url = new URL(friendBotQueryUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Attempt connection
        connection.setRequestMethod("GET");
        connection.setReadTimeout(horizonConnectionTimeout);
        connection.connect();

        // Read response body
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder stringBuilder = new StringBuilder();
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            stringBuilder.append(responseLine).append("\n");
        }

        // Grab the response
        final String responseBody = stringBuilder.length() > 0 ? stringBuilder.toString() : null;

        // Return the key pair
        return new AccountCreationResponse(friendBotQueryUrl, responseBody);
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
