package com.configdeployer.deployer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.configdeployer.binding.PropertiesFile;

public abstract class AbstractPropertiesFileDeployerTest
{

    private File temporaryPropertiesFile;
    private PropertiesFile propertiesFileProfile;

    @BeforeClass
    public void setUp()
    {
        try
        {
            temporaryPropertiesFile = File.createTempFile("test-properties", ".properties");
        }
        catch (IOException e)
        {
            Assert.fail("Could not create temporary properties file!", e);
        }

        Properties existingProperties = createExistingProperties();
        Writer temporaryPropertiesFileWriter = null;
        try
        {
            temporaryPropertiesFileWriter = new FileWriter(temporaryPropertiesFile);
            existingProperties.store(temporaryPropertiesFileWriter, null);
        }
        catch (IOException e)
        {
            Assert.fail("Could not write existing properties to temporary file!", e);
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
                    Assert.fail("Could not close temporary file writer!", e);
                }
            }
        }

        propertiesFileProfile = new PropertiesFile();
        propertiesFileProfile.setLocation(temporaryPropertiesFile.getAbsolutePath());
        createProfile(propertiesFileProfile);
    }

    protected abstract Properties createExistingProperties();

    protected abstract void createProfile(PropertiesFile profile);

    protected Properties deployProfile()
    {
        try
        {
            new PropertiesFileDeployer(propertiesFileProfile).deploy();
        }
        catch (DeployerException e)
        {
            Assert.fail("Could not deploy change to properties file!", e);
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
            Assert.fail("Could not load modified properties from temporary file!", e);
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
                    Assert.fail("Could not close temporary file reader!", e);
                }
            }
        }
        return modifiedProperties;
    }

    @AfterClass
    public void tearDown()
    {
        if (temporaryPropertiesFile != null && temporaryPropertiesFile.isFile())
        {
            temporaryPropertiesFile.delete();
        }
    }

}
