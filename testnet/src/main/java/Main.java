import java.io.FileNotFoundException;

/**
 * @Author Luca Vignaroli
 * @Date 16/12/2018 - 12:17 PM
 * @Copyright 2012-2017 Turgid Studios LTD, All rights reserved.
 */
public class Main {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Entry point
     *
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) {
        // Create an inflation pool on testnet with 5 voters and 2 payment channels
        InflationPoolManager.createNewPool(5, 2);
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
