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

    // When a transaction fails because of a timeout error from the horizon response
    // wait this much before resubmitting it (milliseconds)
    public static final int TRANSACTION_RESUBMISSION_DELAY = 10000;

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

    // Channels
    public static boolean SETTING_PARALLEL_CHANNELS_ENABLED = false;
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_1 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_1 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_2 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_2 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_3 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_3 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_4 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_4 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_5 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_5 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_6 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_6 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_7 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_7 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_8 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_8 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_9 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_9 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_10 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_10 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_11 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_11 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_12 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_12 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_13 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_13 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_14 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_14 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_15 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_15 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_16 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_16 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_17 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_17 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_18 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_18 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_19 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_19 = "";
    public static String SETTING_PARALLEL_CHANNEL_ADDRESS_20 = "";
    public static String SETTING_PARALLEL_CHANNEL_KEY_20 = "";

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
            SETTING_PARALLEL_CHANNEL_ADDRESS_1 = PROPERTIES.getProperty("channelAddress1", "");
            SETTING_PARALLEL_CHANNEL_KEY_1 = PROPERTIES.getProperty("channelKey1", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_2 = PROPERTIES.getProperty("channelAddress2", "");
            SETTING_PARALLEL_CHANNEL_KEY_2 = PROPERTIES.getProperty("channelKey2", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_3 = PROPERTIES.getProperty("channelAddress3", "");
            SETTING_PARALLEL_CHANNEL_KEY_3 = PROPERTIES.getProperty("channelKey3", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_4 = PROPERTIES.getProperty("channelAddress4", "");
            SETTING_PARALLEL_CHANNEL_KEY_4 = PROPERTIES.getProperty("channelKey4", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_5 = PROPERTIES.getProperty("channelAddress5", "");
            SETTING_PARALLEL_CHANNEL_KEY_5 = PROPERTIES.getProperty("channelKey5", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_6 = PROPERTIES.getProperty("channelAddress6", "");
            SETTING_PARALLEL_CHANNEL_KEY_6 = PROPERTIES.getProperty("channelKey6", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_7 = PROPERTIES.getProperty("channelAddress7", "");
            SETTING_PARALLEL_CHANNEL_KEY_7 = PROPERTIES.getProperty("channelKey7", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_8 = PROPERTIES.getProperty("channelAddress8", "");
            SETTING_PARALLEL_CHANNEL_KEY_8 = PROPERTIES.getProperty("channelKey8", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_9 = PROPERTIES.getProperty("channelAddress9", "");
            SETTING_PARALLEL_CHANNEL_KEY_9 = PROPERTIES.getProperty("channelKey9", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_10 = PROPERTIES.getProperty("channelAddress10", "");
            SETTING_PARALLEL_CHANNEL_KEY_10 = PROPERTIES.getProperty("channelKey10", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_11 = PROPERTIES.getProperty("channelAddress11", "");
            SETTING_PARALLEL_CHANNEL_KEY_11 = PROPERTIES.getProperty("channelKey11", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_12 = PROPERTIES.getProperty("channelAddress12", "");
            SETTING_PARALLEL_CHANNEL_KEY_12 = PROPERTIES.getProperty("channelKey12", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_13 = PROPERTIES.getProperty("channelAddress13", "");
            SETTING_PARALLEL_CHANNEL_KEY_13 = PROPERTIES.getProperty("channelKey13", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_14 = PROPERTIES.getProperty("channelAddress14", "");
            SETTING_PARALLEL_CHANNEL_KEY_14 = PROPERTIES.getProperty("channelKey14", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_15 = PROPERTIES.getProperty("channelAddress15", "");
            SETTING_PARALLEL_CHANNEL_KEY_15 = PROPERTIES.getProperty("channelKey15", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_16 = PROPERTIES.getProperty("channelAddress16", "");
            SETTING_PARALLEL_CHANNEL_KEY_16 = PROPERTIES.getProperty("channelKey16", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_17 = PROPERTIES.getProperty("channelAddress17", "");
            SETTING_PARALLEL_CHANNEL_KEY_17 = PROPERTIES.getProperty("channelKey17", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_18 = PROPERTIES.getProperty("channelAddress18", "");
            SETTING_PARALLEL_CHANNEL_KEY_18 = PROPERTIES.getProperty("channelKey18", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_19 = PROPERTIES.getProperty("channelAddress19", "");
            SETTING_PARALLEL_CHANNEL_KEY_19 = PROPERTIES.getProperty("channelKey19", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_20 = PROPERTIES.getProperty("channelAddress20", "");
            SETTING_PARALLEL_CHANNEL_KEY_20 = PROPERTIES.getProperty("channelKey20", "");
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
            SETTING_PARALLEL_CHANNEL_ADDRESS_1 = PROPERTIES.getProperty("channelAddress1", "");
            SETTING_PARALLEL_CHANNEL_KEY_1 = PROPERTIES.getProperty("channelKey1", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_2 = PROPERTIES.getProperty("channelAddress2", "");
            SETTING_PARALLEL_CHANNEL_KEY_2 = PROPERTIES.getProperty("channelKey2", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_3 = PROPERTIES.getProperty("channelAddress3", "");
            SETTING_PARALLEL_CHANNEL_KEY_3 = PROPERTIES.getProperty("channelKey3", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_4 = PROPERTIES.getProperty("channelAddress4", "");
            SETTING_PARALLEL_CHANNEL_KEY_4 = PROPERTIES.getProperty("channelKey4", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_5 = PROPERTIES.getProperty("channelAddress5", "");
            SETTING_PARALLEL_CHANNEL_KEY_5 = PROPERTIES.getProperty("channelKey5", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_6 = PROPERTIES.getProperty("channelAddress6", "");
            SETTING_PARALLEL_CHANNEL_KEY_6 = PROPERTIES.getProperty("channelKey6", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_7 = PROPERTIES.getProperty("channelAddress7", "");
            SETTING_PARALLEL_CHANNEL_KEY_7 = PROPERTIES.getProperty("channelKey7", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_8 = PROPERTIES.getProperty("channelAddress8", "");
            SETTING_PARALLEL_CHANNEL_KEY_8 = PROPERTIES.getProperty("channelKey8", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_9 = PROPERTIES.getProperty("channelAddress9", "");
            SETTING_PARALLEL_CHANNEL_KEY_9 = PROPERTIES.getProperty("channelKey9", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_10 = PROPERTIES.getProperty("channelAddress10", "");
            SETTING_PARALLEL_CHANNEL_KEY_10 = PROPERTIES.getProperty("channelKey10", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_11 = PROPERTIES.getProperty("channelAddress11", "");
            SETTING_PARALLEL_CHANNEL_KEY_11 = PROPERTIES.getProperty("channelKey11", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_12 = PROPERTIES.getProperty("channelAddress12", "");
            SETTING_PARALLEL_CHANNEL_KEY_12 = PROPERTIES.getProperty("channelKey12", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_13 = PROPERTIES.getProperty("channelAddress13", "");
            SETTING_PARALLEL_CHANNEL_KEY_13 = PROPERTIES.getProperty("channelKey13", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_14 = PROPERTIES.getProperty("channelAddress14", "");
            SETTING_PARALLEL_CHANNEL_KEY_14 = PROPERTIES.getProperty("channelKey14", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_15 = PROPERTIES.getProperty("channelAddress15", "");
            SETTING_PARALLEL_CHANNEL_KEY_15 = PROPERTIES.getProperty("channelKey15", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_16 = PROPERTIES.getProperty("channelAddress16", "");
            SETTING_PARALLEL_CHANNEL_KEY_16 = PROPERTIES.getProperty("channelKey16", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_17 = PROPERTIES.getProperty("channelAddress17", "");
            SETTING_PARALLEL_CHANNEL_KEY_17 = PROPERTIES.getProperty("channelKey17", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_18 = PROPERTIES.getProperty("channelAddress18", "");
            SETTING_PARALLEL_CHANNEL_KEY_18 = PROPERTIES.getProperty("channelKey18", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_19 = PROPERTIES.getProperty("channelAddress19", "");
            SETTING_PARALLEL_CHANNEL_KEY_19 = PROPERTIES.getProperty("channelKey19", "");
            SETTING_PARALLEL_CHANNEL_ADDRESS_20 = PROPERTIES.getProperty("channelAddress20", "");
            SETTING_PARALLEL_CHANNEL_KEY_20 = PROPERTIES.getProperty("channelKey20", "");

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
        PROPERTIES.setProperty("channelAddress1", SETTING_PARALLEL_CHANNEL_ADDRESS_1);
        PROPERTIES.setProperty("channelKey1", SETTING_PARALLEL_CHANNEL_KEY_1);
        PROPERTIES.setProperty("channelAddress2", SETTING_PARALLEL_CHANNEL_ADDRESS_2);
        PROPERTIES.setProperty("channelKey2", SETTING_PARALLEL_CHANNEL_KEY_2);
        PROPERTIES.setProperty("channelAddress3", SETTING_PARALLEL_CHANNEL_ADDRESS_3);
        PROPERTIES.setProperty("channelKey3", SETTING_PARALLEL_CHANNEL_KEY_3);
        PROPERTIES.setProperty("channelAddress4", SETTING_PARALLEL_CHANNEL_ADDRESS_4);
        PROPERTIES.setProperty("channelKey4", SETTING_PARALLEL_CHANNEL_KEY_4);
        PROPERTIES.setProperty("channelAddress5", SETTING_PARALLEL_CHANNEL_ADDRESS_5);
        PROPERTIES.setProperty("channelKey5", SETTING_PARALLEL_CHANNEL_KEY_5);
        PROPERTIES.setProperty("channelAddress6", SETTING_PARALLEL_CHANNEL_ADDRESS_6);
        PROPERTIES.setProperty("channelKey6", SETTING_PARALLEL_CHANNEL_KEY_6);
        PROPERTIES.setProperty("channelAddress7", SETTING_PARALLEL_CHANNEL_ADDRESS_7);
        PROPERTIES.setProperty("channelKey7", SETTING_PARALLEL_CHANNEL_KEY_7);
        PROPERTIES.setProperty("channelAddress8", SETTING_PARALLEL_CHANNEL_ADDRESS_8);
        PROPERTIES.setProperty("channelKey8", SETTING_PARALLEL_CHANNEL_KEY_8);
        PROPERTIES.setProperty("channelAddress9", SETTING_PARALLEL_CHANNEL_ADDRESS_9);
        PROPERTIES.setProperty("channelKey9", SETTING_PARALLEL_CHANNEL_KEY_9);
        PROPERTIES.setProperty("channelAddress10", SETTING_PARALLEL_CHANNEL_ADDRESS_10);
        PROPERTIES.setProperty("channelKey10", SETTING_PARALLEL_CHANNEL_KEY_10);
        PROPERTIES.setProperty("channelAddress11", SETTING_PARALLEL_CHANNEL_ADDRESS_11);
        PROPERTIES.setProperty("channelKey11", SETTING_PARALLEL_CHANNEL_KEY_11);
        PROPERTIES.setProperty("channelAddress12", SETTING_PARALLEL_CHANNEL_ADDRESS_12);
        PROPERTIES.setProperty("channelKey12", SETTING_PARALLEL_CHANNEL_KEY_12);
        PROPERTIES.setProperty("channelAddress13", SETTING_PARALLEL_CHANNEL_ADDRESS_13);
        PROPERTIES.setProperty("channelKey13", SETTING_PARALLEL_CHANNEL_KEY_13);
        PROPERTIES.setProperty("channelAddress14", SETTING_PARALLEL_CHANNEL_ADDRESS_14);
        PROPERTIES.setProperty("channelKey14", SETTING_PARALLEL_CHANNEL_KEY_14);
        PROPERTIES.setProperty("channelAddress15", SETTING_PARALLEL_CHANNEL_ADDRESS_15);
        PROPERTIES.setProperty("channelKey15", SETTING_PARALLEL_CHANNEL_KEY_15);
        PROPERTIES.setProperty("channelAddress16", SETTING_PARALLEL_CHANNEL_ADDRESS_16);
        PROPERTIES.setProperty("channelKey16", SETTING_PARALLEL_CHANNEL_KEY_16);
        PROPERTIES.setProperty("channelAddress17", SETTING_PARALLEL_CHANNEL_ADDRESS_17);
        PROPERTIES.setProperty("channelKey17", SETTING_PARALLEL_CHANNEL_KEY_17);
        PROPERTIES.setProperty("channelAddress18", SETTING_PARALLEL_CHANNEL_ADDRESS_18);
        PROPERTIES.setProperty("channelKey18", SETTING_PARALLEL_CHANNEL_KEY_18);
        PROPERTIES.setProperty("channelAddress19", SETTING_PARALLEL_CHANNEL_ADDRESS_19);
        PROPERTIES.setProperty("channelKey19", SETTING_PARALLEL_CHANNEL_KEY_19);
        PROPERTIES.setProperty("channelAddress20", SETTING_PARALLEL_CHANNEL_ADDRESS_20);
        PROPERTIES.setProperty("channelKey20", SETTING_PARALLEL_CHANNEL_KEY_20);

        // Store
        PROPERTIES.store(new FileOutputStream("data/settings.ini"), "Settings");
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

