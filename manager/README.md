# Security Disclaimer
We will never provide prebuilt binaries for this tool. If you download it, and it was compiled by someone else, make sure
it has not been tampered with! Your best bet for a secure operation is to always work from a fresh copy of the tool from 
this repository.

# Prerequisites
* JDK 1.8.x (Java 9 is not supported)
* Git
* IntelliJ IDEA community edition (optional, only needed if you want to customize the code)

# Windows setup
* Download and install the latest version of the [Java Development Kit 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Make sure that the java compiler is installed by opening a command prompt and launching the command `javac -version`, 
which yould output something like this:
```bash
javac 1.8.0_152
```
* Download and install [git for windows](http://gitforwindows.org/)
* Make sure that git is installed by launching the command `git --version`, which should output something like this:
```bash
git version 2.15.1.windows.2
``` 

## Cloning the repository
Move to a folder where you would like to contain the tools and run `git clone https://github.com/Lumenaut-Network/tools.git lumenauttools`, 
where `lumenauttools` is the destination folder (it will be created for you). You now have the latest version of the source 
code.

## Updating from the repository
To update the tool open a command prompt where the tool is located and run `git pull`. If your version is already up to date 
it will let you know.

## Running the application
Move to the manager's folder `cd lumenauttools\manager` and launch the application build and run with: `gradlew run`. If updates have been 
downloaded that require a new compilation, that will be done automatically for you.

## How to use
Read the [Pool Manager's User Manual](https://github.com/Lumenaut-Network/tools/wiki/Pool-Manager) in the wiki!

## Install horizon node with docker
* Install [Docker](https://docs.docker.com/engine/installation/)
* Read [Docker Stellar Core Horizon](https://github.com/stellar/docker-stellar-core-horizon)
* Run (example):
```bash
docker run --rm -it -p "5432:5432" -v "/home/<username>/stellar:/opt/stellar" --name stellar stellar/quickstart --testnet
```

# Linux setup
* Download and install the latest version of the [Java Development Kit 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Make sure that the java compiler is installed by opening a command prompt and launching the command `javac -version`, 
which yould output something like this:
```bash
javac 1.8.0_152
```
* Install git package with:
    * Fedora :
        
        ```bash
        dnf install git
        ```
    * Ubuntu/Debian
    
        ```bash
        apt-get install git
        ```
    * CentOS/Fedora(<22)
        
        ```bash
        yum install git
        ```
* Make sure that git is installed by launching the command `git --version`, which should output something like this:
```bash
git version 2.14.2
``` 
## Cloning the repository
Move to a folder where you would like to contain the tools and run `git clone https://github.com/Lumenaut-Network/tools.git lumenauttools`, 
where `lumenauttools` is the destination folder (it will be created for you). You now have the latest version of the source 
code.

## Updating from the repository
To update the tool open a command prompt where the tool is located and run `git pull`. If your version is already up to date 
it will let you know.

## Running the application
Move to the manager's folder `cd lumenauttools\manager`, if you haven't done so, make the gradle wrapper executable: `chmod +x gradlew`, and launch the application build and run with: `/gradlew run`. If updates have been 
downloaded that require a new compilation, that will be done automatically for you.

## How to use
Read the [Pool Manager's User Manual](https://github.com/Lumenaut-Network/tools/wiki/Pool-Manager) in the wiki!

## Install horizon node with docker
* Install [Docker](https://docs.docker.com/engine/installation/)
* Read [Docker Stellar Core Horizon](https://github.com/stellar/docker-stellar-core-horizon)
* Run (example):
```bash
docker run --rm -it -p "5432:5432" -v "/home/<username>/stellar:/opt/stellar" --name stellar stellar/quickstart --testnet
```

# LICENSE
Copyright 2018 Lumenaut Network (https://lumenaut.net)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

