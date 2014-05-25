package com.configdeployer.provider;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamProvider
{

    InputStream getInputStream() throws IOException;

}
