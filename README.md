# sosmessagedecarte - Admin app

## Overview

The messages are stored in a mongoDB database.

The administration interface is done using [Play 2.0](http://www.playframework.org/2.0).

## MongoDB

To install it on your system, see [here](http://www.mongodb.org/display/DOCS/Quickstart).

### Casbah

We are using the non released version 3.0.0-SNAPSHOT due to some bugs fixed in this version.

Before running the Administration or API app, you need to install on you local repository the 3.0.0-SNAPSHOT version of Casbah.

	$ git clone git://github.com/mongodb/casbah.git
	$ cd casbah
	$ sbt publish-local

## Administration

### Install Play 2.0

See the **Building from sources** section [here](https://github.com/playframework/Play20/wiki/Installing).

### Launch the server

	$ cd backend/sosmessage-admin
	$ play run

The application will be accessible at `http://localhost:9000/`.
