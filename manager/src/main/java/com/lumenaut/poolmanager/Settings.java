package com.lumenaut.poolmanager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.Properties;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 11/01/2018 - 6:05 PM
 */
public class Settings {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Multi-Threading
    // This setting should be as high as the maximum number of supported channels that the application can use.
    // The transactions' threads (ParallelTransactionTask.java) are super lightweight and will spend 99% of the time I/O blocked,
    // don't worry if your CPU doesn't an equal amount of cores, your kernel's scheduler will manage just fine.
    public static final int MAX_THREADS = 20;

    // Minimum valid account balance accepted to receive inflation (in stroops)
    public static final long MIN_ACCOUNT_BALANCE = 25_000_000L;

    // Properties instance
    private static final Properties PROPERTIES = new Properties();

    // Horizon networks
    public static String SETTING_HORIZON_TEST_NETWORK = "";
    public static String SETTING_HORIZON_LIVE_NETWORK = "";
    public static String SETTING_HORIZON_DB_TEST_ADDRESS = "";
    public static String SETTING_HORIZON_DB_TEST_PORT = "";
    public static String SETTING_HORIZON_DB_TEST_USER = "";
    public static String SETTING_HORIZON_DB_TEST_PASS = "";
    public static String SETTING_HORIZON_DB_LIVE_ADDRESS = "";
    public static String SETTING_HORIZON_DB_LIVE_PORT = "";
    public static String SETTING_HORIZON_DB_LIVE_USER = "";
    public static String SETTING_HORIZON_DB_LIVE_PASS = "";

    // Default Settings
    public static String SETTING_OPERATIONS_NETWORK = "";
    public static String SETTING_INFLATION_POOL_ADDRESS = "";
    public static String SETTING_FEDERATION_NETWORK_INFLATION_URL = "";

    public static String SETTING_MEMO = "";
    public static long SETTING_FEE = 100;
    public static int SETTING_OPERATIONS_PER_TRANSACTION_BATCH = 100;
    public static String SETTING_DONATION_DATANAME_PREFIX = "";

    // Payment Channels
    public static boolean SETTING_PARALLEL_CHANNELS_ENABLED = false;

