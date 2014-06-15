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

The source code directory `/src/main/resources/com/configdeployer/binding` contains a XML schema file for the configuration profile and an example profile which can be used as a template.

Every configuration profile must have the root element `config-profile` with at least the two attributes `name` and `version` (version is currently not used internally). The root element should contain at least one of the possible target resources `properties-file` or `database`:

    <?xml version="1.0" encoding="UTF-8"?>
    <config-profile name="Example Profile" description="Optional description" version="1.0"
        xmlns="http://configdeployer.com/profile" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://configdeployer.com/profile config-profile.xsd ">
        (at least one properties-file or database element)
    </config-profile>

The resources within the profile are processed in the order they appear in the XML file.

*Important:* Special XML characters like < > & " must be encoded to their respective entities when used in attribute values!

### Changing properties files

Configurations stored in properties files can be modified using the `properties-file` element within the profile:

    <?xml version="1.0" encoding="UTF-8"?>
    <config-profile name="Example Profile" description="Optional description of the profile" version="1.0"
        xmlns="http://configdeployer.com/profile" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://configdeployer.com/profile config-profile.xsd ">
        <properties-file location="${env:BASEDIR}/basic.properties">
            <add key="added.key" value="Entry which is only added if it does not exist" />
            <update key="updated.key" value="Value which is only set if key already exists" />
            <set key="set.key" value="Value which is always set" />
            <delete key="deleted.key" />
            <rename key="old.key" new-key="new.key" />
        </properties-file>
    </config-profile>

The `properties-file` element must have a `location` attribute specifying the path in the local file system. Note that you can use variables in this attribute if the `VariablesPreparer` is used during deployment.

There should be at least one of the following child elements defining the operations to perform on the properties entries:

-   `add` will create the entry if the key does not exist yet. This is useful to push default configurations to an environment that may already have its specific settings which should not be overwritten.
    -   Requires the `key` attribute which defines the key to insert into the configuration.
    -   Optionally may have the `value` attribute holding the value to set for the key.
    -   May have the `comment` attribute to insert a description right before the new entry.
-   `update` will change the value of existing entries only. If the key does not exist, nothing is added to the configuration.
    -   Requires either the `key` attribute to specify a single entry or the `key-pattern` attribute to define a regular expression selecting the entries to update.
    -   Requires the `value` attribute holding the new value to set for the selected key(s).
    -   Optionally can have the `comment` attribute to insert a description right before the updated entry/entries.
-   `set` will simply push the given value to the specified key. Existing entries are updated, non-existing entries are created.
    -   Requires the `key` attribute which defines the key to set a value for.
    -   Requires the `value` attribute specifying the value to set for the key.
    -   May have the `comment` attribute to insert a description right before the entry.
-   `delete` will remove the defined key(s) from the configuration.
    -   Requires the `key` attribute to specify a single entry or the `key-pattern` attribute to define a regular expression selecting the entries to delete.
    -   Cannot have a `comment` since this does not make sense when removing the keys anyway.
-   `rename` will change the name of the specified key(s) preserving their current value(s).
    -   Requires the `key` attribute to specify a single entry or the `key-pattern` attribute to define a regular expression selecting the entries to rename.
    -   Requires the `new-key` attribute holding the new name of the entry/entries. *Important:* When using a `key-pattern` make sure that the `new-key` attribute contains a back-reference to some sub-pattern of the old key (using `$n` for the n-th sub-pattern or `$0` for the entire old key). Example:
        
            <rename key-pattern="old\.(\d+)" new-key="new.$1" />
        
    -   Optionally can have the `comment` attribute to insert a description right before the renamed entry/entries.

### Using conditions on properties file operations

Every operation element except `add` may have an arbitrary number of `condition` child elements. If at least one of those elements exists all the given conditions are checked against the current configuration state of the property before the change is applied.

    <properties-file location="${sys:user.home}/conditional.properties">
        <set key="conditional.string" value="Value which is only set if current value is 'foo'">
            <condition value-equals="foo" />
        </set>
        <update key="conditional.regex" value="Value which is only set if current value is at least one digit">
            <condition value-matches="\d+" />
        </update>
    </properties-file>

There are multiple conditions which can be checked on the current property value:

-   `value-equals` is true if the current value is exactly the given test string (case-sensitive)
-   `value-contains` is true if the current value contains the given sub-string (case-sensitive)
-   `value-matches` is true if the current value matches the given regular expression
-   `value-not-equals` is true if the current value is *not* exactly the given test string (case-sensitive)
-   `value-not-contains` is true if the current value does *not* contain the given sub-string (case-sensitive)
-   `value-not-matches` is true if the current value does *not* match the given regular expression

If the `condition` element has multiple attributes then each must be satisfied by the current value to render the condition true. If there are multiple `condition` elements within the operation element then the change is only applied if every single condition is satisfied.

### Removing entire properties files

If a whole properties file is not needed any more it can be deleted during deployment using the `operation="DELETE"` attribute on the `properties-file` element (which should not contain any operation elements in this case).

*Warning:* This will physically delete the entire file without prompt. Use with caution!

