package com.lumenaut.poolmanager.gateways;

import com.lumenaut.poolmanager.DataFormats.VoterCustomDataEntry;
import com.lumenaut.poolmanager.DataFormats.VoterDataEntry;
import com.lumenaut.poolmanager.DataFormats.VotersData;
import org.postgresql.util.Base64;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.UIUtils.showError;

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
    private final VotersData votersData;

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
        votersData = new VotersData();
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

        switch (SETTING_OPERATIONS_NETWORK) {
            case "TEST": {
                // Check if we have all the settings required
                if (SETTING_HORIZON_DB_TEST_ADDRESS.isEmpty() ||
                    SETTING_HORIZON_DB_TEST_PORT.isEmpty() ||
                    SETTING_HORIZON_DB_TEST_USER.isEmpty() ||
                    SETTING_HORIZON_DB_TEST_PASS.isEmpty()) {
                    showError("You must specify all required connection settings in order to use a Horizon node database for the TEST network.");

                    return;
                }

                horizonNodeDatabaseUrl.append("jdbc:postgresql://").append(SETTING_HORIZON_DB_TEST_ADDRESS);
                horizonNodeDatabaseUrl.append(":").append(SETTING_HORIZON_DB_TEST_PORT);

                // Establish connections
                final Properties props = new Properties();
                props.setProperty("user", SETTING_HORIZON_DB_TEST_USER);
                props.setProperty("password", SETTING_HORIZON_DB_TEST_PASS);

                // Core database connection
                conn = DriverManager.getConnection(horizonNodeDatabaseUrl.toString() + "/core", props);
                connected = true;
            }
            break;
            case "LIVE": {
                // Check if we have all the settings required
                if (SETTING_HORIZON_DB_LIVE_ADDRESS.isEmpty() ||
                    SETTING_HORIZON_DB_LIVE_PORT.isEmpty() ||
                    SETTING_HORIZON_DB_LIVE_USER.isEmpty() ||
                    SETTING_HORIZON_DB_LIVE_PASS.isEmpty()) {
                    showError("You must specify all required connection settings in order to use a Horizon node database for the LIVE network.");

                    return;
                }

                horizonNodeDatabaseUrl.append("jdbc:postgresql://").append(SETTING_HORIZON_DB_LIVE_ADDRESS);
                horizonNodeDatabaseUrl.append(":").append(SETTING_HORIZON_DB_LIVE_PORT);

                // Establish connections
                final Properties props = new Properties();
                props.setProperty("user", SETTING_HORIZON_DB_LIVE_USER);
                props.setProperty("password", SETTING_HORIZON_DB_LIVE_PASS);

                // Core database connection
                conn = DriverManager.getConnection(horizonNodeDatabaseUrl.toString() + "/core", props);
                connected = true;
            }
            break;
            default:
                showError("Unknown operations network selected: " + SETTING_OPERATIONS_NETWORK);
        }
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
    public VotersData getVotersData(final String inflationDestination) throws SQLException {
        // Reset voters data
        votersData.reset();

        // Get donations data
        final HashMap<String, HashMap<String, String>> donationsData = getVotersCustomData(inflationDestination, "lumenaut.net donation%");
        final long poolBalance = getBalance(inflationDestination);

        // Stellar core's inflation implementation does not include accounts holding less than 100XLM in their balance, neither should we
        final PreparedStatement inflationStm = conn.prepareStatement("SELECT * FROM core.public.accounts WHERE inflationdest = ? AND balance >= 1000000000 ORDER BY balance DESC;");

        // Fetch in batches of 100 records
        inflationStm.setFetchSize(100);
        inflationStm.setString(1, inflationDestination);

        // Extract results
        final ResultSet inflationRs = inflationStm.executeQuery();
        if (!inflationRs.isBeforeFirst()) {
            // No records found
            return null;
        } else {
            // Extract votes
            while (inflationRs.next()) {
                final String publicKey = inflationRs.getString("accountid");
                final long balance = inflationRs.getLong("balance");
                final VoterDataEntry entry = new VoterDataEntry();
                entry.setAccount(publicKey);
                entry.setBalance(balance);

                // Append
                votersData.getEntries().add(entry);
            }

            // Append entries
            votersData.getEntries().forEach((voterDataEntry) -> {
                if (donationsData != null && donationsData.containsKey(voterDataEntry.getAccount())) {
                    final HashMap<String, String> voterAdditionalData = donationsData.get(voterDataEntry.getAccount());

                    // Add all custom data to the entry data node array
                    final List<VoterCustomDataEntry> voterCustomData = new ArrayList<>();
                    for (HashMap.Entry<String, String> entry : voterAdditionalData.entrySet()) {
                        final VoterCustomDataEntry customData = new VoterCustomDataEntry();
                        customData.setDataname(entry.getKey());
                        customData.setDatavalue(entry.getValue());

                        // Append to the voter's custom data
                        voterCustomData.add(customData);

                        // Integrate voter data with all the custom data found
                        voterDataEntry.setData(voterCustomData);
                    }
                } else {
                    // No data
                    voterDataEntry.setData(null);
                }
            });

            // Add root nodes
            votersData.setInflationdest(inflationDestination);
            votersData.setBalance(poolBalance);

            // Release resources
            inflationRs.close();
            inflationStm.close();

            // Return generated structure
            return votersData;
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
        sb.append("SELECT * FROM core.public.accounts LEFT JOIN core.public.accountdata ON accountdata.accountid = accounts.accountid WHERE inflationdest = ? AND balance >= 1000000000");

        if (dataNames.length > 1) {
            sb.append(" AND (");
            for (String dataname : dataNames) {
                sb.append(" dataname ILIKE '").append(dataname).append("' OR");
            }

            // Cut trailing " OR" and close the AND clause
            sb.delete(sb.length() - 3, sb.length());
            sb.append(")");
        } else {
            if (!dataNames[0].equals("*")) {
                sb.append(" AND dataname ILIKE '").append(dataNames[0]).append("'");
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
    public Long getBalance(final String accountId) throws SQLException {
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

            final long result = inflationRs.getLong("balance");

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
