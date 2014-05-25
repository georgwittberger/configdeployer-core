package com.configdeployer.deployer;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.PropertiesEntry;
import com.configdeployer.binding.PropertiesEntryCondition;
import com.configdeployer.binding.PropertiesEntryOperation;
import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.binding.PropertiesFileOperation;

public class PropertiesFileDeployer
{

    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileDeployer.class);
    private final PropertiesFile propertiesFileProfile;

    public PropertiesFileDeployer(PropertiesFile propertiesFileProfile)
    {
        this.propertiesFileProfile = propertiesFileProfile;
    }

    public void deploy() throws DeployerException
    {
        File propertiesFile = getPropertiesFile();
        if (propertiesFileProfile.getOperation() == PropertiesFileOperation.DELETE)
        {
            deletePropertiesFile(propertiesFile);
            return;
        }
        PropertiesConfiguration propertiesConfiguration = loadPropertiesConfiguration(propertiesFile);
        for (PropertiesEntry entry : propertiesFileProfile.getEntry())
        {
            if (!checkEntryConditions(entry, propertiesConfiguration))
            {
                continue;
            }
            applyEntryConfiguration(entry, propertiesConfiguration);
        }
        savePropertiesConfiguration(propertiesConfiguration);
    }

    private File getPropertiesFile() throws DeployerException
    {
        return new File(propertiesFileProfile.getLocation());
    }

    private PropertiesConfiguration loadPropertiesConfiguration(File propertiesFile) throws DeployerException
    {
        try
        {
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(propertiesFile);
            ConfigurationInterpolator variableInterpolator = propertiesConfiguration.getInterpolator();
            variableInterpolator.deregisterLookup(ConfigurationInterpolator.PREFIX_CONSTANTS);
            variableInterpolator.deregisterLookup(ConfigurationInterpolator.PREFIX_ENVIRONMENT);
            variableInterpolator.deregisterLookup(ConfigurationInterpolator.PREFIX_SYSPROPERTIES);
            variableInterpolator.setDefaultLookup(null);
            return propertiesConfiguration;
        }
        catch (ConfigurationException e)
        {
            throw new DeployerException("Could not load properties file: " + propertiesFile.getAbsolutePath(), e);
        }
    }

    private void deletePropertiesFile(File propertiesFile) throws DeployerException
    {
        if (propertiesFile.isFile())
        {
            if (propertiesFile.delete())
            {
                logger.info("Properties file '{}' successfully deleted.", propertiesFile.getAbsolutePath());
            }
            else
            {
                throw new DeployerException("Could not delete properties file: " + propertiesFile.getAbsolutePath());
            }
        }
        else
        {
            logger.info("Skipped deleting properties file '{}' because it does not exist.",
                    propertiesFile.getAbsolutePath());
        }
    }

    private boolean checkEntryConditions(PropertiesEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
        if (entry.getCondition().isEmpty())
        {
            return true;
        }
        String propertyValue = propertiesConfiguration.getString(entry.getKey());
        for (PropertiesEntryCondition condition : entry.getCondition())
        {
            if (condition.getValueEquals() != null && !StringUtils.equals(propertyValue, condition.getValueEquals()))
            {
                logger.info(
                        "{} action for property '{}' in file '{}' is skipped because current value does not equal '{}'.",
                        entry.getOperation(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueEquals());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueContains())
                    && !StringUtils.contains(propertyValue, condition.getValueContains()))
            {
                logger.info(
                        "{} action for property '{}' in file '{}' is skipped because current value does not contain '{}'.",
                        entry.getOperation(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueContains());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueMatches())
                    && (propertyValue == null || !propertyValue.matches(condition.getValueMatches())))
            {
                logger.info(
                        "{} action for property '{}' in file '{}' is skipped because current value does not match '{}'.",
                        entry.getOperation(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueMatches());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueNotEquals())
                    && StringUtils.equals(propertyValue, condition.getValueNotEquals()))
            {
                logger.info("{} action for property '{}' in file '{}' is skipped because current value equals '{}'.",
                        entry.getOperation(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueNotEquals());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueNotContains())
                    && StringUtils.contains(propertyValue, condition.getValueNotContains()))
            {
                logger.info("{} action for property '{}' in file '{}' is skipped because current value contains '{}'.",
                        entry.getOperation(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueNotContains());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueNotMatches()) && propertyValue != null
                    && propertyValue.matches(condition.getValueNotMatches()))
            {
                logger.info("{} action for property '{}' in file '{}' is skipped because current value matches '{}'.",
                        entry.getOperation(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueNotMatches());
                return false;
            }
        }
        return true;
    }

    private void applyEntryConfiguration(PropertiesEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
        switch (entry.getOperation())
        {
        case ADD:
            if (!propertiesConfiguration.containsKey(entry.getKey()))
            {
                propertiesConfiguration.setProperty(entry.getKey(), entry.getValue());
                logger.info("Added property '{}' with value '{}' to file '{}'.", entry.getKey(), entry.getValue(),
                        propertiesConfiguration.getFile().getAbsolutePath());
            }
            else
            {
                logger.info("Skipped adding property '{}' with value '{}' to file '{}' because it already exists.",
                        entry.getKey(), entry.getValue(), propertiesConfiguration.getFile().getAbsolutePath());
            }
            break;
        case UPDATE:
            if (propertiesConfiguration.containsKey(entry.getKey()))
            {
                propertiesConfiguration.setProperty(entry.getKey(), entry.getValue());
                logger.info("Updated property '{}' with value '{}' in file '{}'.", entry.getKey(), entry.getValue(),
                        propertiesConfiguration.getFile().getAbsolutePath());
            }
            else
            {
                logger.info("Skipped updating property '{}' with value '{}' in file '{}' because it does not exist.",
                        entry.getKey(), entry.getValue(), propertiesConfiguration.getFile().getAbsolutePath());
            }
            break;
        case SET:
            propertiesConfiguration.setProperty(entry.getKey(), entry.getValue());
            logger.info("Set property '{}' with value '{}' in file '{}'.", entry.getKey(), entry.getValue(),
                    propertiesConfiguration.getFile().getAbsolutePath());
            break;
        case DELETE:
            if (propertiesConfiguration.containsKey(entry.getKey()))
            {
                propertiesConfiguration.clearProperty(entry.getKey());
                logger.info("Deleted property '{}' from file '{}'.", entry.getKey(), propertiesConfiguration.getFile()
                        .getAbsolutePath());
            }
            else
            {
                logger.info("Skipped deleting property '{}' in file '{}' because it does not exist.", entry.getKey(),
                        propertiesConfiguration.getFile().getAbsolutePath());
            }
            break;
        case RENAME:
            if (propertiesConfiguration.containsKey(entry.getKey()))
            {
                Object propertyValue = propertiesConfiguration.getProperty(entry.getKey());
                propertiesConfiguration.setProperty(entry.getValue(), propertyValue);
                PropertiesConfigurationLayout propertiesLayout = propertiesConfiguration.getLayout();
                propertiesLayout.setBlancLinesBefore(entry.getValue(),
                        propertiesLayout.getBlancLinesBefore(entry.getKey()));
                propertiesLayout.setComment(entry.getValue(), propertiesLayout.getComment(entry.getKey()));
                propertiesLayout.setSeparator(entry.getValue(), propertiesLayout.getSeparator(entry.getKey()));
                propertiesLayout.setSingleLine(entry.getValue(), propertiesLayout.isSingleLine(entry.getKey()));
                propertiesConfiguration.clearProperty(entry.getKey());
                logger.info("Renamed property '{}' to '{}' in file '{}'.", entry.getKey(), entry.getValue(),
                        propertiesConfiguration.getFile().getAbsolutePath());
            }
            else
            {
                logger.info("Skipped renaming property '{}' to '{}' in file '{}' because it does not exist.",
                        entry.getKey(), entry.getValue(), propertiesConfiguration.getFile().getAbsolutePath());
            }
            break;
        }
        if (entry.getOperation() != PropertiesEntryOperation.DELETE && entry.getComment() != null)
        {
            propertiesConfiguration.getLayout().setComment(entry.getKey(), entry.getComment());
        }
    }

    private void savePropertiesConfiguration(PropertiesConfiguration propertiesConfiguration) throws DeployerException
    {
        try
        {
            propertiesConfiguration.save();
            logger.info("Properties file '{}' successfully saved.", propertiesConfiguration.getFile().getAbsolutePath());
        }
        catch (ConfigurationException e)
        {
            throw new DeployerException("Could not save properties file: "
                    + propertiesConfiguration.getFile().getAbsolutePath(), e);
        }
    }

}
