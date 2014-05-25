package com.configdeployer.preparer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.ConfigProfile;
import com.configdeployer.binding.ConfigProfile.Databases;
import com.configdeployer.binding.ConfigProfile.PropertiesFiles;
import com.configdeployer.binding.Database;
import com.configdeployer.binding.PropertiesFile;

public class VariablesPreparer implements ProfilePreparer
{

    private static final Logger logger = LoggerFactory.getLogger(VariablesPreparer.class);
    private final StrSubstitutor variablesSubstitutor = new StrSubstitutor(new VariablesLookup());
    private final Map<String, VariablesResolver> variablesResolvers = new HashMap<String, VariablesResolver>();
    private int depth = 1;

    @Override
    public void prepareProfile(ConfigProfile profile) throws PreparerException
    {
        profile.setName(replaceVariables(profile.getName()));
        PropertiesFiles propertiesFiles = profile.getPropertiesFiles();
        if (propertiesFiles != null)
        {
            for (PropertiesFile propertiesFile : propertiesFiles.getPropertiesFile())
            {
                propertiesFile.setLocation(replaceVariables(propertiesFile.getLocation()));
            }
        }
        Databases databases = profile.getDatabases();
        if (databases != null)
        {
            for (Database database : databases.getDatabase())
            {
                database.setDriverClass(replaceVariables(database.getDriverClass()));
                database.setJdbcUrl(replaceVariables(database.getJdbcUrl()));
                database.setUsername(replaceVariables(database.getUsername()));
                database.setPassword(replaceVariables(database.getPassword()));
            }
        }
    }

    public void addResolver(String prefix, VariablesResolver resolver)
    {
        variablesResolvers.put(prefix != null ? prefix : "", resolver);
    }

    public void removeResolver(String prefix)
    {
        variablesResolvers.remove(prefix != null ? prefix : "");
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    private String replaceVariables(String inputString)
    {
        String outputString = inputString;
        for (int i = 0; i < depth; i++)
        {
            outputString = variablesSubstitutor.replace(outputString);
        }
        return outputString;
    }

    private class VariablesLookup extends StrLookup<String>
    {

        @Override
        public String lookup(String key)
        {
            int prefixDelimiterIndex = key.indexOf(':');
            String prefix = prefixDelimiterIndex > 0 ? key.substring(0, prefixDelimiterIndex) : "";
            String variableName = key.substring(prefixDelimiterIndex + 1);
            VariablesResolver resolver = variablesResolvers.get(prefix);
            if (resolver == null)
            {
                logger.error("Cannot resolve variable '{}'. No resolver for prefix '{}'!", key, prefix);
                return null;
            }
            return resolver.resolveVariable(variableName);
        }

    }

}
