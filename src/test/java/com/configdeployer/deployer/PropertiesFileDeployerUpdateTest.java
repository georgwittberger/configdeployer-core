package com.configdeployer.deployer;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.binding.PropertiesUpdateEntry;

public class PropertiesFileDeployerUpdateTest extends AbstractPropertiesFileDeployerTest
{

    @Override
    protected Properties createExistingProperties()
    {
        Properties existingProperties = new Properties();
        existingProperties.setProperty("existing.key", "original.existing.value");
        existingProperties.setProperty("existing.123", "original.123.value");
        existingProperties.setProperty("existing.456", "original.456.value");
        return existingProperties;
    }

    @Override
    protected void createProfile(PropertiesFile profile)
    {
        PropertiesUpdateEntry updateEntry = new PropertiesUpdateEntry();
        updateEntry.setKey("existing.key");
        updateEntry.setValue("new.existing.value");
        profile.getAddOrUpdateOrSet().add(updateEntry);
        updateEntry = new PropertiesUpdateEntry();
        updateEntry.setKeyPattern("existing\\.\\d+");
        updateEntry.setValue("new.regex.value");
        profile.getAddOrUpdateOrSet().add(updateEntry);
        updateEntry = new PropertiesUpdateEntry();
        updateEntry.setKey("non.existing.key");
        updateEntry.setValue("non.existing.value");
        profile.getAddOrUpdateOrSet().add(updateEntry);
    }

    @Test(groups = { "properties", "deployer" }, description = "Updating properties file entries: update value of existing entry and ignore non-existing entry")
    public void testUpdatePropertiesEntry()
    {
        Properties modifiedProperties = deployProfile();
        Assert.assertEquals(modifiedProperties.getProperty("existing.key"), "new.existing.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.123"), "new.regex.value");
        Assert.assertEquals(modifiedProperties.getProperty("existing.456"), "new.regex.value");
        Assert.assertNull(modifiedProperties.getProperty("non.existing.key"));
    }

}
