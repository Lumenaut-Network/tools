package com.lumenaut.poolmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 10/01/2018 - 4:38 PM
 */
public class FederationNetwork {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // JSON object OBJECT_MAPPER
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    private FederationNetwork() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Retrieve the inflation pool data from https://fed.network/inflation/
     *
     * @param inflationDestinationAccount The accountId of the inflation destination
     * @return
     */
    public static String getInflationPoolData(final String inflationDestinationAccount) throws Exception {
        // HTTP connection
        final String fedUrl = String.format("https://fed.network/inflation/%s", inflationDestinationAccount);
        final URL url = new URL(fedUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Attempt connection
        connection.setRequestMethod("GET");
        connection.setReadTimeout(15 * 1000);
        connection.connect();

        // Read response body
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder stringBuilder = new StringBuilder();
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            stringBuilder.append(responseLine).append("\n");
        }

        // Json object OBJECT_MAPPER
        if (stringBuilder.length() > 0) {
            // Read in the JSON
            final JsonNode jsonNode = OBJECT_MAPPER.readTree(stringBuilder.toString());

            // Format and return
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        }

        return null;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

