package com.configdeployer.deployer;

public class DeployerException extends Exception
{

    private static final long serialVersionUID = 1L;

    public DeployerException()
    {
    }

    public DeployerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DeployerException(String message)
    {
        super(message);
    }

    public DeployerException(Throwable cause)
    {
        super(cause);
    }

}
