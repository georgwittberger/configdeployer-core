package com.configdeployer.deployer;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.binding.PropertiesSetEntry;

public class PropertiesFileDeployerSetTest extends AbstractPropertiesFileDeployerTest
{

    @Override
    protected Properties createExistingProperties()
    {
        Properties existingProperties = new Properties();
        existingProperties.setProperty("existing.key", "original.existing.value");
        existingProperties.setProperty("existing.other", "original.other.value");
        return existingProperties;
    }

    @Override
    protected void createProfile(PropertiesFile profile)
    {
        PropertiesSetEntry setEntry = new PropertiesSetEntry();
        setEntry.setKey("existing.key");
        setEntry.setValue("new.existing.value");
        profile.getAddOrUpdateOrSet().add(setEntry);
        setEntry = new PropertiesSetEntry();
        setEntry.setKey("non.existing.key");
        setEntry.setValue("non.existing.value");
        profile.getAddOrUpdateOrSet().add(setEntry);
    }

    @Test(groups = { "properties", "deployer" }, description = "Setting properties file entries: update value of existing entry and insert non-existing entry")
    public void testSetPropertiesEntry()
    {
        Properties modifiedProperties = deployProfile();
        Assert.assertEquals(modifiedProperties.getProperty("existing.key"), "new.existing.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.other"), "original.other.value");
        Assert.assertEquals(modifiedProperties.getProperty("non.existing.key"), "non.existing.value");
    }

}
