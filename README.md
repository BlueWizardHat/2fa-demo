Two-Factor Authentication Demo Project
=============================================================================

# Purpose

To many sites that handles sensitive or financial information uses only simple
password authentication.

This project provides an example of how little work is needed to implement
Two-Factor Authentication with a Google Authenticator mobile app or a Yubico
Yubikey, at least in a Java project.


# Support

The instructions here assume that you know what you are doing and are therefore
not particularly detailed. Unless you found this project by being pointed at it
by me directly no support whatsoever will be provided.


# Building

The project builds using Gradle (v2.4+), so you will need to have Gradle installed
to build.

Also the project is configured to use PostgreSQL as the database, if you want
to use mysql change that in

	2fa-demo-webapp/src/main/resources/application.properties.

Once gradle is installed and you have choosen a database just run

	gradle clean build

If all goes well the finished war file will reside in 2fa-demo-webapp/build/libs


# Deployment

The project requires a Servlet 3.0+ container, Java 7+ and a PostgreSQL or MySQL database.
The project has been tested under the following configurations

* Java 8, Tomcat 8.0.9, PostgreSQL 9.3.x
* Java 8, Tomcat 7.0.55, PostgreSQL 9.3.x
* Java 7, Tomcat 8.0.9, PostgreSQL 9.3.x
* Java 7, Tomcat 7.0.55, PostgreSQL 9.3.x
* Java 7, Tomcat 7.0.55, MySQL 5.5.x

I have used no container-specific or db-specific code so it should work on other
configurations but I give no guarantees about that.

In your container you need to setup a datasource with the name found in 

	2fa-demo-webapp/src/main/resources/application.properties

Unless you change it, it is set to "jdbc/pgtestdb". Refer to your servlet containers documentation
for how to set up a datasource.

Deploying in a standard tomcat is as simple as copying the war file to the webapps dir.

Then just point you browser to the url that you deployed it on and check out the magic.


# Importing into an IDE

If you want to import the project into an IDE make sure you have installed the excellent
[lombok](http://projectlombok.org/) v1.14.4 or higher in your IDE before doing it.

The project is already ready for eclipse, just run 

	gradle eclipse

to get the project set up for importing into eclipse.


@bluewizardhat
