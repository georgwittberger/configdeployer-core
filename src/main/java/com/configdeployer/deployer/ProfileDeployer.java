package com.configdeployer.deployer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.ConfigProfile;
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
    private boolean failFast;

    public ProfileDeployer(ProfileProvider profileProvider, ProfilePreparer... profilePreparers)
    {
        this(profileProvider, profilePreparers != null ? Arrays.asList(profilePreparers) : null);
    }

    public ProfileDeployer(ProfileProvider profileProvider, List<? extends ProfilePreparer> profilePreparers)
    {
        this.profileProvider = profileProvider;
        if (profilePreparers != null)
        {
            this.profilePreparers = new ArrayList<ProfilePreparer>(profilePreparers.size());
            this.profilePreparers.addAll(profilePreparers);
        }
        else
        {
            this.profilePreparers = Collections.emptyList();
        }
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

    public boolean isFailFast()
    {
        return failFast;
    }

    public void setFailFast(boolean failFast)
    {
        this.failFast = failFast;
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
        for (Object target : profile.getPropertiesFileOrDatabase())
        {
            if (target instanceof PropertiesFile)
            {
                PropertiesFile propertiesFile = (PropertiesFile) target;
                try
                {
                    new PropertiesFileDeployer(propertiesFile).deploy();
                }
                catch (DeployerException e)
                {
                    success = false;
                    logger.error("Could not deploy change to properties file: " + propertiesFile.getLocation(), e);
                    if (isFailFast())
                    {
                        throw e;
                    }
                }
            }
            else if (target instanceof Database)
            {
                Database database = (Database) target;
                try
                {
                    new DatabaseDeployer(database).deploy();
                }
                catch (DeployerException e)
                {
                    success = false;
                    logger.error("Could not deploy change to database: " + database.getJdbcUrl(), e);
                    if (isFailFast())
                    {
                        throw e;
                    }
                }
            }
        }
        return success;
    }

    protected void restoreConfiguration(ConfigProfile profile) throws DeployerException
    {
    }

}
