package com.configdeployer.preparer;

public class SystemPropertiesResolver implements VariablesResolver
{

    @Override
    public String resolveVariable(String variableName)
    {
        return System.getProperty(variableName);
    }

}
