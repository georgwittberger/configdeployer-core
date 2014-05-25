package com.configdeployer.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GZipInputStreamProvider implements InputStreamProvider
{

    private final InputStreamProvider compressedInputStreamProvider;

    public GZipInputStreamProvider(InputStreamProvider compressedInputStreamProvider)
    {
        this.compressedInputStreamProvider = compressedInputStreamProvider;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return new GZIPInputStream(compressedInputStreamProvider.getInputStream());
    }

}
