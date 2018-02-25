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
        private long totalvotes;
        private long totalpayments;
        private long totalfees;
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

        public int getDonations() {
            return donations;
        }

        public void setDonations(int donations) {
            this.donations = donations;
        }

        public long getTotalvotes() {
            return totalvotes;
        }

        public void setTotalvotes(long totalvotes) {
            this.totalvotes = totalvotes;
        }

        public long getTotalpayments() {
            return totalpayments;
        }

        public void setTotalpayments(long totalpayments) {
            this.totalpayments = totalpayments;
        }

        public long getTotalfees() {
            return totalfees;
        }

        public void setTotalfees(long totalfees) {
            this.totalfees = totalfees;
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
        private long amount;
        private String destination;
        private String reroutedfrom = "";   // Optional
        private boolean donation = false;   // Optional

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

        public String getReroutedfrom() {
            return reroutedfrom;
        }

        public void setReroutedfrom(String reroutedfrom) {
            this.reroutedfrom = reroutedfrom;
        }

        public boolean isDonation() {
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
        private String paidTotal;
        private String totalfees;
        private String totalpayment;
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

        public String getPaidTotal() {
            return paidTotal;
        }

        public void setPaidTotal(String paidTotal) {
            this.paidTotal = paidTotal;
        }

        public String getTotalfees() {
            return totalfees;
        }

        public void setTotalfees(String totalfees) {
            this.totalfees = totalfees;
        }

        public String getTotalpayment() {
            return totalpayment;
        }

        public void setTotalpayment(String totalpayment) {
            this.totalpayment = totalpayment;
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
        private long amount;
        private String destination;
        private String reroutedfrom = "";       // Optional
        private boolean donation = false;       // Optional

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
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

        public String getReroutedfrom() {
            return reroutedfrom;
        }

        public void setReroutedfrom(String reroutedfrom) {
            this.reroutedfrom = reroutedfrom;
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
        private int numdonations;
        private List<DonationDataEntry> donations;
        private int numerrors;
        private List<DonationErrorEntry> errors;

        public int getNumdonations() {
            return numdonations;
        }

        public void setNumdonations(int numdonations) {
            this.numdonations = numdonations;
        }

        public List<DonationDataEntry> getDonations() {
            return donations;
        }

        public void setDonations(List<DonationDataEntry> donations) {
            this.donations = donations;
        }

        public int getNumerrors() {
            return numerrors;
        }

        public void setNumerrors(int numerrors) {
            this.numerrors = numerrors;
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
        private String donationstring = "";
        private String errortype = "";

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDonationstring() {
            return donationstring;
        }

        public void setDonationstring(String donationstring) {
            this.donationstring = donationstring;
        }

        public String getErrortype() {
            return errortype;
        }

        public void setErrortype(String errortype) {
            this.errortype = errortype;
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