### Changing database rows

Configurations stored in database tables can be modified using the `database` element within the profile:

    <?xml version="1.0" encoding="UTF-8"?>
    <config-profile name="Example Profile" description="Optional description of the profile" version="1.0"
        xmlns="http://configdeployer.com/profile" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://configdeployer.com/profile config-profile.xsd ">
        <database driver-class="oracle.jdbc.OracleDriver" jdbc-url="jdbc:oracle:thin:@myhost:1521:orcl"
            username="${env:DBUSER}" password="${env:DBPWD}">
            <add table="actors">
                <set column="firstname" type="VARCHAR" value="Emily" />
                <set column="lastname" type="VARCHAR" value="Blunt" />
            </add>
            <update table="movies" condition="title = 'Edge of Tomorrow'">
                <set column="genre" type="VARCHAR" value="Sci-Fi" />
            </update>
            <delete table="movies" condition="year &lt; 1970" />
        </database>
    </config-profile>

The `database` element must have at least the two attributes `driver-class` defining the full-qualified name of the database driver Java class and `jdbc-url` which takes the JDBC connection string. Optionally you can provide credentials for authentication by using the attributes `username` and `password`. Note that you can use variables in all these attributes if the `VariablesPreparer` is used when deploying the profile.

There should be at least one of these operation child element within the `database` element:

-   `add` will insert a new row to the given table if it does not contain one with exactly the same values.
    -   Requires the `table` attribute defining the name of the table to add the row to.
    -   Requires at least one `set` child element which defines a value to set in a certain column.
-   `update` will change the values in certain columns of existing rows in the specified table.
    -   Requires the `table` attribute defining the name of the table to update.
    -   Requires the `condition` attribute holding a SQL where clause which selects the rows that should be updated.
    -   Requires at least one `set` child element which defines a value to set in a certain column.
-   `delete` will remove all selected rows from the given table.
    -   Requires the `table` attribute defining the name of the table to delete rows from.
    -   Requires the `condition` attribute to specify a SQL where clause selecting the rows to delete.

The `set` elements required by the `add` and `update` operations must have at least the two attributes `column` (name of the column) and `type` (column data type, see DDL of the table). If no `value` attribute is provided the application tries to set `NULL` in this column.

*Remember:* Special XML characters in attribute values of the configuration profile must be encoded! Keep an eye on that when dealing with `condition` attributes!

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

    InputStreamProvider inputStreamProvider = new GZipInputStreamProvider(
        new FileInputStreamProvider(new File("myprofile.xml.gz")));

The `ProfileDeployer` will process the target resources in the order they appear in the XML file. The operations for a resource are processed sequentially as well. A `properties-file` element is processed as a transaction, so if saving the new file state fails none of the changes in that element will be rolled-out. The same rule applies to the `database` element - all contained table modifications are performed inside a single transaction and rolled-back if one change fails.

Nevertheless, all target resources are processed independently. You can set the `FailFast` flag on the `ProfileDeployer` to `true` if you want to stop deployment immediately once a failure occurs. However, all changes to resources that have already been processed will *not* be rolled-back. If you need a transaction on the whole profile (i.e. to roll-back everything if only one resource update fails) you will have to sub-class the `ProfileDeployer` and override the `backupConfiguration()` and `restoreConfiguration()` methods to implement your custom transaction handling.

### Using variables

You can add several preparers to the `ProfileDeployer` which will pre-process the profile before the actual deployment starts. One of the most common preparers is the `VariablesPreparer`. It enables substitution of variables in certain elements:

    VariablesPreparer variablesPreparer = new VariablesPreparer();
    variablesPreparer.setDepth(2);
    variablesPreparer.addResolver("env", new EnvironmentVariablesResolver());
    variablesPreparer.addResolver("sys", new SystemPropertiesResolver());
    variablesPreparer.addResolver("p", new PropertiesVariablesResolver(new File("myconfig.properties")));
    
    ProfileDeployer profileDeployer = new ProfileDeployer(new ProfileProvider(inputStreamProvider), variablesPreparer);

With the `setDepth()` method it is possible to specify the maximum number of iterations to perform during variable substitution (Default = 1). This can be useful if environment variables can contain patterns for other variables that need to be resolved in a second run.

The `VariablesPreparer` can use an arbitrary number of resolvers to look for variable values. Each resolver is registered via the `addResolver()` method. You have to provide a prefix and a resolver instance. In the example above the variable `${env:foo}` will be resolved by the `EnvironmentVariablesResolver` and `${sys:user.home}` would be processed using the `SystemPropertiesResolver`.

The following pre-defined variable resolvers are available:

-   `EnvironmentVariablesResolver` will lookup the value in the environment variables passed by the system to the Java process. This is useful to keep file locations and database connection settings in the profile independent from a specific environment.
-   `SystemPropertiesResolver` will lookup the value in the Java system properties. This can be used to obtain the user's home directory or the current working directory.
-   `PropertiesVariablesResolver` will lookup the value in a separate properties file. A good way to separate environment-specific values from the profile itself.
