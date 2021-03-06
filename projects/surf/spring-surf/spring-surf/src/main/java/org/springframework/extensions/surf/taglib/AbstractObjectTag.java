/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This file is part of the Spring Surf Extension project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.extensions.surf.taglib;

/**
 * 
 * @author David Draper
 * @author muzquiano
 */
public abstract class AbstractObjectTag extends RenderServiceTag
{
    private static final long serialVersionUID = 5383361788731531793L;

    private String pageId = null;
    private String pageTypeId = null;
    private String objectId = null;
    private String formatId = null;

    /**
     * <p>The life-cycle of a custom JSP tag is that the class is is instantiated when it is first required and
     * then re-used for all subsequent invocations. When a JSP has non-mandatory properties it means that the 
     * setters for those properties will not be called if the properties are not provided and the old values
     * will still be available which can corrupt the behaviour of the code. In order to prevent this from happening
     * we should override the <code>release</code> method to ensure that all instance variables are reset to their
     * initial state.</p>
     */
    @Override
    public void release()
    {
        super.release();
        this.pageId = null;
        this.pageTypeId = null;
        this.objectId = null;
        this.formatId = null;
    }
    
    public void setPage(String pageId)
    {
        this.pageId = pageId;
    }

    public String getPage()
    {
        return this.pageId;
    }
    
    public void setPageType(String pageTypeId)
    {
        this.pageTypeId = pageTypeId;
    }
    
    public String getPageType()
    {
        return this.pageTypeId;
    }
    
    public void setObject(String objectId)
    {
        this.objectId = objectId;
    }
    
    public String getObject()
    {
        return this.objectId;
    }

    public void setFormat(String formatId)
    {
        this.formatId = formatId;
    }

    public String getFormat()
    {
        return this.formatId;
    }
}
