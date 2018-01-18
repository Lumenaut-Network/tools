#!/usr/bin/env python3
import os
import sys

from stellar_base.address import Address


class Stargazer:
    
    def __init__(self, public_key, data_dir="/var/lib/stargazer", dry_run=False):
        #  On first run, we need to create the datadir
        if not os.path.exists(data_dir):
            os.makedirs(data_dir)

        if not public_key:
            raise ValueError("Please provide public_key.")
            
        self.public_key = public_key
        self.address = Address(address=self.public_key, network='public')
        self.last_cursor_filepath = os.path.join(data_dir, "{}{}".format(self.public_key, '_cursor'))
        self.dry_run = dry_run
        
    def _get_last_cursor(self):
        try:
            # Attempt to read the last cursor from the file
            with open(self.last_cursor_filepath, 'r') as input_:
                last_cursor = input_.read()
                try:
                    return int(last_cursor)
                except ValueError:
                    return None
        except FileNotFoundError:
            return None
            
    def poll(self):
        """ Query for all payments since last cursor.
            For each payment, notify and save last_cursor to datadir
        """
        
        # Welcome message for the first time
        last_cursor = self._get_last_cursor()
        if not last_cursor:
            message = "Hi there! I'll let you know if there are transaction from/to {}.".format(self.public_key)
            self._notify(message)
            
        # Get last 10 payment from last cursor
        payment_data = self.address.payments(cursor=last_cursor)['_embedded']['records']
        
        # No payments, do nothing
        if not payment_data:
            return
            
        last_notified_cursor =  None
        for payment in payment_data:
            current_cursor = payment['paging_token']
            
            # Discard other operations, only interested in payment currently
            if payment['type_i'] != 1:
                print('Not transaction, discarding {}'.format(current_cursor))
                last_notified_cursor = current_cursor
                continue
                
            # Build message to notify
            transaction_url = "https://stellar.expert/explorer/tx/{hash}".format(hash=payment['transaction_hash'])
            if payment['to'] == self.public_key:
                message = "{amount} XLM Received from {source}. Details: {url}".format(
                    amount=payment['amount'], 
                    source=payment['from'], 
                    url=transaction_url
                )
            else:
                message = "{amount} XLM sent to {destination}. Details: {url}".format(
                    amount=payment['amount'], 
                    destination=payment['to'], 
                    url=transaction_url
                )
                
            # Try to notify, if we fail, bail out of the loop and persist last 
            # Notified cursor
            if self._notify(message):
                last_notified_cursor = current_cursor
            else:
                break
                
        # Persist last notified cursor
        if last_notified_cursor:
            self._set_last_cursor(last_notified_cursor)


    def _notify(self, message):
        """ Notify whatever channel we need.
            Return True when notification success, False when failed
        """
        if self.dry_run:
            print(message)
            return True
        else:
            # TODO: use requests to notify slack incoming webhook
            return True
            
    def _set_last_cursor(self, cursor):
        with open(self.last_cursor_filepath, 'w') as output_:
            output_.write(cursor)


# Quick and dirty way to get pub key as arg
if len(sys.argv) != 2:
    raise ValueError("Please provide correct parameters. Usage: ./stargazer.py AB1231988...NAUT")
else:
    gazer = Stargazer(sys.argv[1], dry_run=True)
    gazer.poll()
