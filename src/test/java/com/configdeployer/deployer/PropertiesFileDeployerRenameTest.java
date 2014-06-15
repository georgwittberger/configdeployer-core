package com.configdeployer.deployer;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.binding.PropertiesRenameEntry;

public class PropertiesFileDeployerRenameTest extends AbstractPropertiesFileDeployerTest
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
        PropertiesRenameEntry renameEntry = new PropertiesRenameEntry();
        renameEntry.setKey("existing.key");
        renameEntry.setNewKey("new.existing.key");
        profile.getAddOrUpdateOrSet().add(renameEntry);
        renameEntry = new PropertiesRenameEntry();
        renameEntry.setKeyPattern("existing\\.(\\d+)");
        renameEntry.setNewKey("new.existing.$1");
        profile.getAddOrUpdateOrSet().add(renameEntry);
        renameEntry = new PropertiesRenameEntry();
        renameEntry.setKey("non.existing.key");
        renameEntry.setNewKey("new.non.existing.key");
        profile.getAddOrUpdateOrSet().add(renameEntry);
    }

    @Test(groups = { "properties", "deployer" }, description = "Renaming properties file entries: rename existing key preserving its value and ignore non-existing keys")
    public void testDeletePropertiesEntry()
    {
        Properties modifiedProperties = deployProfile();
        Assert.assertNull(modifiedProperties.getProperty("existing.key"));
        Assert.assertEquals(modifiedProperties.getProperty("new.existing.key"), "original.existing.value");
        Assert.assertNull(modifiedProperties.getProperty("existing.123"));
        Assert.assertNull(modifiedProperties.getProperty("existing.456"));
        Assert.assertEquals(modifiedProperties.getProperty("new.existing.123"), "original.123.value");
        Assert.assertEquals(modifiedProperties.getProperty("new.existing.456"), "original.456.value");
        Assert.assertNull(modifiedProperties.getProperty("new.non.existing.key"));
        Assert.assertEquals(modifiedProperties.getProperty("existing.remain"), "original.remain.value");
    }

}
