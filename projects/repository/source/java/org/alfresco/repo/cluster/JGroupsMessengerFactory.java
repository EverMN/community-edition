/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.repo.cluster;

import java.io.Serializable;

import org.alfresco.repo.jgroups.AlfrescoJGroupsChannelFactory;
import org.alfresco.util.PropertyCheck;
import org.jgroups.Channel;
import org.springframework.beans.factory.annotation.Required;

/**
 * JGroups implementation of the {@link MessengerFactory} interface.
 * 
 * @author Matt Ward
 */
public class JGroupsMessengerFactory implements MessengerFactory
{
    private String appRegion;
    
    
    @Override
    public <T extends Serializable> Messenger<T> createMessenger()
    {
        PropertyCheck.mandatory(this, "appRegion", appRegion);
        Channel channel = AlfrescoJGroupsChannelFactory.getChannel(appRegion);
        return new JGroupsMessenger<T>(channel);
    }

    @Required
    public void setAppRegion(String appRegion)
    {
        this.appRegion = appRegion;
    }    
}
