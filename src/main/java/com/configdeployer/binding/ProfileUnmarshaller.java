package com.configdeployer.binding;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileUnmarshaller
{

    private static final String CONFIG_PROFILE_JAXB_PACKAGE = "com.configdeployer.binding";
    private static final Logger logger = LoggerFactory.getLogger(ProfileUnmarshaller.class);
    private static final ProfileUnmarshaller instance = new ProfileUnmarshaller();
    private final JAXBContext profileJaxbContext;

    private ProfileUnmarshaller()
    {
        JAXBContext profileJaxbContext;
        try
        {
            profileJaxbContext = JAXBContext.newInstance(CONFIG_PROFILE_JAXB_PACKAGE);
        }
        catch (JAXBException e)
        {
            profileJaxbContext = null;
            logger.error("Could not initialize JAXB context: " + CONFIG_PROFILE_JAXB_PACKAGE, e);
        }
        this.profileJaxbContext = profileJaxbContext;
    }

    public static ProfileUnmarshaller getInstance()
    {
        return instance;
    }

    public ConfigProfile unmarshal(Reader configProfileReader) throws JAXBException
    {
        Unmarshaller profileUnmarshaller = profileJaxbContext.createUnmarshaller();
        return (ConfigProfile) profileUnmarshaller.unmarshal(configProfileReader);
    }

    public ConfigProfile unmarshal(InputStream configProfileInputStream) throws JAXBException
    {
        Unmarshaller profileUnmarshaller = profileJaxbContext.createUnmarshaller();
        return (ConfigProfile) profileUnmarshaller.unmarshal(configProfileInputStream);
    }

}
