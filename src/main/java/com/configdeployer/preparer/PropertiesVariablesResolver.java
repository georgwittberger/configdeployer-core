package com.configdeployer.preparer;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class PropertiesVariablesResolver implements VariablesResolver
{

    private final PropertiesConfiguration properties;

    public PropertiesVariablesResolver(File propertiesFile) throws PreparerException
    {
        try
        {
            properties = new PropertiesConfiguration(propertiesFile);
        }
        catch (ConfigurationException e)
        {
            throw new PreparerException("Could not load properties file: " + propertiesFile.getAbsolutePath(), e);
        }
    }

    public PropertiesVariablesResolver(Map<?, ?> propertiesMap)
    {
        properties = new PropertiesConfiguration();
        for (Entry<?, ?> propertiesEntry : propertiesMap.entrySet())
        {
            properties.setProperty(String.valueOf(propertiesEntry.getKey()), propertiesEntry.getValue());
        }
    }

    @Override
    public String resolveVariable(String variableName)
    {
        return properties.getString(variableName);
    }

}
