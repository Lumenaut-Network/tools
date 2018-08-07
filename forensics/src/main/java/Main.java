import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Luca Vignaroli
 * @Date 31/07/2018 - 7:20 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 */
public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Entry point
     *
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) {

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
