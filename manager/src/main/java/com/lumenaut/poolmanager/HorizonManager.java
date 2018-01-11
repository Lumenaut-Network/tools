package com.lumenaut.poolmanager;

import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

import static com.lumenaut.poolmanager.Settings.*;

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

    // Defaults to 15 seconds
    private static final int HORIZON_CONNECTION_TIMEOUT = 15 * 1000;

    // Data
    private Connection conn;
    private boolean connected;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region ACCESSORS


    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    public HorizonManager() {

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
     * Connect to the currently configured horizon database
     *
     * @throws SQLException
     */
    public void connect() throws SQLException {
        // Build jdbc connection url
        final StringBuilder horizonNodeDatabaseUrl = new StringBuilder();
        if (!SETTING_HORIZON_DB_ADDRESS.isEmpty()) {
            horizonNodeDatabaseUrl.append("jdbc:postgresql://").append(SETTING_HORIZON_DB_ADDRESS);

            // Use custom port if specified
            if (!SETTING_HORIZON_DB_PORT.isEmpty()) {
                horizonNodeDatabaseUrl.append(":").append(SETTING_HORIZON_DB_PORT);
            }
        }

        // Establish connections
        final Properties props = new Properties();
        props.setProperty("user", SETTING_HORIZON_DB_USER);
        props.setProperty("password", SETTING_HORIZON_DB_PASS);

        // Core database connection
        conn = DriverManager.getConnection(horizonNodeDatabaseUrl.toString() + "/core", props);
        connected = true;
    }

    /**
     * Close the database connection
     *
     * @throws SQLException
     */
    public void disconnect() throws SQLException {
        conn.close();
        connected = false;
    }

    /**
     * Get the votes currently cast to the specified pool
     *
     * @param votesTargetPublicKey The public key of the account receiving the inflation votes
     * @return A hashmap whose keys are the account ids of the voters and values represent their current balance
     * @throws SQLException
     */
    public HashMap<String, Long> getInflationVoters(final String votesTargetPublicKey) throws SQLException {
        // Prepared statement
        final PreparedStatement inflationStm = conn.prepareStatement("SELECT * FROM core.public.accounts WHERE inflationdest = ?");
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

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
