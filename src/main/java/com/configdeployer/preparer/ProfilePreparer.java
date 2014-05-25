package com.configdeployer.preparer;

import com.configdeployer.binding.ConfigProfile;

public interface ProfilePreparer
{

    void prepareProfile(ConfigProfile profile) throws PreparerException;

}
