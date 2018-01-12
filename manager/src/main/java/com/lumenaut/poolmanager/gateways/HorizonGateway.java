package com.lumenaut.poolmanager.gateways;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

import static com.lumenaut.poolmanager.InflationDataFormat.OBJECT_MAPPER;
import static com.lumenaut.poolmanager.Settings.*;

/**
 * @Author Burn
 * @Date 09/01/2018 - 5:45 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 * <p>
 * This class manages the connection to a custom Horizon Node database
 */
public class HorizonGateway {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

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
    public HorizonGateway() {

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
        if (connected) {
            conn.close();
            connected = false;
        }
    }

    /**
     * Force a disconnection and reconnection, useful when changing parameter settings
     *
     * @throws SQLException
     */
    public void reconnect() throws SQLException {
        disconnect();
        connect();
    }

    /**
     * Get the inflation votes currently cast to the specified accountId
     *
     * @param accountId The public key of the account receiving the inflation votes
     * @return A hashmap whose keys are the account ids of the voters and values represent their current balance
     * @throws SQLException
     */
    public JsonNode getVoters(final String accountId) throws SQLException {
        // Prepared statement
        final PreparedStatement inflationStm = conn.prepareStatement("SELECT * FROM core.public.accounts WHERE inflationdest = ?");
        inflationStm.setString(1, accountId);
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

            // Prepare JSON tree
            final ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
            final ArrayNode entriesNode = OBJECT_MAPPER.createArrayNode();

            // Append entries
            votes.forEach((voterAddress, balance) -> {
                final ObjectNode entryNode = OBJECT_MAPPER.createObjectNode();
                entryNode.put("balance", balance);
                entryNode.put("account", voterAddress);

                // Push to the entries array
                entriesNode.add(entryNode);
            });

            // Add root nodes
            rootNode.put("inflationdest", accountId);
            rootNode.set("entries", entriesNode);

            // Return generated structure
            return rootNode;
        }
    }

    /**
     * Get the account balance of the specified accountId
     *
     * @param accountId The public key of the account
     * @return
     * @throws SQLException
     */
    public BigDecimal getBalance(final String accountId) throws SQLException {
        // Prepared statement
        final PreparedStatement inflationStm = conn.prepareStatement("SELECT * FROM core.public.accounts WHERE accountid = ? LIMIT 1");
        inflationStm.setString(1, accountId);

        // Extract results
        final ResultSet inflationRs = inflationStm.executeQuery();
        if (!inflationRs.isBeforeFirst()) {
            // No records found
            return null;
        } else {
            // Move cursor to the first record
            inflationRs.next();

            // Return balance in XLM
            return new BigDecimal(inflationRs.getLong("balance") / 10000000);
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
