package com.configdeployer.preparer;

public class EnvironmentVariablesResolver implements VariablesResolver
{

    @Override
    public String resolveVariable(String variableName)
    {
        return System.getenv(variableName);
    }

}
