package com.configdeployer.deployer;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.configdeployer.binding.PropertiesAddEntry;
import com.configdeployer.binding.PropertiesFile;

public class PropertiesFileDeployerAddTest extends AbstractPropertiesFileDeployerTest
{

    @Override
    protected Properties createExistingProperties()
    {
        Properties existingProperties = new Properties();
        existingProperties.setProperty("existing.key", "original.existing.value");
        return existingProperties;
    }

    @Override
    protected void createProfile(PropertiesFile profile)
    {
        PropertiesAddEntry addEntry = new PropertiesAddEntry();
        addEntry.setKey("non.existing.key");
        addEntry.setValue("non.existing.value");
        profile.getAddOrUpdateOrSet().add(addEntry);
        addEntry = new PropertiesAddEntry();
        addEntry.setKey("existing.key");
        addEntry.setValue("new.existing.value");
        profile.getAddOrUpdateOrSet().add(addEntry);
    }

    @Test(groups = { "properties", "deployer" }, description = "Adding properties file entries: insert non-existing entry and preserve value of existing entry")
    public void testAddPropertiesEntry()
    {
        Properties modifiedProperties = deployProfile();
        Assert.assertEquals(modifiedProperties.getProperty("non.existing.key"), "non.existing.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.key"), "original.existing.value");
    }

}
