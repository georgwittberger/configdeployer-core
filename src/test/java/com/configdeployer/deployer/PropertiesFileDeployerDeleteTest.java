package com.configdeployer.deployer;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.configdeployer.binding.PropertiesDeleteEntry;
import com.configdeployer.binding.PropertiesFile;

public class PropertiesFileDeployerDeleteTest extends AbstractPropertiesFileDeployerTest
{

    @Override
    protected Properties createExistingProperties()
    {
        Properties existingProperties = new Properties();
        existingProperties.setProperty("existing.key", "original.existing.value");
        existingProperties.setProperty("existing.123", "original.123.value");
        existingProperties.setProperty("existing.456", "original.456.value");
        existingProperties.setProperty("existing.remain", "original.remain.value");
        return existingProperties;
    }

    @Override
    protected void createProfile(PropertiesFile profile)
    {
        PropertiesDeleteEntry deleteEntry = new PropertiesDeleteEntry();
        deleteEntry.setKey("existing.key");
        profile.getAddOrUpdateOrSet().add(deleteEntry);
        deleteEntry = new PropertiesDeleteEntry();
        deleteEntry.setKeyPattern("existing\\.\\d+");
        profile.getAddOrUpdateOrSet().add(deleteEntry);
        deleteEntry = new PropertiesDeleteEntry();
        deleteEntry.setKey("non.existing.key");
        profile.getAddOrUpdateOrSet().add(deleteEntry);
    }

    @Test(groups = { "properties", "deployer" }, description = "Deleting properties file entries: delete existing entry and ignore non-existing entry")
    public void testDeletePropertiesEntry()
    {
        Properties modifiedProperties = deployProfile();
        Assert.assertNull(modifiedProperties.getProperty("existing.key"));
        Assert.assertNull(modifiedProperties.getProperty("existing.123"));
        Assert.assertNull(modifiedProperties.getProperty("existing.456"));
        Assert.assertEquals(modifiedProperties.getProperty("existing.remain"), "original.remain.value");
    }

}
