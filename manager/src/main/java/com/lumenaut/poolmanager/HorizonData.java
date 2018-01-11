package com.lumenaut.poolmanager;

/**
 * @Author Burn
 * @Date 09/01/2018 - 11:03 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 */
public class HorizonData {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Networks
    public static final String HORIZON_LIVE_NETWORK = "https://horizon-testnet.stellar.org";
    public static final String HORIZON_TEST_NETWORK = "https://horizon.stellar.org";

    // Account creation
    public static class AccountCreationResponse {
        /**
         * Constructor
         */
        public AccountCreationResponse() {

        }

        /**
         * Constructor
         *
         * @param requestUrl
         * @param responseBody
         */
        public AccountCreationResponse(String requestUrl, String responseBody) {
            this.requestUrl = requestUrl;
            this.responseBody = responseBody;
        }

        // Data
        public String requestUrl;
        public String responseBody;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    private HorizonData() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
