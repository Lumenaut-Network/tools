package main

import (
    "fmt"
    "flag"
    "io/ioutil"
    "encoding/json"
    "github.com/stellar/go/clients/horizon"
    "github.com/stellar/go/build"
)

type TestnetAccounts struct {
    Accounts []Account `json: "accounts"`    
}

type Account struct {
    Address string `json: "address"`
    Secret string `json: "secret"`
}

var client = horizon.DefaultTestNetClient

func main() {
    
    // Set argument flags and parse input
    fileArg := flag.String("f", "testnet_accounts.json", "Name of file that contain the Testnet accounts to change the Inflation Destination")
    localArg := flag.Bool("l", false, "Use a local Horizon instance (http://localhost:8000) for faster execution")
    inflationArg := flag.String("i", "GDPRLVQKVO7IWNTVZE2YZHZ5FUQK7T36LIMWWF4FS3UAVOJNIKSTRXTQ", "Inflation destination to set for all the accounts on the JSON")
    flag.Parse()
    
    // Open JSON file
    accountsFile, err := ioutil.ReadFile(*fileArg)
    if err != nil {
        panic(err)
    }
    
    // Create struct and unmarshal file
    var accounts TestnetAccounts
    err = json.Unmarshal(accountsFile, &accounts)
    if err != nil {
        fmt.Println("Error unmarshaling JSON: ", err)
        return
    }
    
    
    if *localArg {
        client.URL = "http://localhost:8000"
    }
    
    for i, acc := range accounts.Accounts {
        fmt.Println(i, "- Addr:", acc.Address, "| Sec:", acc.Secret)
        
        horizonAcc, err := client.LoadAccount(acc.Address)
        if err != nil {
            fmt.Println("Error loading account", acc.Address)
            continue
        }
        
        if horizonAcc.InflationDestination == *inflationArg {
            fmt.Println("Inflation Destination already set!")
            continue
        }
        
        fmt.Println("Before - Inflation Destination:", horizonAcc.InflationDestination)
        
        tx := build.Transaction(
            build.SourceAccount{acc.Secret},
            build.AutoSequence{client},
            build.TestNetwork,
            build.SetOptions(
                build.InflationDest(*inflationArg),
            ),
        )
        
        fmt.Println("Transaction:", tx)
        
        txe := tx.Sign(acc.Secret)
        
        fmt.Println("Transaction-Signed:", txe)
        
        txe64, err := txe.Base64()
        if err != nil {
            fmt.Println("Error encoding transaction to base64:", err)
            continue
        }
        
        fmt.Println("Transaction b64:", txe64)
        
        // Actually submit the transaction to the test network
        resp, err := client.SubmitTransaction(txe64)
        if err != nil {
            fmt.Println("Error submitting transaction to the network:", err)
            continue
        }
        
        //fmt.Println("Transaction posted in Ledger:", resp.Ledger)
        
        fmt.Println("Afer - Inflation Destination:", horizonAcc.InflationDestination)
        fmt.Println()
    }
}
