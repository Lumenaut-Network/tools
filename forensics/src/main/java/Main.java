import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.stellar.sdk.Operation;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.xdr.OperationResult;
import org.stellar.sdk.xdr.PaymentResult;
import org.stellar.sdk.xdr.TransactionResult;
import org.stellar.sdk.xdr.XdrDataInputStream;
import shadow.com.google.common.io.BaseEncoding;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Luca Vignaroli
 * @Date 31/07/2018 - 7:20 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 */
public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class Entry {
        public int slot;
        public String accountId;
        public long amount;
        public String result;

        public Entry(int slot, String accountId, long amount, String result) {
            this.slot = slot;
            this.accountId = accountId;
            this.amount = amount;
            this.result = result;
        }
    }

    /**
     * Entry point
     *
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) {
        try {
            extractTransactionsFromErrorFiles("xdrdata");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractTransactionsFromErrorFiles(final String path) throws IOException {
        final HashMap<String, List<Entry>> entries = new HashMap<>();
        Files.list(new File(path).toPath()).forEach(path1 -> {
            final File file = path1.toFile();
            if (!file.getName().equals("plan.json")) {
                final JsonNode errorResult;
                try {
                    errorResult = MAPPER.readTree(file);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    return;
                }

                final String resultXdr = errorResult.get("resultXdr").asText();
                TransactionResult transactionResult = null;
                try {
                    final BaseEncoding base64Encoding = BaseEncoding.base64();
                    final byte[] bytes = base64Encoding.decode(resultXdr);
                    transactionResult = TransactionResult.decode(new XdrDataInputStream(new ByteArrayInputStream(bytes)));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Extract operations
                final String envelopeXdr = errorResult.get("envelopeXdr").asText();
                try {
                    // Extract transaction operations
                    final Transaction transaction = Transaction.fromEnvelopeXdr(envelopeXdr);
                    final Operation[] ops = transaction.getOperations();
                    final OperationResult[] opsResults = transactionResult.getResult().getResults();

                    // Create entry
                    if (!entries.containsKey(file.getName())) {
                        entries.put(file.getName(), new ArrayList<>());
                    }

                    // Populate all accounts found
                    for (int i = 0; i < ops.length; i++) {
                        final String accountId = ((PaymentOperation) ops[i]).getDestination().getAccountId();
                        final String amount = ((PaymentOperation) ops[i]).getAmount();
                        final long stroops = XLMUtils.XLMToStroop(XLMUtils.decimalStringToXLM(amount));
                        final PaymentResult result = opsResults[i].getTr().getPaymentResult();

                        entries.get(file.getName()).add(new Entry(i, accountId, stroops, result.getDiscriminant().name()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("# " + file.getName());
                entries.get(file.getName()).forEach(entry -> {
                    if (!entry.result.equals("PAYMENT_SUCCESS")) {
                        System.out.println(entry.accountId + " -> " + entry.result);
                    }
                });

                System.out.println();
            }
        });

        int breakhere = 0;
    }

    /**
     * Search for the specified result code among the given transaction result files
     * @param transactionResultsPath
     * @param resultCode
     * @throws IOException
     */
    private static void searchForResultCodeInTransactionResults(final String transactionResultsPath, final String resultCode) throws IOException {
        searchForResultCodeInTransactionResults(transactionResultsPath, resultCode, false);
    }

    /**
     * Search for the specified result code among the given transaction result files. Optionally invert the search logic.
     *
     * @param transactionResultsPath
     * @param resultCode
     * @param inverted
     * @throws IOException
     */
    private static void searchForResultCodeInTransactionResults(final String transactionResultsPath, final String resultCode, final boolean inverted) throws IOException {
        final AtomicInteger matchCounter = new AtomicInteger(0);
        Files.list(new File(transactionResultsPath).toPath()).forEach(path -> {
            final File file = path.toFile();

            if (!file.isDirectory()) {
                final JsonNode plan;
                try {
                    plan = MAPPER.readTree(file);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    return;
                }

                final String transactionResultCode = plan.get("transactionResultCode").asText();
                if (inverted && !transactionResultCode.equals(resultCode)) {
                    System.out.println("Found match in: " + file.getName());
                    matchCounter.getAndIncrement();
                } else if (!inverted && transactionResultCode.equals(resultCode)) {
                    System.out.println("Found match in: " + file.getName());
                    matchCounter.getAndIncrement();
                }
            }
        });

        System.out.println("Matches found [" + matchCounter.get() + "]");
    }

    /**
     * Generate fetch data JSON (usable in manager v 1.0) from a list of accounts to be excluded (CSV file format ADDR:BALANCE) from the given transaction plan file
     *
     * @param transactionPlanPath      Path to the transaction plan JSON file
     * @param accountsToExcludeCsvPath Accounts to be excluded from the plan with the format [ADDRESS,PAYOUT_IN_XLM]
     * @throws FileNotFoundException
     */
    private static void extractExclusions(final String transactionPlanPath, final String accountsToExcludeCsvPath, final String outputFilePath) throws IOException {
        // Original payment plan
        final JsonNode plan = MAPPER.readTree(new File(transactionPlanPath));

        // Create exclusions list from the csv file
        final ArrayList<String> exclusions = new ArrayList<>();
        final Scanner scanner = new Scanner(new File(accountsToExcludeCsvPath));
        while (scanner.hasNextLine()) {
            final String[] line = scanner.nextLine().split(",");
            exclusions.add(line[0]);
        }
        scanner.close();

        // Create new data for the manager
        final ObjectNode rootNode = MAPPER.createObjectNode();
        final ArrayNode entriesNode = MAPPER.createArrayNode();

        final ArrayNode entries = (ArrayNode) plan.get("entries");
        for (JsonNode entry : entries) {
            // Check entry data
            final String targetAccount = entry.get("destination").asText();
            final Long targetAccountBalance = entry.get("recordedBalance").asLong();

            // Exclude if needed
            if (exclusions.contains(targetAccount)) {
                continue;
            }

            // Append to the result
            final ObjectNode entryNode = MAPPER.createObjectNode();
            entryNode.put("account", targetAccount);
            entryNode.put("balance", targetAccountBalance);
            entriesNode.add(entryNode);
        }

        // Append node
        rootNode.put("inflationdest", "GCCD6AJOYZCUAQLX32ZJF2MKFFAUJ53PVCFQI3RHWKL3V47QYE2BNAUT");
        rootNode.set("entries", entriesNode);

        // Save to file
        try (
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8);
        BufferedWriter bufWriter = new BufferedWriter(writer)
        ) {
            bufWriter.write(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
        }
    }
}
