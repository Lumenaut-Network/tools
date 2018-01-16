package com.lumenaut.poolmanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.util.ArrayList;
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
 * {"balance": BALANCE, "destination": "VOTER_ADDRESS"},
 * {"balance": BALANCE, "destination": "VOTER_ADDRESS"},
 * {"balance": BALANCE, "destination": "VOTER_ADDRESS"},
 * {"balance": BALANCE, "destination": "VOTER_ADDRESS"},
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

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region TRANSACTION RESPONSES

    public static class TransactionBatchResponse {
        public boolean success = false;
        public SubmitTransactionResponse transactionResponse;
        public ArrayList<String> errorMessages = new ArrayList<>();
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region INFLATION POOL VOTERS DATA STRUCTURE

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InflationData {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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
    //region TRANSACTIONS PLAN DATA STRUCTURE

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionPlan {
        private String uuid;
        private int excluded;
        private int rerouted;
        private long totalvotes;
        private long totalpayment;
        private List<TransactionPlanEntry> entries;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getExcluded() {
            return excluded;
        }

        public void setExcluded(int excluded) {
            this.excluded = excluded;
        }

        public int getRerouted() {
            return rerouted;
        }

        public void setRerouted(int rerouted) {
            this.rerouted = rerouted;
        }

        public long getTotalvotes() {
            return totalvotes;
        }

        public void setTotalvotes(long totalvotes) {
            this.totalvotes = totalvotes;
        }

        public long getTotalpayment() {
            return totalpayment;
        }

        public void setTotalpayment(long totalpayment) {
            this.totalpayment = totalpayment;
        }

        public List<TransactionPlanEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<TransactionPlanEntry> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionPlanEntry {
        private Long amount;
        private String destination;
        private String reroutedfrom;        // Optional

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getReroutedfrom() {
            return reroutedfrom;
        }

        public void setReroutedfrom(String reroutedfrom) {
            this.reroutedfrom = reroutedfrom;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region TRANSACTIONS RESULT DATA STRUCTURE

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionResult {
        private String uuid;
        private List<TransactionResultEntry> entries;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public List<TransactionResultEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<TransactionResultEntry> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionResultEntry {
        private Long timestamp;
        private Long amount;
        private String destination;
        private String reroutedfrom;        // Optional

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getReroutedfrom() {
            return reroutedfrom;
        }

        public void setReroutedfrom(String reroutedfrom) {
            this.reroutedfrom = reroutedfrom;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region EXCLUSIONS DATA STRUCTURE (MUST BE COMPATIBLE WITH TRANSACTION RESULTS)

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExclusionData {
        private List<ExclusionEntry> entries;

        public List<ExclusionEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<ExclusionEntry> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExclusionEntry {
        private String destination;

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region EXCLUSIONS DATA STRUCTURE (MUST BE COMPATIBLE WITH TRANSACTION RESULTS)

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReroutingData {
        private List<ReroutingDataEntry> entries;

        public List<ReroutingDataEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<ReroutingDataEntry> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReroutingDataEntry {
        private String account;
        private String reroute;

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getReroute() {
            return reroute;
        }

        public void setReroute(String reroute) {
            this.reroute = reroute;
        }
    }

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

