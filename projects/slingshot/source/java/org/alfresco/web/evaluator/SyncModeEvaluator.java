/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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
package org.alfresco.web.evaluator;

import org.alfresco.web.scripts.SyncModeConfig;
import org.json.simple.JSONObject;

/**
 * Evaluates whether the sync mode is the specified value
 *
 * @author: David Webster
 */
public class SyncModeEvaluator extends BaseEvaluator
{
    private SyncModeConfig syncMode;
    private String validMode;

    /**
     * Sync Mode bean reference
     * 
     * @param syncMode
     */
    public void setSyncMode(SyncModeConfig syncMode)
    {
        this.syncMode = syncMode;
    }

    /**
     * Define the value to check for
     *
     * @param validMode
     */
    public void setValidMode(String validMode)
    {
        this.validMode = validMode;
    }

    @Override
    public boolean evaluate(JSONObject jsonObject)
    {
        return syncMode.getValue().equals(validMode);
    }
}
