ConfigDeployer Core Module
==========================

ConfigDeployer is a Java framework designed for the deployment of software configurations in different installation environments using XML format profiles.

Getting started
---------------

### Obtaining the source code

Install [Git](http://git-scm.com/downloads) on your computer, open the Git Bash and navigate to the directory where you want to check-out the repository. Then execute the following command:

    git clone https://github.com/georgwittberger/configdeployer-core.git

You will get a new sub-directory named `configdeployer-core` which contains the Maven project.

### Building the binaries

Install [Maven](http://maven.apache.org/download.cgi) on your computer, open a terminal and navigate to the directory where the ConfigDeployer project resides. Then execute the following command:

    mvn install

You will get a new sub-directory named `target` which contains the binary JAR file `configdeployer-core-VERSION.jar`. Note that *VERSION* is the version number you have downloaded.

### Integrating with your project

To use the ConfigDeployer framework in your own Java project you could...

-   Manually add the binary JAR file `configdeployer-core-VERSION.jar` to the classpath of your project.
-   Install the artifact in your Maven repository and use it as a dependency in your POM:
    1.  Open a terminal, navigate to the directory where the JAR file resides and execute the following command: `mvn install:install-file -Dfile=configdeployer-core-VERSION.jar`
    2.  Add the artifact as a dependency to your POM:

            <dependency>
              <groupId>com.configdeployer</groupId>
              <artifactId>configdeployer-core</artifactId>
              <version>VERSION</version>
            </dependency>

Note that *VERSION* stands for the version number you want to use.

Creating a profile
------------------

There is a XML schema definition (XSD) which will help you to create and validate a profile: `/src/main/xsd/config-profile.xsd`. You may generate an example XML file from this schema using your favorite XML tool (e.g. XMLSpy or Eclipse).

### Changing properties files

Properties files can be modified using the `properties-files` section within the profile:

    <?xml version="1.0" encoding="UTF-8"?>
    <config-profile name="Example" version="1.0" xmlns="http://configdeployer.com/profile"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://configdeployer.com/profile config-profile.xsd">
      <properties-files>
        <properties-file location="/etc/config/myconfig.properties" operation="CREATE">
          <entry operation="ADD" key="KEY_TO_ADD" value="VALUE_FOR_ADDED_KEY"/>
          <entry operation="UPDATE" key="KEY_TO_UPDATE" value="NEW_VALUE_FOR_EXISTING_KEY"/>
          <entry operation="SET" key="KEY_TO_SET" value="VALUE_FOR_ADDED_OR_UPDATED_KEY"/>
          <entry operation="DELETE" key="KEY_TO_DELETE"/>
          <entry operation="RENAME" key="KEY_TO_RENAME" value="NEW_KEY_NAME"/>
          <entry operation="SET" key="KEY_TO_SET" value="VALUE_FOR_ADDED_OR_UPDATED_KEY">
            <condition value-contains="STRING_THAT_MUST_BE_CONTAINED_IN_CURRENT_VALUE"/>
            ...
          </entry>
          ...
        </properties-file>
        ...
      </properties-files>
    </config-profile>

This profile example will add, update, delete and rename certain entries within the properties file `/etc/config/myconfig.properties`. There is also a conditional setting which is only applied if the current property value contains the given string.

Please refer to the documentation for more details: `/src/main/site/index.html`.

### Changing database rows

Rows of database tables can be modified using the `databases` section within the profile:

    <?xml version="1.0" encoding="UTF-8"?>
    <config-profile name="Example" version="1.0" xmlns="http://configdeployer.com/profile"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://configdeployer.com/profile config-profile.xsd">
      <databases>
        <database driver-class="oracle.jdbc.OracleDriver" jdbc-url="jdbc:oracle:thin:@myhost:1521:mydb"
            username="scott" password="secret">
          <entry operation="ADD" table-name="MYTABLE">
            <column-value name="COLUMN_1" type="VARCHAR" value="VALUE_FOR_COLUMN_1"/>
            <column-value name="COLUMN_2" type="INTEGER" value="42"/>
            ...
          </entry>
          <entry operation="UPDATE" table-name="MYTABLE" condition="COLUMN_2 &lt; 42">
            <column-value name="COLUMN_1" type="VARCHAR" value="NEW_VALUE_FOR_COLUMN_1"/>
            <column-value name="COLUMN_3" type="CLOB" value="NEW_VALUE_FOR_COLUMN_3"/>
            ...
          </entry>
          <entry operation="DELETE" table-name="MYTABLE" condition="COLUMN_1 IN ('foo', 'bar')"/>
          ...
        </database>
        ...
      </databases>
    </config-profile>

This profile example will use the Oracle JDBC driver to connect to `mydb` located on `myhost`. The credentials for authentication are provided in separate attributes. The profile adds, updates and deletes rows in the table `MYTABLE`. Note that for update and delete operations a condition must be provided to ensure that not the whole table is modified by accident.

Please refer to the documentation for more details: `/src/main/site/index.html`.

Deploying a profile
-------------------

### Basics

You can use the `ProfileDeployer` class in conjunction with the `InputStreamProvider` implementations to roll-out a configuration profile:

    InputStreamProvider inputStreamProvider = new FileInputStreamProvider(new File("myprofile.xml"));
    try {
      ProfileDeployer profileDeployer = new ProfileDeployer(new ProfileProvider(inputStreamProvider));
      boolean success = profileDeployer.deploy();
      // check if deployment was successful
    } catch (DeployerException e) {
      // do some error handling
    }

In this example the profile is read from a local file `myprofile.xml`. If your profile is GZip-compressed you can wrap around the `GZipInputStreamProvider`:

    InputStreamProvider inputStreamProvider = new GZipInputStreamProvider(new FileInputStreamProvider(new File("myprofile.xml")));

The `ProfileDeployer` will apply changes to properties files first and then take care of the database modifications. The entries are processed sequentially in the order they appear in the profile. A `properties-file` element is processed as a transaction, so if saving the new file state fails none of the changes in that element will be rolled-out. The same rule applies to the `database` element - all contained table modifications are performed inside a single transaction and rolled-back if one change fails to complete successfully. Nevertheless, all sibling properties files and databases configurations are processed independently. If you need a transaction on the whole profile (e.g. to roll-back everything if only one file update fails) you will have to sub-class the `ProfileDeployer` and override the `backupConfiguration()` and `restoreConfiguration()` to implement your custom transaction handling.

### Using variables
 
You can add several preparers to the `ProfileDeployer` which will pre-process the profile before the actual deployment starts. One of the most common preparers is the `VariablesPreparer` enables substitution of variables in certain elements:

    VariablesPreparer variablesPreparer = new VariablesPreparer();
    variablesPreparer.setDepth(2);
    variablesPreparer.addResolver("env", new EnvironmentVariablesResolver());
    variablesPreparer.addResolver("sys", new SystemPropertiesResolver());
    ProfileDeployer profileDeployer = new ProfileDeployer(new ProfileProvider(inputStreamProvider), variablesPreparer);

This set-up will activate substitution of environment variables and Java system properties in the location of properties files and the connection configuration of databases. The pattern `${env:foo}` will be resolved to the value of the environment variable `foo` and `${sys:user.home}` will be resolved to the system property `user.home`. In this example the preparer is configured to perform two cycles of substitution which allows the usage of variables in variable values (e.g. a system property in the value of an environment variable).

Please refer to the documentation for more details: `/src/main/site/index.html`.