    // Non persistent settings
    public static RoundingMode ROUNDING_MODE = RoundingMode.HALF_DOWN;

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
    private Settings() {

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

    /**
     * Load settings from the ini file in the working directory or set defaults
     */
    public static void loadSettings() throws IOException {
        try {
            // Attempt to load the file
            PROPERTIES.load(new FileInputStream("data/settings.ini"));

            // Bring the settings in the runtime
            SETTING_OPERATIONS_NETWORK = PROPERTIES.getProperty("operationsNetwork", "TEST");
            SETTING_INFLATION_POOL_ADDRESS = XLMUtils.isPublicKeyValidFormat(PROPERTIES.getProperty("inflationPoolAddress")) ? PROPERTIES.getProperty("inflationPoolAddress") : "";
            SETTING_FEDERATION_NETWORK_INFLATION_URL = PROPERTIES.getProperty("fedNetworkInflationUrl", "https://fed.network/inflation/");
            SETTING_MEMO = PROPERTIES.getProperty("memoText", "Thanks from lumenaut.net");
            SETTING_FEE = Long.parseLong(PROPERTIES.getProperty("fee")) < 100 ? 100 : Long.parseLong(PROPERTIES.getProperty("fee"));
            SETTING_DONATION_DATANAME_PREFIX = PROPERTIES.getProperty("donationsPrefix", "lumenaut.net donation");

            // Horizon database
            SETTING_HORIZON_TEST_NETWORK = PROPERTIES.getProperty("horizonTestNetwork", "https://horizon-testnet.stellar.org");
            SETTING_HORIZON_LIVE_NETWORK = PROPERTIES.getProperty("horizonLiveNetwork", "https://horizon.stellar.org");
            SETTING_HORIZON_DB_TEST_ADDRESS = PROPERTIES.getProperty("horizonDbTestAddress", "");
            SETTING_HORIZON_DB_TEST_PORT = PROPERTIES.getProperty("horizonDbTestPort", "");
            SETTING_HORIZON_DB_TEST_USER = PROPERTIES.getProperty("horizonDbTestUser", "");
            SETTING_HORIZON_DB_TEST_PASS = PROPERTIES.getProperty("horizonDbTestPass", "");
            SETTING_HORIZON_DB_LIVE_ADDRESS = PROPERTIES.getProperty("horizonDbLiveAddress", "");
            SETTING_HORIZON_DB_LIVE_PORT = PROPERTIES.getProperty("horizonDbLivePort", "");
            SETTING_HORIZON_DB_LIVE_USER = PROPERTIES.getProperty("horizonDbLiveUser", "");
            SETTING_HORIZON_DB_LIVE_PASS = PROPERTIES.getProperty("horizonDbLivePass", "");

            // Channels
            SETTING_PARALLEL_CHANNELS_ENABLED = Boolean.parseBoolean(PROPERTIES.getProperty("useParallelChannels", "false"));
        } catch (Exception e) {
            // Init defaults (ONLY EXECUTED WHEN the "settings ini file doesn't exist)
            SETTING_OPERATIONS_NETWORK = PROPERTIES.getProperty("operationsNetwork", "TEST");
            SETTING_INFLATION_POOL_ADDRESS = PROPERTIES.getProperty("inflationPoolAddress", "");
            SETTING_FEDERATION_NETWORK_INFLATION_URL = PROPERTIES.getProperty("fedNetworkInflationUrl", "https://fed.network/inflation/");
            SETTING_MEMO = PROPERTIES.getProperty("memoText", "Thanks from lumenaut.net");
            SETTING_DONATION_DATANAME_PREFIX = PROPERTIES.getProperty("donationsPrefix", "lumenaut.net donation");

            // Try to parse the fee, or just default to 100
            try {
                SETTING_FEE = Long.parseLong(PROPERTIES.getProperty("fee", "100"));
            } catch (NumberFormatException ne) {
                SETTING_FEE = 100;
            }

            // Horizon database
            SETTING_HORIZON_TEST_NETWORK = PROPERTIES.getProperty("horizonTestNetwork", "https://horizon-testnet.stellar.org");
            SETTING_HORIZON_LIVE_NETWORK = PROPERTIES.getProperty("horizonLiveNetwork", "https://horizon.stellar.org");
            SETTING_HORIZON_DB_TEST_ADDRESS = PROPERTIES.getProperty("horizonDbTestAddress", "");
            SETTING_HORIZON_DB_TEST_PORT = PROPERTIES.getProperty("horizonDbTestPort", "");
            SETTING_HORIZON_DB_TEST_USER = PROPERTIES.getProperty("horizonDbTestUser", "");
            SETTING_HORIZON_DB_TEST_PASS = PROPERTIES.getProperty("horizonDbTestPass", "");
            SETTING_HORIZON_DB_LIVE_ADDRESS = PROPERTIES.getProperty("horizonDbLiveAddress", "");
            SETTING_HORIZON_DB_LIVE_PORT = PROPERTIES.getProperty("horizonDbLivePort", "");
            SETTING_HORIZON_DB_LIVE_USER = PROPERTIES.getProperty("horizonDbLiveUser", "");
            SETTING_HORIZON_DB_LIVE_PASS = PROPERTIES.getProperty("horizonDbLivePass", "");

            SETTING_PARALLEL_CHANNELS_ENABLED = Boolean.parseBoolean(PROPERTIES.getProperty("useParallelChannels", "false"));

            // Save defaults
            saveSettings();
        }
    }

    /**
     * Save settings to the ini file in the working directory
     *
     * @throws IOException
     */
    public static void saveSettings() throws IOException {
        // Get current settings
        PROPERTIES.setProperty("operationsNetwork", SETTING_OPERATIONS_NETWORK);
        PROPERTIES.setProperty("inflationPoolAddress", XLMUtils.isPublicKeyValidFormat(SETTING_INFLATION_POOL_ADDRESS) ? SETTING_INFLATION_POOL_ADDRESS : "");
        PROPERTIES.setProperty("fedNetworkInflationUrl", SETTING_FEDERATION_NETWORK_INFLATION_URL);
        PROPERTIES.setProperty("memoText", SETTING_MEMO);
        PROPERTIES.setProperty("fee", SETTING_FEE >= 100 ? String.valueOf(SETTING_FEE) : "100");
        PROPERTIES.setProperty("donationsPrefix", SETTING_DONATION_DATANAME_PREFIX);

        // Horizon database
        PROPERTIES.setProperty("horizonTestNetwork", SETTING_HORIZON_TEST_NETWORK);
        PROPERTIES.setProperty("horizonLiveNetwork", SETTING_HORIZON_LIVE_NETWORK);
        PROPERTIES.setProperty("horizonDbTestAddress", SETTING_HORIZON_DB_TEST_ADDRESS);
        PROPERTIES.setProperty("horizonDbTestPort", SETTING_HORIZON_DB_TEST_PORT);
        PROPERTIES.setProperty("horizonDbTestUser", SETTING_HORIZON_DB_TEST_USER);
        PROPERTIES.setProperty("horizonDbTestPass", SETTING_HORIZON_DB_TEST_PASS);
        PROPERTIES.setProperty("horizonDbLiveAddress", SETTING_HORIZON_DB_LIVE_ADDRESS);
        PROPERTIES.setProperty("horizonDbLivePort", SETTING_HORIZON_DB_LIVE_PORT);
        PROPERTIES.setProperty("horizonDbLiveUser", SETTING_HORIZON_DB_LIVE_USER);
        PROPERTIES.setProperty("horizonDbLivePass", SETTING_HORIZON_DB_LIVE_PASS);

        // Channels
        PROPERTIES.setProperty("useParallelChannels", String.valueOf(SETTING_PARALLEL_CHANNELS_ENABLED));

        // Store
        PROPERTIES.store(new FileOutputStream("data/settings.ini"), "Settings");
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

