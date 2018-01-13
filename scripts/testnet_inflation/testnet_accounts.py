from decimal import Decimal
from stellar_base.keypair import Keypair
from stellar_base.address import Address
import requests
import argparse
import json
import logging


TESTNET_HORIZON = 'https://horizon-testnet.stellar.org'


class AccountNotFunded(Exception):
    pass


def fund_account(keypair):
    pk = keypair.address().decode()
    logging.info('Funding account %s', pk)
    req = TESTNET_HORIZON + '/friendbot?addr=' + pk
    response = requests.get(req)

    if response.status_code != 200:
        logging.warn('Error funding account %s', pk)
        raise AccountNotFunded()
    logging.info('Account %s funded', pk)


def create_and_fund_account():
    keypair = Keypair.random()
    logging.info('Created account: secret=%s, address=%s',
                 keypair.seed().decode(), keypair.address().decode())
    fund_account(keypair)
    return keypair


def account_balance(keypair):
    address = Address(address=keypair.address().decode(), network='TESTNET')
    address.get()
    balance = address.balances[0]
    return Decimal(balance['balance'])


def create_voting_accounts(num_accounts):
    accounts = []
    for _ in range(num_accounts):
        try:
            keypair = create_and_fund_account()
            accounts.append(keypair)
        except AccountNotFunded:
            pass
    return accounts


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('num_accounts', type=int)
    parser.add_argument('output')
    parser.add_argument('-v', '--verbose', action='store_true')
    args = parser.parse_args()

    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)

    accounts = create_voting_accounts(args.num_accounts)
    data = {
        'accounts': [
            {'address': acc.address().decode(), 'secret': acc.seed().decode()}
            for acc in accounts],
    }
    with open(args.output, 'w') as f:
        json.dump(data, f)
