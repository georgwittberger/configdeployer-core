package com.configdeployer.provider;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.ConfigProfile;
import com.configdeployer.binding.ProfileUnmarshaller;

public class ProfileProvider
{

    private static final Logger logger = LoggerFactory.getLogger(ProfileProvider.class);
    private final InputStreamProvider inputStreamProvider;

    public ProfileProvider(InputStreamProvider inputStreamProvider)
    {
        this.inputStreamProvider = inputStreamProvider;
    }

    public ConfigProfile getConfigProfile() throws ProviderException
    {
        InputStream profileInputStream = null;
        try
        {
            profileInputStream = inputStreamProvider.getInputStream();
            return ProfileUnmarshaller.getInstance().unmarshal(profileInputStream);
        }
        catch (IOException e)
        {
            throw new ProviderException("Could not load profile!", e);
        }
        catch (JAXBException e)
        {
            throw new ProviderException("Could not unmarshal profile!", e);
        }
        finally
        {
            if (profileInputStream != null)
            {
                try
                {
                    profileInputStream.close();
                }
                catch (IOException e)
                {
                    logger.error("Could not close profile input stream!", e);
                }
            }
        }
    }

}
