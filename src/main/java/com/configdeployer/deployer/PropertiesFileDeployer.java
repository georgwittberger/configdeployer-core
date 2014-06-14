package com.configdeployer.deployer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.PropertiesAddEntry;
import com.configdeployer.binding.PropertiesDeleteEntry;
import com.configdeployer.binding.PropertiesEntryCondition;
import com.configdeployer.binding.PropertiesFile;
import com.configdeployer.binding.PropertiesFileOperation;
import com.configdeployer.binding.PropertiesRenameEntry;
import com.configdeployer.binding.PropertiesSetEntry;
import com.configdeployer.binding.PropertiesUpdateEntry;

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
        for (Object entry : propertiesFileProfile.getAddOrUpdateOrSet())
        {
            if (entry instanceof PropertiesAddEntry)
            {
                applyEntry((PropertiesAddEntry) entry, propertiesConfiguration);
            }
            else if (entry instanceof PropertiesUpdateEntry)
            {
                PropertiesUpdateEntry updateEntry = (PropertiesUpdateEntry) entry;
                if (updateEntry.getKey() != null && updateEntry.getKeyPattern() == null)
                {
                    if (!checkEntryConditions(createEntryConditions(updateEntry), propertiesConfiguration))
                    {
                        continue;
                    }
                    applyEntry(updateEntry, propertiesConfiguration);
                }
                else if (updateEntry.getKeyPattern() != null)
                {
                    for (PropertiesUpdateEntry virtualEntry : createVirtualEntries(updateEntry, propertiesConfiguration))
                    {
                        if (!checkEntryConditions(createEntryConditions(virtualEntry), propertiesConfiguration))
                        {
                            continue;
                        }
                        applyEntry(virtualEntry, propertiesConfiguration);
                    }
                }
                else
                {
                    logger.error("Update entry for properties file '{}' has neither a key nor a key-pattern.",
                            propertiesFile.getAbsolutePath());
                }
            }
            else if (entry instanceof PropertiesSetEntry)
            {
                PropertiesSetEntry setEntry = (PropertiesSetEntry) entry;
                if (!checkEntryConditions(createEntryConditions(setEntry), propertiesConfiguration))
                {
                    continue;
                }
                applyEntry(setEntry, propertiesConfiguration);
            }
            else if (entry instanceof PropertiesDeleteEntry)
            {
                PropertiesDeleteEntry deleteEntry = (PropertiesDeleteEntry) entry;
                if (deleteEntry.getKey() != null && deleteEntry.getKeyPattern() == null)
                {
                    if (!checkEntryConditions(createEntryConditions(deleteEntry), propertiesConfiguration))
                    {
                        continue;
                    }
                    applyEntry(deleteEntry, propertiesConfiguration);
                }
                else if (deleteEntry.getKeyPattern() != null)
                {
                    for (PropertiesDeleteEntry virtualEntry : createVirtualEntries(deleteEntry, propertiesConfiguration))
                    {
                        if (!checkEntryConditions(createEntryConditions(virtualEntry), propertiesConfiguration))
                        {
                            continue;
                        }
                        applyEntry(virtualEntry, propertiesConfiguration);
                    }
                }
                else
                {
                    logger.error("Delete entry for properties file '{}' has neither a key nor a key-pattern.",
                            propertiesFile.getAbsolutePath());
                }
            }
            else if (entry instanceof PropertiesRenameEntry)
            {
                PropertiesRenameEntry renameEntry = (PropertiesRenameEntry) entry;
                if (renameEntry.getKey() != null && renameEntry.getKeyPattern() == null)
                {
                    if (!checkEntryConditions(createEntryConditions(renameEntry), propertiesConfiguration))
                    {
                        continue;
                    }
                    applyEntry(renameEntry, propertiesConfiguration);
                }
                else if (renameEntry.getKeyPattern() != null)
                {
                    for (PropertiesRenameEntry virtualEntry : createVirtualEntries(renameEntry, propertiesConfiguration))
                    {
                        if (!checkEntryConditions(createEntryConditions(virtualEntry), propertiesConfiguration))
                        {
                            continue;
                        }
                        applyEntry(virtualEntry, propertiesConfiguration);
                    }
                }
                else
                {
                    logger.error("Rename entry for properties file '{}' has neither a key nor a key-pattern.",
                            propertiesFile.getAbsolutePath());
                }
            }
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

    private List<PropertiesUpdateEntry> createVirtualEntries(PropertiesUpdateEntry entry,
            PropertiesConfiguration propertiesConfiguration)
    {
        Pattern keyPattern = Pattern.compile(entry.getKeyPattern());
        List<PropertiesUpdateEntry> virtualEntries = new ArrayList<PropertiesUpdateEntry>();
        if (entry.getKey() != null)
        {
            PropertiesUpdateEntry virtualEntry = cloneEntry(entry);
            virtualEntry.setKeyPattern(null);
            virtualEntries.add(virtualEntry);
        }
        for (Iterator<String> keysIterator = propertiesConfiguration.getKeys(); keysIterator.hasNext();)
        {
            String propertiesKey = keysIterator.next();
            Matcher keyMatcher = keyPattern.matcher(propertiesKey);
            if (!keyMatcher.matches())
            {
                continue;
            }
            PropertiesUpdateEntry virtualEntry = cloneEntry(entry);
            virtualEntry.setKey(propertiesKey);
            virtualEntry.setKeyPattern(null);
            virtualEntries.add(virtualEntry);
        }
        return virtualEntries;
    }

    private PropertiesUpdateEntry cloneEntry(PropertiesUpdateEntry originalEntry)
    {
        PropertiesUpdateEntry clonedEntry = new PropertiesUpdateEntry();
        clonedEntry.setKey(originalEntry.getKey());
        clonedEntry.setKeyPattern(originalEntry.getKeyPattern());
        clonedEntry.setValue(originalEntry.getValue());
        clonedEntry.setComment(originalEntry.getComment());
        for (PropertiesEntryCondition condition : originalEntry.getCondition())
        {
            clonedEntry.getCondition().add(condition);
        }
        return clonedEntry;
    }

    private List<PropertiesDeleteEntry> createVirtualEntries(PropertiesDeleteEntry entry,
            PropertiesConfiguration propertiesConfiguration)
    {
        Pattern keyPattern = Pattern.compile(entry.getKeyPattern());
        List<PropertiesDeleteEntry> virtualEntries = new ArrayList<PropertiesDeleteEntry>();
        if (entry.getKey() != null)
        {
            PropertiesDeleteEntry virtualEntry = cloneEntry(entry);
            virtualEntry.setKeyPattern(null);
            virtualEntries.add(virtualEntry);
        }
        for (Iterator<String> keysIterator = propertiesConfiguration.getKeys(); keysIterator.hasNext();)
        {
            String propertiesKey = keysIterator.next();
            Matcher keyMatcher = keyPattern.matcher(propertiesKey);
            if (!keyMatcher.matches())
            {
                continue;
            }
            PropertiesDeleteEntry virtualEntry = cloneEntry(entry);
            virtualEntry.setKey(propertiesKey);
            virtualEntry.setKeyPattern(null);
            virtualEntries.add(virtualEntry);
        }
        return virtualEntries;
    }

    private PropertiesDeleteEntry cloneEntry(PropertiesDeleteEntry originalEntry)
    {
        PropertiesDeleteEntry clonedEntry = new PropertiesDeleteEntry();
        clonedEntry.setKey(originalEntry.getKey());
        clonedEntry.setKeyPattern(originalEntry.getKeyPattern());
        for (PropertiesEntryCondition condition : originalEntry.getCondition())
        {
            clonedEntry.getCondition().add(condition);
        }
        return clonedEntry;
    }

    private List<PropertiesRenameEntry> createVirtualEntries(PropertiesRenameEntry entry,
            PropertiesConfiguration propertiesConfiguration)
    {
        Pattern keyPattern = Pattern.compile(entry.getKeyPattern());
        List<PropertiesRenameEntry> virtualEntries = new ArrayList<PropertiesRenameEntry>();
        if (entry.getKey() != null)
        {
            PropertiesRenameEntry virtualEntry = cloneEntry(entry);
            virtualEntry.setKeyPattern(null);
            virtualEntries.add(virtualEntry);
        }
        for (Iterator<String> keysIterator = propertiesConfiguration.getKeys(); keysIterator.hasNext();)
        {
            String propertiesKey = keysIterator.next();
            Matcher keyMatcher = keyPattern.matcher(propertiesKey);
            if (!keyMatcher.matches())
            {
                continue;
            }
            PropertiesRenameEntry virtualEntry = cloneEntry(entry);
            virtualEntry.setKey(propertiesKey);
            virtualEntry.setKeyPattern(null);
            virtualEntry.setNewKey(keyMatcher.replaceAll(entry.getNewKey()));
            virtualEntries.add(virtualEntry);
        }
        return virtualEntries;
    }

    private PropertiesRenameEntry cloneEntry(PropertiesRenameEntry originalEntry)
    {
        PropertiesRenameEntry clonedEntry = new PropertiesRenameEntry();
        clonedEntry.setKey(originalEntry.getKey());
        clonedEntry.setKeyPattern(originalEntry.getKeyPattern());
        clonedEntry.setNewKey(originalEntry.getNewKey());
        clonedEntry.setComment(originalEntry.getComment());
        for (PropertiesEntryCondition condition : originalEntry.getCondition())
        {
            clonedEntry.getCondition().add(condition);
        }
        return clonedEntry;
    }

    private PropertiesEntryConditions createEntryConditions(PropertiesUpdateEntry updateEntry)
    {
        return new PropertiesEntryConditions("Update", updateEntry.getKey(), updateEntry.getCondition());
    }

    private PropertiesEntryConditions createEntryConditions(PropertiesSetEntry setEntry)
    {
        return new PropertiesEntryConditions("Set", setEntry.getKey(), setEntry.getCondition());
    }

    private PropertiesEntryConditions createEntryConditions(PropertiesDeleteEntry deleteEntry)
    {
        return new PropertiesEntryConditions("Delete", deleteEntry.getKey(), deleteEntry.getCondition());
    }

    private PropertiesEntryConditions createEntryConditions(PropertiesRenameEntry renameEntry)
    {
        return new PropertiesEntryConditions("Rename", renameEntry.getKey(), renameEntry.getCondition());
    }

    private boolean checkEntryConditions(PropertiesEntryConditions entry,
            PropertiesConfiguration propertiesConfiguration)
    {
        if (entry.getConditions().isEmpty())
        {
            return true;
        }
        String propertyValue = propertiesConfiguration.getString(entry.getKey());
        for (PropertiesEntryCondition condition : entry.getConditions())
        {
            if (condition.getValueEquals() != null && !StringUtils.equals(propertyValue, condition.getValueEquals()))
            {
                logger.info(
                        "{} action for property '{}' in file '{}' is skipped because current value does not equal '{}'.",
                        entry.getType(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueEquals());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueContains())
                    && !StringUtils.contains(propertyValue, condition.getValueContains()))
            {
                logger.info(
                        "{} action for property '{}' in file '{}' is skipped because current value does not contain '{}'.",
                        entry.getType(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueContains());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueMatches())
                    && (propertyValue == null || !propertyValue.matches(condition.getValueMatches())))
            {
                logger.info(
                        "{} action for property '{}' in file '{}' is skipped because current value does not match '{}'.",
                        entry.getType(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueMatches());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueNotEquals())
                    && StringUtils.equals(propertyValue, condition.getValueNotEquals()))
            {
                logger.info("{} action for property '{}' in file '{}' is skipped because current value equals '{}'.",
                        entry.getType(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueNotEquals());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueNotContains())
                    && StringUtils.contains(propertyValue, condition.getValueNotContains()))
            {
                logger.info("{} action for property '{}' in file '{}' is skipped because current value contains '{}'.",
                        entry.getType(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueNotContains());
                return false;
            }
            else if (StringUtils.isNotBlank(condition.getValueNotMatches()) && propertyValue != null
                    && propertyValue.matches(condition.getValueNotMatches()))
            {
                logger.info("{} action for property '{}' in file '{}' is skipped because current value matches '{}'.",
                        entry.getType(), entry.getKey(), propertiesConfiguration.getFile().getAbsolutePath(),
                        condition.getValueNotMatches());
                return false;
            }
        }
        return true;
    }

    private void applyEntry(PropertiesAddEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
        if (!propertiesConfiguration.containsKey(entry.getKey()))
        {
            propertiesConfiguration.setProperty(entry.getKey(), entry.getValue());
            if (entry.getComment() != null)
            {
                propertiesConfiguration.getLayout().setComment(entry.getKey(), entry.getComment());
            }
            logger.info("Added property '{}' with value '{}' to file '{}'.", entry.getKey(), entry.getValue(),
                    propertiesConfiguration.getFile().getAbsolutePath());
        }
        else
        {
            logger.info("Skipped adding property '{}' with value '{}' to file '{}' because it already exists.",
                    entry.getKey(), entry.getValue(), propertiesConfiguration.getFile().getAbsolutePath());
        }
    }

    private void applyEntry(PropertiesUpdateEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
        if (propertiesConfiguration.containsKey(entry.getKey()))
        {
            propertiesConfiguration.setProperty(entry.getKey(), entry.getValue());
            if (entry.getComment() != null)
            {
                propertiesConfiguration.getLayout().setComment(entry.getKey(), entry.getComment());
            }
            logger.info("Updated property '{}' with value '{}' in file '{}'.", entry.getKey(), entry.getValue(),
                    propertiesConfiguration.getFile().getAbsolutePath());
        }
        else
        {
            logger.info("Skipped updating property '{}' with value '{}' in file '{}' because it does not exist.",
                    entry.getKey(), entry.getValue(), propertiesConfiguration.getFile().getAbsolutePath());
        }
    }

    private void applyEntry(PropertiesSetEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
        propertiesConfiguration.setProperty(entry.getKey(), entry.getValue());
        if (entry.getComment() != null)
        {
            propertiesConfiguration.getLayout().setComment(entry.getKey(), entry.getComment());
        }
        logger.info("Set property '{}' with value '{}' in file '{}'.", entry.getKey(), entry.getValue(),
                propertiesConfiguration.getFile().getAbsolutePath());
    }

    private void applyEntry(PropertiesDeleteEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
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
    }

    private void applyEntry(PropertiesRenameEntry entry, PropertiesConfiguration propertiesConfiguration)
    {
        if (propertiesConfiguration.containsKey(entry.getKey()))
        {
            Object propertyValue = propertiesConfiguration.getProperty(entry.getKey());
            propertiesConfiguration.setProperty(entry.getNewKey(), propertyValue);
            PropertiesConfigurationLayout propertiesLayout = propertiesConfiguration.getLayout();
            propertiesLayout.setBlancLinesBefore(entry.getNewKey(),
                    propertiesLayout.getBlancLinesBefore(entry.getKey()));
            propertiesLayout.setComment(entry.getNewKey(), propertiesLayout.getComment(entry.getKey()));
            propertiesLayout.setSeparator(entry.getNewKey(), propertiesLayout.getSeparator(entry.getKey()));
            propertiesLayout.setSingleLine(entry.getNewKey(), propertiesLayout.isSingleLine(entry.getKey()));
            propertiesConfiguration.clearProperty(entry.getKey());
            if (entry.getComment() != null)
            {
                propertiesConfiguration.getLayout().setComment(entry.getNewKey(), entry.getComment());
            }
            logger.info("Renamed property '{}' to '{}' in file '{}'.", entry.getKey(), entry.getNewKey(),
                    propertiesConfiguration.getFile().getAbsolutePath());
        }
        else
        {
            logger.info("Skipped renaming property '{}' to '{}' in file '{}' because it does not exist.",
                    entry.getKey(), entry.getNewKey(), propertiesConfiguration.getFile().getAbsolutePath());
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

    private static class PropertiesEntryConditions
    {

        private final String type;
        private final String key;
        private final List<PropertiesEntryCondition> conditions;

        public PropertiesEntryConditions(String type, String key, List<PropertiesEntryCondition> conditions)
        {
            this.type = type;
            this.key = key;
            this.conditions = conditions;
        }

        public String getType()
        {
            return type;
        }

        public String getKey()
        {
            return key;
        }

        public List<PropertiesEntryCondition> getConditions()
        {
            return conditions;
        }

    }

}
