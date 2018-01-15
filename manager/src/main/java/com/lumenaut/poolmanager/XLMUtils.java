package com.lumenaut.poolmanager;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static com.lumenaut.poolmanager.Settings.ROUNDING_MODE;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 13/01/2018 - 9:44 PM
 */
public class XLMUtils {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Config
    public static final DecimalFormat XLM_FORMATTER = new DecimalFormat("#,###,###,###,##0.00");
    public static final DecimalFormat XLM_PRECISION_FORMATTER = new DecimalFormat("#,###,###,###,##0.0000000");

    // Regex patterns
    private static final String positiveDecimalNumberPattern = "^\\d+(\\.\\d{1,7})?$";
    private static final String negativeDecimalNumberPattern = "^-\\d+(\\.\\d{1,7})?$";
    private static final String decimalNumberPattern = "^[-]?\\d+(\\.\\d{1,7})?$";
    private static final String positiveStroopFormatPattern = "^\\d{1,19}$";
    private static final String negativeStroopFormatPattern = "^-\\d{1,19}$";
    private static final String stroopFormatPattern = "^[-]?\\d{1,19}$";

    // Constants
    public static final BigDecimal STROOPS_IN_XLM = new BigDecimal("10000000");

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Constructor
     */
    private XLMUtils() {

    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Convert a decimal formatted string to a bigdecimal with scale 7 which represents a value in XLM
     *
     * @param amount
     * @return
     */
    public static BigDecimal decimalStringToXLM(final String amount) {
        BigDecimal bd = new BigDecimal(amount);
        bd = bd.setScale(7, ROUNDING_MODE);

        return bd;
    }

    /**
     * Transforms the given amount in 1/10.000.000ths of XLM to a correctly scaled BigDecimal without rounding
     *
     * @param amount
     * @return
     */
    public static BigDecimal stroopToXLM(final long amount) {
        BigDecimal bd = new BigDecimal(amount);
        bd = bd.setScale(7, ROUNDING_MODE);       // Push it to the new scale
        bd = bd.divide(STROOPS_IN_XLM, ROUNDING_MODE);     // We can now safely convert stroops to XLM

        return bd;
    }

    /**
     * Transforms the given amount in BigDecimal format into its relative long value
     *
     * @param amount
     * @return
     */
    public static long XLMToStroop(final BigDecimal amount) {
        return amount.setScale(7, ROUNDING_MODE).multiply(STROOPS_IN_XLM).longValue();
    }

    /**
     * Formats the given amount in 1/10.000.000ths of XLM to human readable value with two decimal digits
     *
     * @param amount
     * @return
     */
    public static String formatBalance(final long amount) {
        return XLM_FORMATTER.format(stroopToXLM(amount));
    }

    /**
     * Formats the given amount in 1/10.000.000ths of XLM to human readable value with two decimal digits
     *
     * @param amount
     * @return
     */
    public static String formatBalance(final BigDecimal amount) {
        return XLM_FORMATTER.format(amount);
    }

    /**
     * Formats the given amount in 1/10.000.000ths of XLM to human readable value with seven decimal digits
     *
     * @param amount
     * @return
     */
    public static String formatBalanceFullPrecision(final long amount) {
        return XLM_PRECISION_FORMATTER.format(stroopToXLM(amount));
    }

    /**
     * Formats the given amount in 1/10.000.000ths of XLM to human readable value with seven decimal digits
     *
     * @param amount
     * @return
     */
    public static String formatBalanceFullPrecision(final BigDecimal amount) {
        return XLM_PRECISION_FORMATTER.format(amount);
    }

    /**
     * Checks if the specified string matches the forma of a long
     *
     * @param amount
     * @return
     */
    public static boolean isStroopFormat(final String amount) {
        return amount.matches(stroopFormatPattern);
    }

    /**
     * Checks if the specified string matches the forma of a positive long number
     *
     * @param amount
     * @return
     */
    public static boolean isPositiveStroopFormat(final String amount) {
        return amount.matches(positiveStroopFormatPattern);
    }

    /**
     * Checks if the specified string matches the forma of a negative long number
     *
     * @param amount
     * @return
     */
    public static boolean isNegativeStroopFormat(final String amount) {
        return amount.matches(negativeStroopFormatPattern);
    }

    /**
     * Checks if the specified string matches a decimal number where decimals are dot (.) separated
     *
     * @param amount
     * @return
     */
    public static boolean isDecimalFormat(final String amount) {
        return amount.matches(decimalNumberPattern);
    }

    /**
     * Checks if the specified string matches a positive decimal number where decimals are dot (.) separated
     *
     * @param amount
     * @return
     */
    public static boolean isPositiveDecimalFormat(final String amount) {
        return amount.matches(positiveDecimalNumberPattern);
    }

    /**
     * Checks if the specified string matches a positive decimal number where decimals are dot (.) separated
     *
     * @param amount
     * @return
     */
    public static boolean isNegativeDecimalFormat(final String amount) {
        return amount.matches(negativeDecimalNumberPattern);
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

