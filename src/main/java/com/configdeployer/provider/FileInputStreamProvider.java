package com.configdeployer.provider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileInputStreamProvider implements InputStreamProvider
{

    private final File file;

    public FileInputStreamProvider(File file)
    {
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return new BufferedInputStream(new FileInputStream(file));
    }

}
