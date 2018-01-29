package com.lumenaut.poolmanager.gateways;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumenaut.poolmanager.XLMUtils;
import org.postgresql.util.Base64;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;
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
    private final StringBuilder sb = new StringBuilder();

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
     * This is a rough attempt to detect if the horizon node we're connected is on a test network.
     *
     * @return
     * @throws SQLException
     */
    public boolean isTestNetwork() throws SQLException {
        // Prepared statement
        final Statement stm = conn.createStatement();
        final String checkNetworkSQL = "SELECT * FROM core.public.storestate WHERE statename = 'networkpassphrase'";

        // Extract results
        final ResultSet checkNetworkRS = stm.executeQuery(checkNetworkSQL);

        // Assume live network unless the network passphrase contains the "test" keyword.
        boolean isTestNework = false;
        if (checkNetworkRS.isBeforeFirst()) {
            checkNetworkRS.next();
            final String state = checkNetworkRS.getString("state");
            isTestNework = state.toLowerCase().contains("test");
        }

        // Release resources
        checkNetworkRS.close();
        stm.close();

        // Respond
        return isTestNework;
    }

    /**
     * Get the inflation votes currently cast to the specified accountId
     *
     * @param inflationDestination The public key of the account receiving the inflation votes
     * @return A JsonNode object containing the full structure of the extracted data
     * @throws SQLException
     */
    public JsonNode getVotersData(final String inflationDestination) throws SQLException {
        // Get donations data
        final HashMap<String, HashMap<String, String>> donationsData = getVotersCustomData(inflationDestination, "*");

        // Prepared statement
        final PreparedStatement inflationStm = conn.prepareStatement("SELECT * FROM core.public.accounts WHERE inflationdest = ?");
        inflationStm.setString(1, inflationDestination);
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

                if (donationsData.containsKey(voterAddress)) {
                    final HashMap<String, String> voterData = donationsData.get(voterAddress);
                    final ArrayNode voterDataEntriesArray = OBJECT_MAPPER.createArrayNode();

                    // Add all data to the entry data node array
                    for (HashMap.Entry<String, String> entry : voterData.entrySet()) {
                        final ObjectNode voterDataEntryNode = OBJECT_MAPPER.createObjectNode();
                        voterDataEntryNode.put(entry.getKey(), entry.getValue());
                        voterDataEntriesArray.add(voterDataEntryNode);
                    }

                    // Add the entry data array to the entry node
                    entryNode.set("data", voterDataEntriesArray);
                } else {
                    // No data
                    entryNode.set("data", null);
                }

                // Push to the entries array
                entriesNode.add(entryNode);
            });

            // Add root nodes
            rootNode.put("inflationdest", inflationDestination);
            rootNode.set("entries", entriesNode);

            // Release resources
            inflationRs.close();
            inflationStm.close();

            // Return generated structure
            return rootNode;
        }
    }

    /**
     * Retrieve the specified data entry (by name) for any account voting the given inflation destination address
     *
     * @param inflationDestination
     * @return
     * @throws SQLException
     */
    public HashMap<String, HashMap<String, String>> getVotersCustomData(final String inflationDestination, final String... dataNames) throws SQLException {
        // Build the query
        sb.setLength(0);
        sb.append("SELECT * FROM core.public.accounts LEFT JOIN core.public.accountdata ON accountdata.accountid = accounts.accountid WHERE inflationdest = ?");

        if (dataNames.length > 1) {
            sb.append(" AND (");
            for (String dataname : dataNames) {
                sb.append(" dataname = '").append(dataname).append("' OR");
            }

            // Cut trailing " OR" and close the AND clause
            sb.delete(sb.length() - 3, sb.length());
            sb.append(")");
        } else {
            if (!dataNames[0].equals("*")) {
                sb.append(" AND dataname = '").append(dataNames[0]).append("'");
            }
        }

        final PreparedStatement accountDataStm = conn.prepareStatement(sb.toString());
        accountDataStm.setString(1, inflationDestination);
        accountDataStm.setFetchSize(50);  // Fetch in batches of 50 records

        // Data results
        final HashMap<String, HashMap<String, String>> accountsData = new HashMap<>();

        // Create result entries
        final ResultSet accountDataRs = accountDataStm.executeQuery();
        if (!accountDataRs.isBeforeFirst()) {
            // Release resources
            accountDataRs.close();
            accountDataStm.close();

            // No records found
            return null;
        } else {
            while (accountDataRs.next()) {
                final String publicKey = accountDataRs.getString("accountid");
                final String dataName = accountDataRs.getString("dataname");
                final String dataValue = accountDataRs.getString("datavalue");

                // Skip rows without data entries
                if (dataName == null || dataValue == null) {
                    continue;
                }

                // Decode the value
                final String decodedDataValue = new String(Base64.decode(accountDataRs.getString("datavalue")), StandardCharsets.UTF_8);

                // Create entry in the hashmap if needed
                if (!accountsData.containsKey(publicKey)) {
                    accountsData.put(publicKey, new HashMap<>());
                }

                // Put the data entry
                final HashMap<String, String> accountData = accountsData.get(publicKey);
                accountData.put(dataName, decodedDataValue);
            }

            // Release resources
            accountDataRs.close();
            accountDataStm.close();

            // Return generated structure
            return accountsData;
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

            final BigDecimal result = XLMUtils.stroopToXLM(inflationRs.getLong("balance"));

            // Release resources
            inflationRs.close();
            inflationStm.close();

            // Return balance in XLM
            return result;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
