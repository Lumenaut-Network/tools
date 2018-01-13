package com.lumenaut.poolmanager;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 11/01/2018 - 2:22 PM
 * <p>
 * This class allows the json parser to serialize and deserialize the inflation data seamlessly. The data source format
 * is JSON (same as the one used by https://fed.network) and its structure is the following:
 * <p>
 * {
 * "inflationdest": "POOL_ADDRESS",
 * "entries": [
 * {"balance": BALANCE, "account": "VOTER_ADDRESS"},
 * {"balance": BALANCE, "account": "VOTER_ADDRESS"},
 * {"balance": BALANCE, "account": "VOTER_ADDRESS"},
 * {"balance": BALANCE, "account": "VOTER_ADDRESS"},
 * ...
 * ...
 * ...
 * ]
 * }
 */
public class DataFormats {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // JSON object OBJECT_MAPPER
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Object mapper configuration
    static {
        OBJECT_MAPPER.configure(Feature.ALLOW_COMMENTS, true);
    }

    // Inflation data root
    public static class InflationDataRoot {
        private String inflationdest;
        private List<InflationDataEntry> entries;

        public String getInflationdest() {
            return inflationdest;
        }

        public void setInflationdest(String inflationdest) {
            this.inflationdest = inflationdest;
        }

        public List<InflationDataEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<InflationDataEntry> entries) {
            this.entries = entries;
        }
    }

    // Inflation data entry
    public static class InflationDataEntry {
        private Long balance;
        private String account;

        public Long getBalance() {
            return balance;
        }

        public void setBalance(Long balance) {
            this.balance = balance;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region ACCESSORS

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    private DataFormats() {

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

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

