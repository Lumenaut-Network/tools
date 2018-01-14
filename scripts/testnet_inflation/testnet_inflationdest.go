package main

import (
    "fmt"
    "flag"
    "sync"
    "io/ioutil"
    "encoding/json"
    "github.com/stellar/go/clients/horizon"
    "github.com/stellar/go/build"
)

const WGMAX = 50

type TestnetAccounts struct {
    Accounts []Account `json: "accounts"`    
}

type Account struct {
    Address string `json: "address"`
    Secret string `json: "secret"`
}

type TxResponse struct {
    resp horizon.TransactionSuccess
    err error
}

func main() {
    
    // Set argument flags and parse input
    fileArg := flag.String("f", "testnet_accounts.json", "Name of file that contain the Testnet accounts to change the Inflation Destination")
    localArg := flag.Bool("l", false, "Use a local Horizon instance (http://localhost:8000) for faster execution")
    inflationArg := flag.String("i", "GCFXD4OBX4TZ5GGBWIXLIJHTU2Z6OWVPYYU44QSKCCU7P2RGFOOHTEST", "Inflation destination to set for all the accounts on the JSON")
    skipArg := flag.Int("s", 0, "Start on element number 's' of the file")
    flag.Parse()
    
    // Set Horizon to  https://horizon-testnet.stellar.org or localhost
    client := horizon.DefaultTestNetClient
    if *localArg {
        client.URL = "http://localhost:8000"
    }
    
    // Open JSON file
    accountsFile, err := ioutil.ReadFile(*fileArg)
    if err != nil {
        panic(err)
    }
    
    // Create struct and unmarshal file
    var accounts TestnetAccounts
    err = json.Unmarshal(accountsFile, &accounts)
    if err != nil {
        fmt.Println("ERROR unmarshaling JSON: ", err)
        return
    }
    fmt.Println("Input file contains", len(accounts.Accounts), "keypairs!")
    
    // Channels to control goroutines and responses
    var wg sync.WaitGroup
    guard := make(chan struct{}, WGMAX)
    respChan := make(chan TxResponse, len(accounts.Accounts))
    
    // Create one goroutine for each account
    for i, acc := range accounts.Accounts {
        // User asked to skip, probably already has the Inflation Dest setup
        if i < *skipArg {
            continue
        }
        
        // Build the JSON transaction
        tx := build.Transaction(
            build.SourceAccount{acc.Address},
            build.AutoSequence{client},
            build.TestNetwork,
            build.SetOptions(build.InflationDest(*inflationArg)),
        )
        // Sign it with the secret key
        txe := tx.Sign(acc.Secret)
        // Encode in the XDR format
        xdr, err := txe.Base64()
        if err != nil {
            fmt.Println("ERROR encoding XDR:", err)
            continue
        }
        
        fmt.Println("Build-for i:", i, "- Addr:", acc.Address)
        
        // Mark that a new goroutine will be used (block when guard is full)
        guard<- struct{}{}
        wg.Add(1)
        
        go func(xdr string, respChan chan TxResponse) {
            defer wg.Done()
            
            // Actually submit the transaction to the horizon server (blocks)
            response, err := client.SubmitTransaction(xdr)
            // Bundle results and send on the channel
            respChan<- TxResponse{response, err}
            
            //Allow a new goroutine to work, but don't block if guard is empty
            select {
                case <-guard:
                default:
            }
        }(xdr, respChan)
    }
    
    wg.Wait()
    results:
    for i := 0; i < len(accounts.Accounts); i++ {
        // Take result from the channel, but don't block if it's empty
        select {
            case r:= <-respChan:
                fmt.Println("Result-for i:", i, "- Error:", r.err)
                if r.err != nil {
                    horizonError, isHorizonError := r.err.(*horizon.Error)
                    if isHorizonError {
                        resultCodes, err := horizonError.ResultCodes()
                        if err != nil {
                            fmt.Println("ERROR extracting result codes from a Horizon response:", err)
                            continue
                        }
                        fmt.Println("ERROR Submitting transaction (Horizon-Error) - Result Code:", resultCodes)
                    } else {
                        fmt.Println("ERROR Submitting transaction:", err)
                    }
                }
            default:
                fmt.Println("respChan is empty!")
                break results
        }
    }
}
