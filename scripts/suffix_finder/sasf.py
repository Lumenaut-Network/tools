# -*- coding: utf-8 -*-
"""
    Stellar Address Suffix Finder
"""

from stellar_base.keypair import Keypair
import argparse
import sys
import os

__author__ = "Anthony Da Mota"
__credits__ = ["Lumenaut Network", "AkdM"]
__version__ = "1.0.0"
__maintainer__ = "Anhony Da Mota"


class XLMAddress:
  def __init__(self):
    self.tries = 0
    self.addressFound = False
    self.suffixToFind = ""
    self.keyPair = False

  def clear_and_write_console(self, finding):
    if os.name == "nt":
        os.system("cls")
    else:
        os.system("clear")
    if finding:
      print "[Stellar Address Suffix Finder - Finding '{}' - v{}]\n".format(self.suffixToFind, __version__)
    else: 
      print "[Stellar Address Suffix Finder - v{}]\n".format(__version__)
  
  def generateAddress(self):
    self.keyPair = Keypair.random()
  
  def findAddress(self, suffixToFind):
    self.suffixToFind = suffixToFind
    while not self.addressFound:
      self.tries += 1
      if self.tries % 5000 == 0:
        self.clear_and_write_console(True)
        print "{} tries".format(self.tries)
      self.generateAddress()
      if self.keyPair.address().decode().endswith(self.suffixToFind):
        self.addressFound = True
        self.clear_and_write_console(False)
        print "Found one ending with '{}' in {} tries".format(self.suffixToFind, self.tries)
        print "Public Key = {}".format(self.keyPair.address().decode())
        print "Secret Key = {}".format(self.keyPair.seed().decode())

        print "\n#########################################################"
        print "# If you want it, save BOTH the secret AND public keys! #"
        print "#########################################################"

def arguments():
  parser = argparse.ArgumentParser()
  parser.add_argument('s', type=str, help='Suffix to find')
  args = parser.parse_args()
  return args

def main(argv):
  try:
    args = arguments()
    addr = XLMAddress()
    addr.findAddress(args.s.upper())
  except KeyboardInterrupt:
    print "\nExiting...\n"
    sys.exit()

if __name__ == "__main__":
  main(sys.argv[1:])