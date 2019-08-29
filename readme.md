#### master
[![Build Status](https://travis-ci.org/FAForever/downlords-faf-client.svg?branch=master)](https://travis-ci.org/FAForever/downlords-faf-client)
[![Coverage Status](https://coveralls.io/repos/github/FAForever/downlords-faf-client/badge.svg?branch=develop)](https://coveralls.io/github/FAForever/downlords-faf-client?branch=develop)

Please see the [project page](http://FAForever.github.io/downlords-faf-client/)

Please take a look at the [contribution guidelines](https://github.com/FAForever/java-guidelines/wiki/Contribution-Guidelines) before creating a pull request


# How to run

1. Clone the project with git
1. Open the project into IntelliJ Ultimate
1. Make sure you have the IntelliJ [Lombok plugin](https://plugins.jetbrains.com/idea/plugin/6317-lombok-plugin) installed
1. Make sure you have `Enable annotation processing` enabled in the settings
1. Run the Gradle task `:webview-patch build` to build the webview-patch JAR which is referenced by the run configurations
1. Run the Gradle task `:downlords-faf-client downloadNativeDependencies` to download the ICE adapter

# Open Source licenses used 
<img src="https://www.ej-technologies.com/images/product_banners/install4j_large.png" width="128">Thanks to [ej-technologies](https://www.ej-technologies.com) for their [open source license](https://www.ej-technologies.com/buy/install4j/openSource) for Install4j. We use Install4j to build installers.

<img src="https://slack-files2.s3-us-west-2.amazonaws.com/avatars/2017-12-13/286651735269_a5ab3167acef52b0111e_512.png" width="96">Thanks to [bugsnag](https://www.bugsnag.com) for their [open source license](https://www.bugsnag.com/open-source/). We use bugsnag for our error reporting.

<img src="https://faforever.github.io/downlords-faf-client/images/yklogo.png" width="48"> Thanks to [YourKit](https://www.yourkit.com) for their open source license

# See how to use IntelliJ Community Edition

We have a YouTube video on it [here](https://youtu.be/8EwK16kk0BE). In case you are a student or have other ways to acquire it, choose IntelliJ Ultimate over the Community Edition.

# Interested in contributing?

Have a look at our [wiki](https://github.com/FAForever/downlords-faf-client/wiki) .

# How to install on Linux
https://github.com/FAForever/downlords-faf-client/wiki/Install-on-Linux
