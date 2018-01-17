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
Move to a folder where you would like to contain the tools and run `git clone https://github.com/Lumenaut-Network/tools.git lumeanuttools`, 
where `lumenauttools` is the destination folder (it will be created for you). You now have the latest version of the source 
code.

## Updating from the repository
To update the tool open a command prompt where the tool is located and run `git pull`. If your version is already up to date 
it will let you know.

## Running the application
To execute the application run `gradlew run`. If updates were downloaded with git the application we'll be recompiled for 
you and run immediately 

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

