package com.configdeployer.deployer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.configdeployer.binding.ConfigProfile;
import com.configdeployer.binding.ProfileMarshaller;
import com.configdeployer.binding.PropertiesAddEntry;
import com.configdeployer.binding.PropertiesDeleteEntry;
import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.binding.PropertiesSetEntry;
import com.configdeployer.binding.PropertiesUpdateEntry;
import com.configdeployer.preparer.SystemPropertiesResolver;
import com.configdeployer.preparer.VariablesPreparer;
import com.configdeployer.provider.FileInputStreamProvider;
import com.configdeployer.provider.ProfileProvider;

public class ProfileDeployerPropertiesTest
{

    private File temporaryPropertiesFile;
    private File userHomePropertiesFile;
    private File temporaryProfileFile;

    @BeforeClass
    public void setUp()
    {
        Properties existingProperties = new Properties();
        existingProperties.setProperty("existing.key", "original.existing.value");
        existingProperties.setProperty("existing.123", "original.123.value");
        existingProperties.setProperty("existing.456", "original.456.value");
        existingProperties.setProperty("existing.obsolete", "original.obsolete.value");
        existingProperties.setProperty("existing.remain", "original.remain.value");

        Writer temporaryPropertiesFileWriter = null;
        try
        {
            temporaryPropertiesFile = File.createTempFile("test-properties", ".properties");
            temporaryPropertiesFileWriter = new FileWriter(temporaryPropertiesFile);
            existingProperties.store(temporaryPropertiesFileWriter, null);
        }
        catch (IOException e)
        {
            Assert.fail("Could not create temporary properties file!", e);
        }
        finally
        {
            if (temporaryPropertiesFileWriter != null)
            {
                try
                {
                    temporaryPropertiesFileWriter.close();
                }
                catch (IOException e)
                {
                    Assert.fail("Could not close temporary properties file writer!", e);
                }
            }
        }

        userHomePropertiesFile = new File(new File(System.getProperty("user.home")), "configdeployer-test.properties");
        if (userHomePropertiesFile.exists())
        {
            userHomePropertiesFile.delete();
        }

        ConfigProfile profile = new ConfigProfile();
        profile.setName("Test profile");
        profile.setVersion("1.0");
        PropertiesFile propertiesFile = new PropertiesFile();
        propertiesFile.setLocation(temporaryPropertiesFile.getAbsolutePath());
        PropertiesAddEntry addEntry = new PropertiesAddEntry();
        addEntry.setKey("non.existing.key");
        addEntry.setValue("non.existing.value");
        propertiesFile.getAddOrUpdateOrSet().add(addEntry);
        PropertiesUpdateEntry updateEntry = new PropertiesUpdateEntry();
        updateEntry.setKeyPattern("existing\\.\\d+");
        updateEntry.setValue("new.regex.value");
        propertiesFile.getAddOrUpdateOrSet().add(updateEntry);
        PropertiesSetEntry setEntry = new PropertiesSetEntry();
        setEntry.setKey("existing.key");
        setEntry.setValue("new.existing.value");
        propertiesFile.getAddOrUpdateOrSet().add(setEntry);
        PropertiesDeleteEntry deleteEntry = new PropertiesDeleteEntry();
        deleteEntry.setKey("existing.obsolete");
        propertiesFile.getAddOrUpdateOrSet().add(deleteEntry);
        profile.getPropertiesFileOrDatabase().add(propertiesFile);
        propertiesFile = new PropertiesFile();
        propertiesFile.setLocation("${sys:user.home}/" + userHomePropertiesFile.getName());
        addEntry = new PropertiesAddEntry();
        addEntry.setKey("user.home.key");
        addEntry.setValue("user.home.value");
        propertiesFile.getAddOrUpdateOrSet().add(addEntry);
        profile.getPropertiesFileOrDatabase().add(propertiesFile);

        try
        {
            temporaryProfileFile = File.createTempFile("test-profile", ".xml");
            ProfileMarshaller.getInstance().marshal(profile, temporaryProfileFile);
        }
        catch (IOException e)
        {
            Assert.fail("Could not create temporary profile file!", e);
        }
        catch (JAXBException e)
        {
            Assert.fail("Could not marshal temporary profile to file!", e);
        }
    }

    @Test(groups = { "deployer" }, description = "Deploying a configuration profile for properties files with variable substitution")
    public void testProfileDeployer()
    {
        ProfileProvider profileProvider = new ProfileProvider(new FileInputStreamProvider(temporaryProfileFile));
        VariablesPreparer variablesPreparer = new VariablesPreparer();
        variablesPreparer.addResolver("sys", new SystemPropertiesResolver());
        ProfileDeployer profileDeployer = new ProfileDeployer(profileProvider, variablesPreparer);
        try
        {
            profileDeployer.deploy();
        }
        catch (DeployerException e)
        {
            Assert.fail("Could not deploy test profile!", e);
        }

        Properties modifiedProperties = new Properties();
        Reader temporaryPropertiesFileReader = null;
        try
        {
            temporaryPropertiesFileReader = new FileReader(temporaryPropertiesFile);
            modifiedProperties.load(temporaryPropertiesFileReader);
        }
        catch (IOException e)
        {
            Assert.fail("Could not load temporary properties file!", e);
        }
        finally
        {
            if (temporaryPropertiesFileReader != null)
            {
                try
                {
                    temporaryPropertiesFileReader.close();
                }
                catch (IOException e)
                {
                    Assert.fail("Could not close temporary properties file reader!", e);
                }
            }
        }

        Assert.assertEquals(modifiedProperties.getProperty("non.existing.key"), "non.existing.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.123"), "new.regex.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.456"), "new.regex.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.key"), "new.existing.value");
        Assert.assertNull(modifiedProperties.getProperty("existing.obsolete"));
        Assert.assertEquals(modifiedProperties.getProperty("existing.remain"), "original.remain.value");

        Properties userHomeProperties = new Properties();
        Reader userHomePropertiesFileReader = null;
        try
        {
            userHomePropertiesFileReader = new FileReader(userHomePropertiesFile);
            userHomeProperties.load(userHomePropertiesFileReader);
        }
        catch (IOException e)
        {
            Assert.fail("Could not load user home properties file!", e);
        }
        finally
        {
            if (userHomePropertiesFileReader != null)
            {
                try
                {
                    userHomePropertiesFileReader.close();
                }
                catch (IOException e)
                {
                    Assert.fail("Could not close user home properties file reader!", e);
                }
            }
        }

        Assert.assertEquals(userHomeProperties.getProperty("user.home.key"), "user.home.value");
    }

    @AfterClass
    public void tearDown()
    {
        if (temporaryPropertiesFile != null && temporaryPropertiesFile.isFile())
        {
            temporaryPropertiesFile.delete();
        }
        if (temporaryProfileFile != null && temporaryProfileFile.isFile())
        {
            temporaryProfileFile.delete();
        }
        if (userHomePropertiesFile != null && userHomePropertiesFile.isFile())
        {
            userHomePropertiesFile.delete();
        }
    }

}
