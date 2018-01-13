package com.lumenaut.poolmanager.gateways;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.lumenaut.poolmanager.DataFormats.OBJECT_MAPPER;
import static com.lumenaut.poolmanager.Settings.SETTING_FEDERATION_NETWORK_INFLATION_URL;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 10/01/2018 - 4:38 PM
 *
 * Stateless class, exposes quick methods to accomplish operations on "https://fed.network"
 */
public class FederationGateway {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Connection timeout
    private static final int HTTP_CONNECTION_TIMEOUT = 15 * 1000;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    private FederationGateway() {

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
    public static JsonNode getVoters(final String inflationDestinationAccount) throws IOException {
        // HTTP connection
        final String fedUrl = String.format(SETTING_FEDERATION_NETWORK_INFLATION_URL + "%s", inflationDestinationAccount);
        final URL url = new URL(fedUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Attempt connection
        connection.setRequestMethod("GET");
        connection.setReadTimeout(HTTP_CONNECTION_TIMEOUT);
        connection.connect();

        // Read response body
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder stringBuilder = new StringBuilder();
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            stringBuilder.append(responseLine).append("\n");
        }

        // Attempt to map to a JSON node object and return
        if (stringBuilder.length() > 0) {
            return OBJECT_MAPPER.readTree(stringBuilder.toString());
        }

        return null;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

