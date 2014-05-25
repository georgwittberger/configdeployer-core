package com.configdeployer.deployer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.ConfigProfile;
import com.configdeployer.binding.ConfigProfile.Databases;
import com.configdeployer.binding.ConfigProfile.PropertiesFiles;
import com.configdeployer.binding.Database;
import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.preparer.PreparerException;
import com.configdeployer.preparer.ProfilePreparer;
import com.configdeployer.provider.ProfileProvider;
import com.configdeployer.provider.ProviderException;

public class ProfileDeployer
{

    private static final Logger logger = LoggerFactory.getLogger(ProfileDeployer.class);
    private final ProfileProvider profileProvider;
    private final List<ProfilePreparer> profilePreparers;

    public ProfileDeployer(ProfileProvider profileProvider, ProfilePreparer... profilePreparers)
    {
        this.profileProvider = profileProvider;
        List<ProfilePreparer> preparers;
        if (profilePreparers != null)
        {
            preparers = Arrays.asList(profilePreparers);
        }
        else
        {
            preparers = Collections.emptyList();
        }
        this.profilePreparers = preparers;
    }

    public boolean deploy() throws DeployerException
    {
        ConfigProfile profile = loadProfile();
        prepareProfile(profile);
        backupConfiguration(profile);
        boolean success = true;
        try
        {
            success = deployProfile(profile);
        }
        catch (Exception e)
        {
            success = false;
            restoreConfiguration(profile);
        }
        if (success)
        {
            logger.info("Profile '{}' successfully deployed.", profile.getName());
        }
        else
        {
            logger.error("Profile '{}' could not be deployed successfully!", profile.getName());
        }
        return success;
    }

    protected ConfigProfile loadProfile() throws DeployerException
    {
        try
        {
            ConfigProfile profile = profileProvider.getConfigProfile();
            logger.info("Profile '{}' successfully loaded.", profile.getName());
            return profile;
        }
        catch (ProviderException e)
        {
            throw new DeployerException("Could not load profile!", e);
        }
    }

    protected void prepareProfile(ConfigProfile profile) throws DeployerException
    {
        for (ProfilePreparer preparer : profilePreparers)
        {
            try
            {
                preparer.prepareProfile(profile);
            }
            catch (PreparerException e)
            {
                throw new DeployerException("Could not prepare profile!", e);
            }
        }
        logger.info("Profile '{}' successfully prepared.", profile.getName());
    }

    protected void backupConfiguration(ConfigProfile profile) throws DeployerException
    {
    }

    protected boolean deployProfile(ConfigProfile profile) throws DeployerException
    {
        boolean success = true;
        PropertiesFiles propertiesFiles = profile.getPropertiesFiles();
        if (propertiesFiles != null)
        {
            for (PropertiesFile propertiesFile : propertiesFiles.getPropertiesFile())
            {
                try
                {
                    new PropertiesFileDeployer(propertiesFile).deploy();
                }
                catch (DeployerException e)
                {
                    success = false;
                    logger.error("Could not deploy properties file configuration: " + propertiesFile.getLocation(), e);
                }
            }
        }
        Databases databases = profile.getDatabases();
        if (databases != null)
        {
            for (Database database : databases.getDatabase())
            {
                try
                {
                    new DatabaseDeployer(database).deploy();
                }
                catch (DeployerException e)
                {
                    success = false;
                    logger.error("Could not deploy database configuration: " + database.getJdbcUrl(), e);
                }
            }
        }
        return success;
    }

    protected void restoreConfiguration(ConfigProfile profile) throws DeployerException
    {
    }

}
