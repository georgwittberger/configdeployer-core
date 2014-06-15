package com.configdeployer.binding;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileMarshaller
{

    private static final String CONFIG_PROFILE_JAXB_PACKAGE = "com.configdeployer.binding";
    private static final Logger logger = LoggerFactory.getLogger(ProfileMarshaller.class);
    private static final ProfileMarshaller instance = new ProfileMarshaller();
    private final JAXBContext profileJaxbContext;

    private ProfileMarshaller()
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

    public static ProfileMarshaller getInstance()
    {
        return instance;
    }

    public void marshal(ConfigProfile profile, Writer configProfileWriter) throws JAXBException
    {
        Marshaller profileMarshaller = profileJaxbContext.createMarshaller();
        profileMarshaller.marshal(profile, configProfileWriter);
    }

    public void marshal(ConfigProfile profile, OutputStream configProfileOutputStream) throws JAXBException
    {
        Marshaller profileMarshaller = profileJaxbContext.createMarshaller();
        profileMarshaller.marshal(profile, configProfileOutputStream);
    }

    public void marshal(ConfigProfile profile, File configProfileFile) throws JAXBException
    {
        Marshaller profileMarshaller = profileJaxbContext.createMarshaller();
        profileMarshaller.marshal(profile, configProfileFile);
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
