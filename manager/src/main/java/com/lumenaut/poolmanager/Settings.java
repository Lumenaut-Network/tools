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

    // Horizon networks
    public static final String HORIZON_TEST_NETWORK = "https://horizon-testnet.stellar.org";
    public static final String HORIZON_LIVE_NETWORK = "https://horizon.stellar.org";

    // Properties instance
    private static final Properties PROPERTIES = new Properties();

    // Default Settings
    public static String SETTING_OPERATIONS_NETWORK = "";
    public static String SETTING_INFLATION_POOL_ADDRESS = "";
    public static String SETTING_FEDERATION_NETWORK_INFLATION_URL = "";
    public static String SETTING_HORIZON_DB_ADDRESS = "";
    public static String SETTING_HORIZON_DB_PORT = "";
    public static String SETTING_HORIZON_DB_USER = "";
    public static String SETTING_HORIZON_DB_PASS = "";
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
            SETTING_HORIZON_DB_ADDRESS = PROPERTIES.getProperty("horizonDbAddress", "");
            SETTING_HORIZON_DB_PORT = PROPERTIES.getProperty("horizonDbPort", "");
            SETTING_HORIZON_DB_USER = PROPERTIES.getProperty("horizonDbUser", "");
            SETTING_HORIZON_DB_PASS = PROPERTIES.getProperty("horizonDbPass", "");

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

            SETTING_HORIZON_DB_ADDRESS = PROPERTIES.getProperty("horizonDbAddress", "");
            SETTING_HORIZON_DB_PORT = PROPERTIES.getProperty("horizonDbPort", "");
            SETTING_HORIZON_DB_USER = PROPERTIES.getProperty("horizonDbUser", "");
            SETTING_HORIZON_DB_PASS = PROPERTIES.getProperty("horizonDbPass", "");

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

        // Horizon DB
        PROPERTIES.setProperty("horizonDbAddress", SETTING_HORIZON_DB_ADDRESS);
        PROPERTIES.setProperty("horizonDbPort", SETTING_HORIZON_DB_PORT);
        PROPERTIES.setProperty("horizonDbUser", SETTING_HORIZON_DB_USER);
        PROPERTIES.setProperty("horizonDbPass", SETTING_HORIZON_DB_PASS);

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

        // Store
        PROPERTIES.store(new FileOutputStream("data/settings.ini"), "Settings");
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

