ConfigDeployer Core Module
==========================

ConfigDeployer is a Java framework designed for the deployment of software configurations in different installation environments using XML format profiles.

Getting started
---------------

### Obtaining the source code

Install *Git* on your computer, open the *Git Bash* and navigate to the directory where you want to check-out the repository. Then execute the following command:

`git clone https://github.com/georgwittberger/configdeployer-core.git`

You will get a new sub-directory named `configdeployer-core` which contains the Maven project.

### Building the binaries

Install *Maven* on your computer, open a terminal and navigate to the directory where the ConfigDeployer project resides. Then execute the following command:

`mvn install`

You will get a new sub-directory named `target` which contains the binary JAR file `configdeployer-core-VERSION.jar`. Note that *VERSION* is the version number you have downloaded.

### Integrating with your project

To use the ConfigDeployer framework in your own Java project you could...

- Manually add the binary JAR file "configdeployer-core-VERSION.jar" to the classpath of your project.
- Install the artifact in your Maven repository and use it as a dependency in your POM:
  1. Open a terminal, navigate to the directory where the JAR file resides and execute the following command: `mvn install:install-file -Dfile=configdeployer-core-VERSION.jar`
  2. Add the artifact as a dependency to your POM:
  `<dependency>
    <groupId>com.configdeployer</groupId>
    <artifactId>configdeployer-core</artifactId>
    <version>VERSION</version>
  </dependency>`

Note that *VERSION* stands for the version number you want to use.

Creating a profile
------------------

There is a XML schema definition (XSD) you can use to create and validate a profile: `/src/main/xsd/config-profile.xsd`. Here is a simple properties file example:

`<?xml version="1.0" encoding="UTF-8"?>
<config-profile name="Example" version="1.0" xmlns="http://tsmms.com/utils/configdeployer/profile"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://tsmms.com/utils/configdeployer/profile config-profile.xsd ">
  <properties-files>
    <properties-file location="/etc/myconfig.properties">
      <entry operation="SET" key="my.property.key" value="My new property value"/>
    </properties-file>
  </properties-files>
</config-profile>`

This profile will change the value of the key `my.property.key` within the properties file `/etc/myconfig.properties` to the new value `My new property value` (or create this entry if it does not exist).

Please refer to the documentation for more details on the profile structure: `/src/main/site/index.html`.

Deploying a profile
-------------------

Use the `ProfileDeployer` class to roll-out the configuration:

`InputStreamProvider inputStreamProvider = new FileInputStreamProvider(new File("myprofile.xml"));
try {
  ProfileDeployer profileDeployer = new ProfileDeployer(new ProfileProvider(inputStreamProvider));
  boolean success = profileDeployer.deploy();
  // check if deployment was successful
} catch (DeployerException e) {
  // do some error handling
}`

You may use additional preparers to do the pre-processing before the profile is applied to the environment. This allows you to substitute variables in the profile with their environment-specific values. Please refer to the documentation for more details on deployment: `/src/main/site/index.html`.
