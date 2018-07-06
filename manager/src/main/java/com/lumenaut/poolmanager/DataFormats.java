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
    public static class VotersData {
        private String inflationdest;
        private long balance;
        private long inflation;
        private String created;
        private String expires;
        private String operation;
        private String txhash;
        private List<VoterDataEntry> entries;

        public String getInflationdest() {
            return inflationdest;
        }

        public void setInflationdest(String inflationdest) {
            this.inflationdest = inflationdest;
        }

        public long getBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }

        public long getInflation() {
            return inflation;
        }

        public void setInflation(long inflation) {
            this.inflation = inflation;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getExpires() {
            return expires;
        }

        public void setExpires(String expires) {
            this.expires = expires;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getTxhash() {
            return txhash;
        }

        public void setTxhash(String txhash) {
            this.txhash = txhash;
        }

        public List<VoterDataEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<VoterDataEntry> entries) {
            this.entries = entries;
        }

        /**
         * Reset this data structure
         */
        public void reset() {
            inflationdest = "";
            balance = 0L;
            inflation = 0L;
            created = "";
            expires = "";
            operation = "";
            txhash = "";

            if (entries == null) {
                entries = new ArrayList<>();
            } else {
                entries.clear();
            }

            // Issue a request for garbage collection, we're likely freeing a lot of memory here
            System.gc();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VoterDataEntry {
        private long balance;
        private String account;
        private List<VoterCustomDataEntry> data;

        public long getBalance() {
            return balance;
        }

        public void setBalance(long balance) {
            this.balance = balance;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public List<VoterCustomDataEntry> getData() {
            return data;
        }

        public void setData(List<VoterCustomDataEntry> data) {
            this.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VoterCustomDataEntry {
        private String dataname;
        private String datavalue;

        public String getDataname() {
            return dataname;
        }

        public void setDataname(String dataname) {
            this.dataname = dataname;
        }

        public String getDatavalue() {
            return datavalue;
        }

        public void setDatavalue(String datavalue) {
            this.datavalue = datavalue;
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
        private int donations;
        private int donationBeneficiaries;
        private long totalDonationsPayment;
        private long totalPoolDonations;
        private long totalVotes;
        private long totalPayouts;
        private long totalFees;
        private long totalPayment;
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

        public int getDonations() {
            return donations;
        }

        public void setDonations(int donations) {
            this.donations = donations;
        }

        public int getDonationBeneficiaries() {
            return donationBeneficiaries;
        }

        public void setDonationBeneficiaries(int donationBeneficiaries) {
            this.donationBeneficiaries = donationBeneficiaries;
        }

        public long getTotalDonationsPayment() {
            return totalDonationsPayment;
        }

        public void setTotalDonationsPayment(long totalDonationsPayment) {
            this.totalDonationsPayment = totalDonationsPayment;
        }

        public long getTotalPoolDonations() {
            return totalPoolDonations;
        }

        public void setTotalPoolDonations(long totalPoolDonations) {
            this.totalPoolDonations = totalPoolDonations;
        }

        public long getTotalVotes() {
            return totalVotes;
        }

        public void setTotalVotes(long totalVotes) {
            this.totalVotes = totalVotes;
        }

        public long getTotalPayouts() {
            return totalPayouts;
        }

        public void setTotalPayouts(long totalPayouts) {
            this.totalPayouts = totalPayouts;
        }

        public long getTotalFees() {
            return totalFees;
        }

        public void setTotalFees(long totalFees) {
            this.totalFees = totalFees;
        }

        public long getTotalPayment() {
            return totalPayment;
        }

        public void setTotalPayment(long totalPayment) {
            this.totalPayment = totalPayment;
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
        private long recordedBalance;
        private long amount;
        private long donatedAmount;
        private String destination;
        private String reroutedFrom = "";   // Optional
        private boolean donation = false;   // Optional

        public long getRecordedBalance() {
            return recordedBalance;
        }

        public void setRecordedBalance(long recordedBalance) {
            this.recordedBalance = recordedBalance;
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public long getDonatedAmount() {
            return donatedAmount;
        }

        public void setDonatedAmount(long donatedAmount) {
            this.donatedAmount = donatedAmount;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getReroutedFrom() {
            return reroutedFrom;
        }

        public void setReroutedFrom(String reroutedFrom) {
            this.reroutedFrom = reroutedFrom;
        }

        public boolean getDonation() {
            return donation;
        }

        public void setDonation(boolean donation) {
            this.donation = donation;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region TRANSACTIONS RESULT DATA STRUCTURE

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionResult {
        private String uuid;
        private int plannedOperations;
        private int executedOperations;
        private String totalPayout;
        private String totalFees;
        private String totalPayment;
        private String remainingPayment;
        private String resultOutcome;
        private List<TransactionResultEntry> entries;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getPlannedOperations() {
            return plannedOperations;
        }

        public void setPlannedOperations(int plannedOperations) {
            this.plannedOperations = plannedOperations;
        }

        public int getExecutedOperations() {
            return executedOperations;
        }

        public void setExecutedOperations(int executedOperations) {
            this.executedOperations = executedOperations;
        }

        public String getTotalPayout() {
            return totalPayout;
        }

        public void setTotalPayout(String totalPayout) {
            this.totalPayout = totalPayout;
        }

        public String getTotalFees() {
            return totalFees;
        }

        public void setTotalFees(String totalFees) {
            this.totalFees = totalFees;
        }

        public String getTotalPayment() {
            return totalPayment;
        }

        public void setTotalPayment(String totalPayment) {
            this.totalPayment = totalPayment;
        }

        public String getRemainingPayment() {
            return remainingPayment;
        }

        public void setRemainingPayment(String remainingPayment) {
            this.remainingPayment = remainingPayment;
        }

        public String getResultOutcome() {
            return resultOutcome;
        }

        public void setResultOutcome(String resultOutcome) {
            this.resultOutcome = resultOutcome;
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
        private long timestamp;
        private long recordedBalance;
        private long amount;
        private String destination;
        private String reroutedFrom = "";       // Optional
        private boolean donation = false;       // Optional

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getRecordedBalance() {
            return recordedBalance;
        }

        public void setRecordedBalance(long recordedBalance) {
            this.recordedBalance = recordedBalance;
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getReroutedFrom() {
            return reroutedFrom;
        }

        public void setReroutedFrom(String reroutedFrom) {
            this.reroutedFrom = reroutedFrom;
        }

        public boolean getDonation() {
            return donation;
        }

        public void setDonation(boolean donation) {
            this.donation = donation;
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
    //region REROUTING DATA STRUCTURE

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
    //region DONATIONS DATA STRUCTURE

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DonationsData {
        private int numDonations;
        private List<DonationDataEntry> donations;
        private int numErrors;
        private List<DonationErrorEntry> errors;

        public int getNumDonations() {
            return numDonations;
        }

        public void setNumDonations(int numDonations) {
            this.numDonations = numDonations;
        }

        public List<DonationDataEntry> getDonations() {
            return donations;
        }

        public void setDonations(List<DonationDataEntry> donations) {
            this.donations = donations;
        }

        public int getNumErrors() {
            return numErrors;
        }

        public void setNumErrors(int numErrors) {
            this.numErrors = numErrors;
        }

        public List<DonationErrorEntry> getErrors() {
            return errors;
        }

        public void setErrors(List<DonationErrorEntry> errors) {
            this.errors = errors;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DonationDataEntry {
        private String source = "";
        private String destination = "";
        private int percent = 0;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public int getPercent() {
            return percent;
        }

        public void setPercent(int percent) {
            this.percent = percent;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DonationErrorEntry {
        private String source = "";
        private String donationString = "";
        private String errorType = "";

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDonationString() {
            return donationString;
        }

        public void setDonationString(String donationString) {
            this.donationString = donationString;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
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

