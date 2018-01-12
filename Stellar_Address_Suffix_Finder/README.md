# Stellar Address Suffix Finder
----

SASF is a small tool to find the suffix you want for your public address.

## How to use

It is always better to create a virtual environment to isolate Python environments (optional).

Assuming you already have a working Python environment set up, open up a terminal and refer to [the virtualenv webpage][virtualenv rtd] to set up everything.

**Installing the dependencies**

Now that you have the sourced the environment, run the following to install the dependencies:

`pip install -r requirements.txt`

### Using the tool

It's pretty straightforward, you only need to provide the suffix you want as a positional argument. For example if you want to find `ABC`, you'll need to type:

`python sasf.py ABC`

[virtualenv rtd]: <https://virtualenv.pypa.io/en/stable/>