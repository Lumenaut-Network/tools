package com.lumenaut.poolmanager;

import com.lumenaut.poolmanager.DataFormats.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.lumenaut.poolmanager.DataFormats.*;
import static com.lumenaut.poolmanager.Settings.*;
import static com.lumenaut.poolmanager.UIUtils.showError;
import static com.lumenaut.poolmanager.UIUtils.showInfo;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 12/01/2018 - 6:46 PM
 */
public class TransactionsController {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FXML BINDINGS

    ////////////////////////////////////////////////////////
    // UI

    @FXML
    private AnchorPane transactionPlannerStage;

    @FXML
    private TextArea transactionPlanTextArea;

    @FXML
    private TextArea exclusionsTextArea;

    @FXML
    private TextArea reroutingTextArea;

    @FXML
    private TextArea donationsTextArea;

    @FXML
    private TextField signingKeyTextField;

    @FXML
    private TextField inflationAmountTextField;

    ////////////////////////////////////////////////////////
    // BUTTONS

    @FXML
    private Button rebuildTransactionPlanBtn;

    @FXML
    private Button saveTransactionPlanBtn;

    @FXML
    private Button importTransactionPlanBtn;

    @FXML
    private Button saveExclusionsBtn;

    @FXML
    private Button saveReroutingBtn;

    @FXML
    private Button executeTransactionBtn;

    ////////////////////////////////////////////////////////
    // LABELS

    @FXML
    private Label excludedTransactionsLabel;

    @FXML
    private Label reroutedTransactionsLabel;

    @FXML
    private Label plannedTransactionsLabel;

    @FXML
    private Label plannedDonationsLabel;

    @FXML
    private Label plannedBeneficiariesLabel;

    @FXML
    private Label executedTransactionsLabel;

    @FXML
    private Label totalToBePaidLabel;

    @FXML
    private Label plannedDonationsAmountLabel;

    @FXML
    private Label totalFeesPaidLabel;

    @FXML
    private Label totalPaidLabel;

    @FXML
    private Label totalPoolDonationsLabel;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // File names
    public static final String DATA_EXCLUSIONS_JSON_PATH = "data/exclusions.json";
    public static final String DATA_REROUTING_JSON_PATH = "data/rerouting.json";
    public static final String DATA_DONATIONS_JSON_PATH = "data/donations.json";
    public static final String TRANSACTION_PLAN_JSON_SUFFIX = "transaction_plan.json";
    public static final String TRANSACTIONS_RESULT_JSON_SUFFIX = "transactions_result.json";
    public static final String TRANSACTION_SUCCESSFUL_JSON_SUFFIX = "transaction_successful.json";
    public static final String TRANSACTION_ERROR_JSON_SUFFIX = "transaction_error.json";
    public static final DateFormat FOLDER_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat FILE_DATE_FORMATTER = new SimpleDateFormat("HH'h'mm'm'ss's'");

    // Data bindings from the MainController
    public TextField poolAddressTextField;
    public VotersData currentVotersData;
    public BigDecimal currentPoolBalance;
    public AnchorPane primaryStage;
    public Stage window;

    // Data
    private HashMap<String, Long> votesAndBalances;
    private HashMap<String, Long> votesAndPayments;
    private TransactionPlan currentPlan;
    private DonationsData donationsData;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    public TransactionsController() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Update visual clues for the selected network
        switch (SETTING_OPERATIONS_NETWORK) {
            case "TEST":
                executeTransactionBtn.getStyleClass().removeAll("redBg");
                executeTransactionBtn.getStyleClass().add("greenBg");

                break;
            case "LIVE":
                executeTransactionBtn.getStyleClass().removeAll("greenBg");
                executeTransactionBtn.getStyleClass().add("redBg");

                break;
        }

        // Load default data for exclusions, rerouting and donations
        loadSavedExclusionsData();
        loadSavedReroutingData();
        loadDonationsData();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TEXTFIELD HANDLERS

        inflationAmountTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                validateInputAmountToPay();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // REBUILD PLAN HANDLER
        rebuildTransactionPlanBtn.setOnAction(event -> {
            // Update textarea
            transactionPlanTextArea.clear();
            transactionPlanTextArea.appendText("Building transactions plan... ");

            if (!validateInputAmountToPay()) {
                planFailed();

                return;
            }

            if (buildTransactionPlan()) {
                if (!validateAmountToPay()) {
                    planFailed();

                    return;
                }

                planningSuccessful(false);
            } else {
                planFailed();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // EXECUTE HANDLER
        executeTransactionBtn.setOnAction(event -> {
            // Check signature
            final String signingKey = signingKeyTextField.getText();
            if (!XLMUtils.isSecretKeyValidFormat(signingKey)) {
                showError("The signing key specified cannot be a valid signing key!");

                return;
            }

            // Check the transaction plan has a positive amount to pay
            if (currentPlan == null || currentPlan.getTotalPayouts() <= 0) {
                showError("The current transaction plan has nothing to pay!");

                return;
            }

            // Check if we have a signign key
            if (signingKey.isEmpty()) {
                showError("You must specify the signing key for the transactions");
            } else {
                // Save current transactions plan, abort if it fails
                if (!saveTransactionPlan(true)) {
                    return;
                }

                // Start the payment process
                try {
                    // Create new stage
                    final Stage stage = new Stage();
                    final FXMLLoader fxmlLoader = new FXMLLoader();
                    fxmlLoader.setLocation(getClass().getResource("/inflationManagerProcessing.fxml"));
                    final AnchorPane transactionsProcessingFrame = fxmlLoader.load();
                    final ProcessingController processingController = fxmlLoader.getController();

                    // Bind references in the settings controller
                    processingController.primaryStage = primaryStage;
                    processingController.transactionPlan = currentPlan;
                    processingController.executeTransactionBtn = executeTransactionBtn;
                    processingController.executedTransactionsLabel = executedTransactionsLabel;
                    processingController.rebuildTransactionPlanBtn = rebuildTransactionPlanBtn;
                    processingController.poolAddress = poolAddressTextField.getText();
                    processingController.signingKey = signingKey;

                    // Initialize the transactions stage and show it
                    stage.setTitle("Transactions progress");
                    stage.setScene(new Scene(transactionsProcessingFrame));
                    stage.getIcons().add(new Image(Main.class.getResourceAsStream("/inflationManager.png")));
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.initOwner(transactionPlannerStage.getScene().getWindow());
                    stage.initStyle(StageStyle.UNDECORATED);
                    stage.setWidth(primaryStage.getWidth() + 80);
                    stage.setHeight(primaryStage.getHeight() + 130);

                    // Open
                    stage.show();
                } catch (IOException e) {
                    showError(e.getMessage());
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // OTHER HANDLERS
        saveExclusionsBtn.setOnAction(event -> saveExclusionsData());
        saveReroutingBtn.setOnAction(event -> saveReroutingData());
        saveTransactionPlanBtn.setOnAction(event -> saveTransactionPlan(false));
        importTransactionPlanBtn.setOnAction(event -> importTransactionPlan(false));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TOOLTIPS
        saveExclusionsBtn.setTooltip(new Tooltip("Save the current exclusions data, it will be loaded as the new default next time you run the program"));
        saveReroutingBtn.setTooltip(new Tooltip("Save the current rerouting data, it will be loaded as the new default next time you run the program"));
        saveTransactionPlanBtn.setTooltip(new Tooltip("Save the current transaction plan data for future reference"));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // RUN AFTER INIT

        // Extract donations data (if present)
        Platform.runLater(() -> {
            donationsData = getDonationsData();

            // Notify the user
            if (donationsData != null && (donationsData.getNumDonations() != 0 || donationsData.getNumErrors() != 0)) {
                // Replace the contents of the donations panel
                try {
                    // Write the data to the donations tab
                    donationsTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(donationsData));

                    // Notify the user of results
                    showInfo("Found and imported donations' data. You can inspect the results in the \"Donations\" tab!\n\nNumber of valid donations found: " + donationsData.getNumDonations() + "\nInvalid donations format found: " + donationsData.getNumErrors());
                } catch (Exception e) {
                    showError("Error occurred while converting donations data to its JsonFormat: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Updates the planner ui with the data from the currently planned transactions
     */
    private void planningSuccessful(boolean imported) {
        // Update textarea
        if (!imported) {
            transactionPlanTextArea.appendText(" DONE!\nREADY TO EXECUTE!");
        } else {
            transactionPlanTextArea.appendText("TRANSACTION PLAN IMPORT DONE! >>> IMPORTANT <<< THE PLAN WILL EXECUTE AS IT WAS BUILT AND WILL NOT PROCESS EXCLUSIONS AGAIN !!!\n\nREADY TO EXECUTE!");
        }

        // Update planned total operations
        plannedTransactionsLabel.setText(String.valueOf(currentPlan.getEntries().size()));

        // Update total amount to pay
        totalToBePaidLabel.setText(XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalPayouts()) + " XLM");
        totalFeesPaidLabel.setText(XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalFees()) + " XLM");
        totalPaidLabel.setText(XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalPayment()) + " XLM");
        plannedDonationsAmountLabel.setText(XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalDonationsPayment()) + " XLM");
        totalPoolDonationsLabel.setText(XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalPoolDonations()) + " XLM");

        // Update the rerouting and exclusion labels
        reroutedTransactionsLabel.setText(String.valueOf(currentPlan.getRerouted()));
        excludedTransactionsLabel.setText(String.valueOf(currentPlan.getExcluded()));

        // Update donations labels
        plannedDonationsLabel.setText(String.valueOf(currentPlan.getDonations()));
        plannedBeneficiariesLabel.setText(String.valueOf(currentPlan.getDonationBeneficiaries()));

        // Activate execution
        executeTransactionBtn.setDisable(false);
        executeTransactionBtn.setText("EXECUTE");
    }

    /**
     * Reset the current transaction plan and all the counters
     */
    private void planFailed() {
        // Update textarea
        transactionPlanTextArea.appendText(" FAILED!");

        // Update
        plannedTransactionsLabel.setText("0");
        executedTransactionsLabel.setText("0");
        reroutedTransactionsLabel.setText("0");
        totalToBePaidLabel.setText("0 XLM");
        totalFeesPaidLabel.setText("0 XLM");
        totalPaidLabel.setText("0 XLM");
        plannedDonationsAmountLabel.setText("0 XLM");
        totalPoolDonationsLabel.setText("0 XLM");
        executeTransactionBtn.setDisable(true);
        currentPlan = null;
    }

    /**
     * Attempt extraction of donations data from voters data
     *
     * @return DonationsData can be null or have no entries
     */
    private DonationsData getDonationsData() {
        // Get the pool address
        final String poolAddress = poolAddressTextField.getText();

        // Check pool address
        if (poolAddress == null || poolAddress.isEmpty() || !XLMUtils.isPublicKeyValidFormat(poolAddress)) {
            return null;
        }

        try {
            final VotersData votersData = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(currentVotersData), VotersData.class);

            // Ignore if voters data has no entries at all
            if (votersData.getEntries().size() > 0) {
                // Prepare result object
                final DonationsData donationsData = new DonationsData();
                donationsData.setDonations(new ArrayList<>());
                donationsData.setErrors(new ArrayList<>());

                for (VoterDataEntry entry : votersData.getEntries()) {
                    // Voter address
                    final String voterAddress = entry.getAccount();

                    // Check for custom data that is relevant to us
                    final List<VoterCustomDataEntry> voterCustomData = entry.getData();
                    if (voterCustomData != null) {
                        for (VoterCustomDataEntry customDataEntry : voterCustomData) {
                            if (customDataEntry.getDataname() != null && customDataEntry.getDatavalue() != null) {
                                final String dataName = customDataEntry.getDataname().toLowerCase();
                                if (dataName.startsWith(SETTING_DONATION_DATANAME_PREFIX)) {
                                    // This voter is donating a % of his inflation to someone
                                    final String dataValue = customDataEntry.getDatavalue();
                                    final String[] tokens = dataValue.split("%");
                                    if (tokens.length == 2) {
                                        final String percent = tokens[0];
                                        final String destinationAddress = tokens[1];

                                        // Exclude if the voter is donating to itself
                                        if (voterAddress.equals(destinationAddress)) {
                                            // Invalid destination
                                            final DonationErrorEntry donationErrorEntry = new DonationErrorEntry();
                                            donationErrorEntry.setSource(voterAddress);
                                            donationErrorEntry.setDonationString(dataValue);
                                            donationErrorEntry.setErrorType("Invalid destination address, donating to self");

                                            donationsData.getErrors().add(donationErrorEntry);

                                            continue;
                                        }

                                        if (XLMUtils.isPublicKeyValidFormat(destinationAddress)) {
                                            try {
                                                // Attempt parsing the donation %
                                                int intPercent = Integer.parseInt(percent);

                                                // Clamp values between 0 and 100
                                                if (intPercent < 0) {
                                                    intPercent = 0;
                                                }

                                                if (intPercent > 100) {
                                                    intPercent = 100;
                                                }

                                                // Create a new donation entry
                                                final DonationDataEntry donationDataEntry = new DonationDataEntry();
                                                donationDataEntry.setSource(voterAddress);
                                                donationDataEntry.setDestination(destinationAddress);
                                                donationDataEntry.setPercent(intPercent);

                                                // Append
                                                donationsData.getDonations().add(donationDataEntry);
                                            } catch (NumberFormatException e) {
                                                // Failed to parse percent amount
                                                final DonationErrorEntry donationErrorEntry = new DonationErrorEntry();
                                                donationErrorEntry.setSource(voterAddress);
                                                donationErrorEntry.setDonationString(dataValue);
                                                donationErrorEntry.setErrorType("Invalid percentage specified");

                                                donationsData.getErrors().add(donationErrorEntry);
                                            }
                                        } else {
                                            // Invalid destination
                                            final DonationErrorEntry donationErrorEntry = new DonationErrorEntry();
                                            donationErrorEntry.setSource(voterAddress);
                                            donationErrorEntry.setDonationString(dataValue);
                                            donationErrorEntry.setErrorType("Invalid destination address");

                                            donationsData.getErrors().add(donationErrorEntry);
                                        }
                                    } else {
                                        // Invalid format
                                        final DonationErrorEntry donationErrorEntry = new DonationErrorEntry();
                                        donationErrorEntry.setSource(voterAddress);
                                        donationErrorEntry.setDonationString(dataValue);
                                        donationErrorEntry.setErrorType("Invalid donation string format");

                                        donationsData.getErrors().add(donationErrorEntry);
                                    }
                                }
                            }
                        }

                        // Update donations data instance counters
                        donationsData.setNumDonations(donationsData.getDonations().size());
                        donationsData.setNumErrors(donationsData.getErrors().size());
                    }
                }

                // Return donations data object
                return donationsData;
            } else {
                // No data
                showError("Voters data is empty, cannot extract donations");

                return null;
            }
        } catch (IOException e) {
            // No data
            showError("Voters data format is invalid: " + e.getMessage());

            return null;
        }
    }

    /**
     * Find a destination folder for this run, keep appending numbers (#) to it until a non used folder is found
     *
     * @param folderPath
     * @param attempt
     * @return
     */
    private String findAvailableFolderName(final String folderPath, int attempt) {
        // We want to start appending from the number 2, if 0 is passed at first attempt bump it up
        if (attempt == 0) {
            attempt++;
        }

        // Dig down
        if (attempt == 1) {
            final File destinationDir = new File(folderPath);
            if (destinationDir.exists()) {
                return findAvailableFolderName(folderPath, attempt + 1);
            } else {
                return folderPath;
            }
        } else {
            final String targetFolder = folderPath + " (" + attempt + ")";
            final File destinationDir = new File(targetFolder);
            if (destinationDir.exists()) {
                return findAvailableFolderName(folderPath, attempt + 1);
            } else {
                return targetFolder;
            }
        }
    }

    /**
     * Build a new transactions plan
     */
    private boolean buildTransactionPlan() {
        if (inflationAmountTextField.getText().isEmpty()) {
            showError("Cannot build transaction plan without specifying the amount that you wish to be distributed. Please fill in the \"Inflation Amount\" field.");

            return false;
        } else if (currentPoolBalance == null || currentVotersData == null) {
            showError("Cannot build transaction plan, voters data or pool balance missing, close this transaction planner and fetch new data");

            return false;
        } else {
            // Output folder
            final String outputFolder = findAvailableFolderName("data/" + FOLDER_DATE_FORMATTER.format(new Date()), 0);

            // Init payments rerouting and exclusion counters
            final AtomicInteger numRerouted = new AtomicInteger(0);
            final AtomicInteger numExcluded = new AtomicInteger(0);
            final AtomicInteger numDonations = new AtomicInteger(0);
            final AtomicInteger numDonationBeneficiaries = new AtomicInteger(0);
            final AtomicLong totalVoterPayout = new AtomicLong(0);
            final AtomicLong totalDonationsPayout = new AtomicLong(0);
            final AtomicLong totalPoolDonations = new AtomicLong(0);
            final AtomicLong totalFees = new AtomicLong(0);
            final AtomicLong totalTransactionsPayment = new AtomicLong(0);


            // Init special trackers for exclusions
            final AtomicBoolean excludedPoolSelfVote = new AtomicBoolean(false);
            final AtomicInteger excludedNegativePayments = new AtomicInteger(0);
            final AtomicInteger excludedLowBalancePayments = new AtomicInteger(0);

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // STEP 1: Prepare voters data (voters, balances, donations)
            if (votesAndBalances == null) {
                votesAndBalances = new HashMap<>();
            } else {
                votesAndBalances.clear();
            }

            // Get the pool address
            final String poolAddress = poolAddressTextField.getText();

            // Check pool address
            if (poolAddress == null || poolAddress.isEmpty() || !XLMUtils.isPublicKeyValidFormat(poolAddress)) {
                showError("Pool address missing or invalid");

                return false;
            }

            // Prepare voters data structure
            if (currentVotersData.getEntries().size() > 0) {
                for (VoterDataEntry entry : currentVotersData.getEntries()) {
                    // Pull balance and account data
                    final Long balance = entry.getBalance();
                    final String voterAddress = entry.getAccount();

                    // !!! Important !!!
                    // Exclude the pool's own vote, this will prevent the pool balance (which includes the inflation amount)
                    // from adding itself to the computations on payments.
                    if (voterAddress.equals(poolAddress)) {
                        // Exclude
                        numExcluded.getAndIncrement();

                        // Flag pool exclusion
                        excludedPoolSelfVote.set(true);

                        // Skip it
                        continue;
                    }

                    // Append to the votes list
                    votesAndBalances.put(voterAddress, balance);
                }
            }

            // !!! IMPORTANT !!!
            // Total votes balance is always computed BEFORE negative payout exclusions
            long totalVotesAmount = 0L;
            for (long voterBalance : votesAndBalances.values()) {
                totalVotesAmount += voterBalance;
            }

            // Parse inflation amount
            final String inflationAmountString = inflationAmountTextField.getText();
            long inflationAmount;
            if (XLMUtils.isStroopFormat(inflationAmountString)) {
                inflationAmount = Long.parseLong(inflationAmountString);
            } else if (XLMUtils.isDecimalFormat(inflationAmountString)) {
                inflationAmount = XLMUtils.XLMToStroop(XLMUtils.decimalStringToXLM(inflationAmountString));
            } else {
                showError("Invalid inflation amount: " + inflationAmountString);

                return false;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // STEP 2: Prepare voters and their payouts
            if (votesAndPayments == null) {
                votesAndPayments = new HashMap<>();
            } else {
                votesAndPayments.clear();
            }

            // Populate it
            for (HashMap.Entry<String, Long> voter : votesAndBalances.entrySet()) {
                final String voterAddres = voter.getKey();
                final long voterBalance = voter.getValue();
                final long voterPayment = computeVoterPayout(inflationAmount, totalVotesAmount, voterBalance);

                // Only append if the actual payment is positive (if the fees are higher than the payout it can happen)
                if (voterPayment > 0) {
                    votesAndPayments.put(voterAddres, voterPayment);
                } else {
                    numExcluded.getAndIncrement();
                    excludedNegativePayments.getAndIncrement();
                }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // STEP 3: Process exclusions and rerouting

            // Get exclusion data
            ExclusionData exclusionData;
            try {
                if (!exclusionsTextArea.getText().isEmpty()) {
                    exclusionData = OBJECT_MAPPER.readValue(exclusionsTextArea.getText(), ExclusionData.class);
                } else {
                    exclusionData = null;
                }
            } catch (IOException e) {
                showError("Cannot parse exclusion data: " + e.getMessage());

                return false;
            }


            // Get rerouting data
            ReroutingData reroutingData;
            try {
                if (!reroutingTextArea.getText().isEmpty()) {
                    reroutingData = OBJECT_MAPPER.readValue(reroutingTextArea.getText(), ReroutingData.class);
                } else {
                    reroutingData = null;
                }
            } catch (IOException e) {
                showError("Cannot parse rerouting data: " + e.getMessage());

                return false;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // STEP 4: Build the transaction plan

            // Generate UUID
            final String uuid = UUID.randomUUID().toString();

            // Build new transaction plan
            final TransactionPlan newPlan = new TransactionPlan();
            newPlan.setEntries(new LinkedList<>());
            newPlan.setUuid(uuid);
            newPlan.setOut(outputFolder);

            // Set total votes balance
            newPlan.setTotalVotes(totalVotesAmount);

            // Set input amount
            newPlan.setInputAmount(inflationAmountString);

            // Get donations entries
            final List<DonationDataEntry> donationDataEntries = donationsData.getDonations();

            // Prepare donations temporary buffer
            final HashMap<String, Long> donationBeneficiariesAndAmounts = new HashMap<>();

            // Compute plan entries
            votesAndPayments.forEach((voterAccount, voterAmount) -> {
                // Get the recorded balance for this voter
                final Long recordedVoterBalance = votesAndBalances.get(voterAccount);

                // Create new entry node
                final TransactionPlanEntry voterPaymentEntry = new TransactionPlanEntry();
                voterPaymentEntry.setRecordedBalance(recordedVoterBalance);
                voterPaymentEntry.setAmount(voterAmount);
                voterPaymentEntry.setDestination(voterAccount);

                // Reroute if needed
                if (reroutingData != null) {
                    for (ReroutingDataEntry reroutingDataEntry : reroutingData.getEntries()) {
                        if (voterAccount.equals(reroutingDataEntry.getAccount())) {
                            // Reroute
                            voterPaymentEntry.setDestination(reroutingDataEntry.getReroute());
                            voterPaymentEntry.setReroutedFrom(voterAccount);

                            // Update rerouted counter
                            numRerouted.getAndIncrement();
                        }
                    }
                }

                // If this account (or its rerouted address) is in the exclusion list skip appending it
                if (exclusionData != null) {
                    for (ExclusionEntry exclusionEntry : exclusionData.getEntries()) {
                        if (exclusionEntry.getDestination().equals(voterPaymentEntry.getReroutedFrom())) {
                            numExcluded.getAndIncrement();

                            // Return immediately without appending the entry
                            return;
                        }

                        if (exclusionEntry.getDestination().equals(voterPaymentEntry.getDestination())) {
                            numExcluded.getAndIncrement();

                            // Return immediately without appending the entry
                            return;
                        }
                    }
                }

                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // STEP 5 (executes per voter): Scan donations data for a matching voter sourceAccount
                boolean voterHasPaymentLeft = true;
                for (DonationDataEntry donationEntry : donationDataEntries) {
                    if (donationEntry.getSource().equals(voterAccount)) {
                        // Extract donation data and compute donation amount
                        final String beneficiary = donationEntry.getDestination();

                        // Percent to donate
                        final int percent = donationEntry.getPercent();

                        // Calculate donation amount (voterAmount / 100 * percent donated)
                        final BigDecimal voterAmountXLM = XLMUtils.stroopToXLM(voterAmount);
                        final BigDecimal voterAmountOnePercent = voterAmountXLM.divide(new BigDecimal(100), ROUNDING_MODE);
                        final BigDecimal donationAmount = voterAmountOnePercent.multiply(new BigDecimal(percent));

                        // Convert donation amount back to stroops and substract its transaction fee (the donation is going to be a new operation)
                        final long donationAmountStroops = XLMUtils.XLMToStroop(donationAmount) - SETTING_FEE;

                        // Check if the voter amount minus the donation amount leaves enough in the voter's payment
                        if (voterAmount - donationAmountStroops <= 0L) {
                            // Remove voter from the payment plan, he's left with nothing to be paid
                            voterHasPaymentLeft = false;
                        } else {
                            // Deduct donation from voter amount and set the donated amount entry
                            voterPaymentEntry.setAmount(voterAmount - donationAmountStroops);
                            voterPaymentEntry.setDonatedAmount(donationAmountStroops);
                        }

                        // Add a new entry to the beneficiaries or update an existing one
                        if (donationBeneficiariesAndAmounts.containsKey(beneficiary)) {
                            // Increment existing beneficiary amount
                            final Long newBeneficiaryAmount = donationBeneficiariesAndAmounts.get(beneficiary) + donationAmountStroops;

                            // Update entry
                            donationBeneficiariesAndAmounts.put(beneficiary, newBeneficiaryAmount);
                        } else {
                            // Insert a new beneficiary
                            donationBeneficiariesAndAmounts.put(beneficiary, donationAmountStroops);

                            // Update beneficiaries count
                            numDonationBeneficiaries.getAndIncrement();
                        }

                        // Update donations counter
                        numDonations.getAndIncrement();
                    }
                }

                // Check if the voter has any payment left after donating, if not, return immediately without appending his entry
                if (!voterHasPaymentLeft) {
                    numExcluded.getAndIncrement();

                    return;
                }

                // Update total payment up until this point
                totalFees.getAndAdd(SETTING_FEE);
                totalVoterPayout.getAndAdd(voterPaymentEntry.getAmount());
                totalTransactionsPayment.getAndAdd(voterPaymentEntry.getAmount() + SETTING_FEE);

                // Append to the entries
                newPlan.getEntries().add(voterPaymentEntry);
            });

            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // STEP 6: Add donation beneficiaries entries to the plan
            donationBeneficiariesAndAmounts.forEach((beneficiary, donationsAmount) -> {
                // If the destination is the pool itself, don't generate the transaction, the payment will simply remain in the pool's balance
                if (beneficiary.equals(poolAddress)) {
                    // Only update the total donations
                    totalDonationsPayout.getAndAdd(donationsAmount);
                    totalPoolDonations.getAndAdd(donationsAmount);
                } else {
                    // Create new entry node
                    final TransactionPlanEntry beneficiaryEntry = new TransactionPlanEntry();

                    // Append transaction
                    beneficiaryEntry.setDestination(beneficiary);
                    beneficiaryEntry.setAmount(donationsAmount);
                    beneficiaryEntry.setDonation(true);

                    // Update payment counts
                    totalFees.getAndAdd(SETTING_FEE);
                    totalDonationsPayout.getAndAdd(donationsAmount);
                    totalTransactionsPayment.getAndAdd(donationsAmount + SETTING_FEE);

                    // Append to the entries
                    newPlan.getEntries().add(beneficiaryEntry);
                }
            });

            // Update computed totals
            newPlan.setRerouted(numRerouted.get());
            newPlan.setExcluded(numExcluded.get());
            newPlan.setTotalPayouts(totalVoterPayout.get());
            newPlan.setTotalFees(totalFees.get());
            newPlan.setDonations(numDonations.get());
            newPlan.setDonationBeneficiaries(numDonationBeneficiaries.get());
            newPlan.setTotalDonationsPayment(totalDonationsPayout.get());
            newPlan.setTotalPoolDonations(totalPoolDonations.get());
            newPlan.setTotalPayment(totalTransactionsPayment.get());

            // Update the transaction plan
            currentPlan = newPlan;

            // Notify of pool exclusion
            if (excludedPoolSelfVote.get()) {
                showInfo("The pool is voting for itself, it has been excluded from the payment plan!");
            }

            // Notify of negative payments exclusions
            if (excludedNegativePayments.get() > 0) {
                showInfo(excludedNegativePayments.get() + " accounts have been excluded from the distribution because their payment amounts minus the fee would be negative");
            }

            // Notify of low balance exclusions
            if (excludedLowBalancePayments.get() > 0) {
                showInfo(excludedLowBalancePayments.get() + " accounts have been excluded from the distribution because their balance was below the minimum network threshold of 100XLM");
            }

            // Notify of pool donations
            if (currentPlan.getTotalPoolDonations() > 0) {
                // Inform the user that the pool has received donations
                showInfo("The pool has received a total donation of: " + XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalPoolDonations()) + " XLM");
            }

            // Plan ready
            return true;
        }
    }

    /**
     * Compute the amount of inflation this account should be sent
     *
     * @param inflationAmountToPay
     * @param totalVotesAmount
     * @param voterBalance
     * @return
     */
    private long computeVoterPayout(final long inflationAmountToPay, final long totalVotesAmount, final long voterBalance) {
        // Constants in the payout calculation
        final BigDecimal totalInflation = XLMUtils.stroopToXLM(inflationAmountToPay).setScale(20, ROUNDING_MODE);
        final BigDecimal totalBalance = XLMUtils.stroopToXLM(totalVotesAmount).setScale(20, ROUNDING_MODE);
        final BigDecimal grossVoterBalance = XLMUtils.stroopToXLM(voterBalance).setScale(20, ROUNDING_MODE);
        final BigDecimal fee = XLMUtils.stroopToXLM(SETTING_FEE).setScale(20, ROUNDING_MODE);

        // Percent of the total balance represented by the voter
        final BigDecimal voterPercentOfTotalBalance = grossVoterBalance.divide(totalBalance, ROUNDING_MODE);

        // Amount to pay
        final BigDecimal payout = totalInflation.multiply(voterPercentOfTotalBalance).subtract(fee);

        // Convert back to stroops
        return XLMUtils.XLMToStroop(payout);
    }

    /**
     * Load the existing exclusions file
     */
    private boolean loadSavedExclusionsData() {
        final StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_EXCLUSIONS_JSON_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
        } catch (IOException ignore) {
            return false;
        }

        // Update the runtime json object
        try {
            final ExclusionData exclusionDataData = OBJECT_MAPPER.readValue(contents.toString(), ExclusionData.class);
            exclusionsTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(exclusionDataData));
        } catch (IOException e) {
            showError("Exclusions list format error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Saves the exclusions data currently present in the text area
     */
    private void saveExclusionsData() {
        // Read the current contents of the text area
        final String contents = exclusionsTextArea.getText();

        // Try to decode them to see if they are in a valid format
        try {
            OBJECT_MAPPER.readValue(contents, ExclusionData.class);
        } catch (IOException e) {
            showError("Exclusions list format error: " + e.getMessage());

            return;
        }

        // Save to file
        try (
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(DATA_EXCLUSIONS_JSON_PATH), "UTF-8");
        BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(contents);

            showInfo("The current exclusions data has been saved and is now the new default");
        } catch (IOException e) {
            showError("Cannot write exclusions list file [" + System.getProperty("user.dir") + "/" + DATA_EXCLUSIONS_JSON_PATH + "]: " + e.getMessage());
        }
    }

    /**
     * Load the existing exclusions file
     */
    private boolean loadSavedReroutingData() {
        final StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_REROUTING_JSON_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
        } catch (IOException ignore) {
            return false;
        }

        // Update the runtime json object
        try {
            final ReroutingData reroutingData = OBJECT_MAPPER.readValue(contents.toString(), ReroutingData.class);
            reroutingTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(reroutingData));
        } catch (IOException e) {
            showError("Exclusions list format error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Saves the rerouting data currently present in the text area
     */
    private void saveReroutingData() {
        // Read the current contents of the text area
        final String contents = reroutingTextArea.getText();

        // Try to decode them to see if they are in a valid format
        try {
            OBJECT_MAPPER.readValue(contents, ReroutingData.class);
        } catch (IOException e) {
            showError("Rerouting list format error: " + e.getMessage());

            return;
        }

        // Save to file
        try (
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(DATA_REROUTING_JSON_PATH), "UTF-8");
        BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(contents);

            showInfo("The current rerouting data has been saved and is now the new default");
        } catch (IOException e) {
            showError("Cannot write Rerouting list file [" + System.getProperty("user.dir") + "/" + DATA_REROUTING_JSON_PATH + "]: " + e.getMessage());
        }
    }

    /**
     * Load the existing donations file
     */
    private boolean loadDonationsData() {
        final StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_DONATIONS_JSON_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
        } catch (IOException ignore) {
            return false;
        }

        // Update the runtime json object
        try {
            final DonationsData donationsData = OBJECT_MAPPER.readValue(contents.toString(), DonationsData.class);
            donationsTextArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(donationsData));
        } catch (IOException e) {
            showError("Donations list format error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Saves the current transaction plan
     */
    private boolean saveTransactionPlan(boolean quietMode) {
        // Check if the current plan is valid
        if (currentPlan == null) {
            showInfo("Nothing to save. Build a transaction plan first!");

            return false;
        }

        // Try to decode them to see if they are in a valid format
        final String contents;
        try {
            contents = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(currentPlan);
        } catch (IOException e) {
            showError("Transaction plan format error: " + e.getMessage());

            return false;
        }

        // Create folder if missing
        final String destinationFolder = currentPlan.getOut();
        final String destinationFileName = FILE_DATE_FORMATTER.format(new Date()) + "_" + TRANSACTION_PLAN_JSON_SUFFIX;
        final File destinationDir = new File(destinationFolder);
        boolean destinationReady;
        if (!destinationDir.exists()) {
            destinationReady = destinationDir.mkdirs();
        } else {
            destinationReady = true;
        }

        // Save to file
        if (destinationReady) {
            final String outPutFilePath = destinationFolder + "/" + destinationFileName;
            try (
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outPutFilePath), StandardCharsets.UTF_8);
            BufferedWriter bufWriter = new BufferedWriter(writer)
            ) {
                bufWriter.write(contents);
            } catch (IOException e) {
                showError("Cannot write transaction plan file [" + outPutFilePath + "]: " + e.getMessage());

                return false;
            }

            // Show info
            if (!quietMode) {
                showInfo("The current transaction plan has been saved in the following file: " + outPutFilePath);
            }
        } else {
            showError("Cannot write transaction plan file, unable to create folder: " + destinationFolder);

            return false;
        }

        return true;
    }

    private boolean importTransactionPlan(boolean quietMode) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import saved transaction plan");
        fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("JSON", "*.json")
        );

        final File file = fileChooser.showOpenDialog(window);

        final TransactionPlan importedPlan;
        try {
            importedPlan = OBJECT_MAPPER.readValue(file, TransactionPlan.class);
        } catch (Exception e) {
            planFailed();
            showError("The selected file does not contain a valid transaction plan. Error: " + e.getMessage());

            return false;
        }

        // Set the current plan
        currentPlan = importedPlan;

        // Update the inflation amount field
        inflationAmountTextField.setText(currentPlan.getInputAmount());

        // Update dashboard
        planningSuccessful(true);

        return true;
    }

    /**
     * Validate the input amount specified by the user
     *
     * @return
     */
    private boolean validateInputAmountToPay() {
        // Check that the amount specified can be parsed, otherwise fail
        final String inflationAmountString = inflationAmountTextField.getText();
        if (!XLMUtils.isPositiveDecimalFormat(inflationAmountString)) {
            showError("Invalid payment amount. Please make sure you enter a positive value in decimal format (1234.1234567). The value must be expressed in XLM, not Stroops!");

            return false;
        }

        return true;
    }

    /**
     * Validate the currently set amount to pay
     */
    private boolean validateAmountToPay() {
        if (currentPlan.getTotalPayouts() == 0) {
            showError("The plan that you built has nothing to pay, if you specified exclusions it is possible that everyone has been paid already");

            return false;
        }


        // If the pool's balance is not enough to cover the payment, fail
        if (currentPlan.getTotalPayouts() > XLMUtils.XLMToStroop(currentPoolBalance)) {
            showError("The pool does not have enough balance to pay the inflation amount you specified.\n\n"
                      + "Pool balance: " + XLMUtils.formatBalanceFullPrecision(currentPoolBalance) + " XLM\n"
                      + "Payment requires (excluding fees): " + XLMUtils.formatBalanceFullPrecision(currentPlan.getTotalPayouts()) + " XLM");

            return false;
        }

        // All checks passed
        return true;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

