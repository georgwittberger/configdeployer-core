package com.configdeployer.preparer;

public class PreparerException extends Exception
{

    private static final long serialVersionUID = 1L;

    public PreparerException()
    {
    }

    public PreparerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public PreparerException(String message)
    {
        super(message);
    }

    public PreparerException(Throwable cause)
    {
        super(cause);
    }

}
