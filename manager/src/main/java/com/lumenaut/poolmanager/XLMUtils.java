package com.lumenaut.poolmanager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @Author Luca Vignaroli
 * @Email luca@burning.it
 * @Date 13/01/2018 - 9:44 PM
 */
public class XLMUtils {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Formatters
    private static final DecimalFormat XLM_FORMATTER = new DecimalFormat("#,###,###,###,##0.00");
    private static final DecimalFormat XLM_PRECISION_FORMATTER = new DecimalFormat("#,###,###,###,##0.0000000");

    // Regex patterns
    private static final String positiveDecimalNumber = "^\\d+(\\.\\d{1,7})?$";
    private static final String negativeDecimalNumber = "^-\\d+(\\.\\d{1,7})?$";
    private static final String decimalNumber = "^[-]?\\d+(\\.\\d{1,7})?$";

    private static final String positivedatabaseBalanceFormat = "^\\d{1,19}$";
    private static final String negativedatabaseBalanceFormat = "^-\\d{1,19}$";
    private static final String databaseBalanceFormat = "^[-]?\\d{1,19}$";

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
     * Transforms the given amount in 1/10.000.000ths of XLM to a correctly scaled BigDecimal without rounding
     *
     * @param amount
     * @return
     */
    public static BigDecimal balanceToBigDecimal(final long amount) {
        BigDecimal bd = new BigDecimal(amount);
        bd = bd.setScale(7, RoundingMode.HALF_EVEN);
        bd = bd.divide(new BigDecimal(10000000), RoundingMode.HALF_EVEN);

        return bd;
    }

    /**
     * Transforms the given amount in BigDecimal format into its relative long value
     *
     * @param amount
     * @return
     */
    public static long bigDecimalToBalance(final BigDecimal amount) {
        return amount.setScale(0, BigDecimal.ROUND_HALF_EVEN).longValue();
    }

    /**
     * Formats the given amount in 1/10.000.000ths of XLM to human readable value with two decimal digits
     *
     * @param amount
     * @return
     */
    public static String formatBalance(final long amount) {
        return XLM_FORMATTER.format(balanceToBigDecimal(amount));
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
        return XLM_PRECISION_FORMATTER.format(balanceToBigDecimal(amount));
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
    public static boolean isBalanceFormat(final String amount) {
        return amount.matches(databaseBalanceFormat);
    }

    /**
     * Checks if the specified string matches the forma of a positive long number
     *
     * @param amount
     * @return
     */
    public static boolean isPositiveBalanceFormat(final String amount) {
        return amount.matches(positivedatabaseBalanceFormat);
    }

    /**
     * Checks if the specified string matches the forma of a negative long number
     *
     * @param amount
     * @return
     */
    public static boolean isNegativeBalanceFormat(final String amount) {
        return amount.matches(negativedatabaseBalanceFormat);
    }

    /**
     * Checks if the specified string matches a decimal number where decimals are dot (.) separated
     *
     * @param amount
     * @return
     */
    public static boolean isDecimalFormat(final String amount) {
        return amount.matches(decimalNumber);
    }

    /**
     * Checks if the specified string matches a positive decimal number where decimals are dot (.) separated
     *
     * @param amount
     * @return
     */
    public static boolean isPositiveDecimalFormat(final String amount) {
        return amount.matches(positiveDecimalNumber);
    }

    /**
     * Checks if the specified string matches a positive decimal number where decimals are dot (.) separated
     *
     * @param amount
     * @return
     */
    public static boolean isNegativeDecimalFormat(final String amount) {
        return amount.matches(negativeDecimalNumber);
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

