/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 * This file is part of Alfresco
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.po.share.dashlet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.alfresco.webdrone.RenderElement.getVisibleRenderElement;

import java.util.NoSuchElementException;

import org.alfresco.po.share.SharePage;
import org.alfresco.po.share.exception.ShareException;
import org.alfresco.webdrone.RenderTime;
import org.alfresco.webdrone.WebDrone;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Page object to reflect the rss feed url box web elements
 * 
 * @author Marina.Nenadovets
 */
public class RssFeedUrlBoxPage extends SharePage
{
    private static final By OK_BUTTON = By.cssSelector("button[id$='configDialog-ok-button']");
    private static final By CANCEL_BUTTON = By.cssSelector("button[id$='configDialog-cancel-button']");
    private static final By CLOSE_BUTTON = By.cssSelector(".container-close");
    private static final By URL_FIELD = By.cssSelector("input[id$='configDialog-url']");
    private static final By CHK_OPEN_IN_NEW_WINDOW = By.cssSelector("input[id$='default-configDialog-new_window']");

    public enum NrItems
    {
        ALL("All"), Five("5"), Ten("10"), Fifteen("15"), Twenty("20"), twentyFive("25");

        private String label;

        private NrItems(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }
    }

    /**
     * Constructor.
     */
    protected RssFeedUrlBoxPage(WebDrone drone)
    {
        super(drone);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RssFeedUrlBoxPage render(RenderTime timer)
    {
        elementRender(timer, getVisibleRenderElement(OK_BUTTON), getVisibleRenderElement(CANCEL_BUTTON), getVisibleRenderElement(CLOSE_BUTTON));
        return this;

    }

    @SuppressWarnings("unchecked")
    @Override
    public RssFeedUrlBoxPage render(long time)
    {
        return render(new RenderTime(time));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RssFeedUrlBoxPage render()
    {
        return render(new RenderTime(maxPageLoadingTime));
    }

    /**
     * Mimic click on OK button.
     */
    public void clickOk()
    {
        click(OK_BUTTON);
    }

    /**
     * Mimic click on CANCEL button.
     */
    public void clickCancel()
    {
        click(CANCEL_BUTTON);
    }

    /**
     * Mimic click on CLOSE button.
     */
    public void clickClose()
    {
        click(CLOSE_BUTTON);
    }

    /**
     * Mimic fill URL field
     * 
     * @param url
     */
    public void fillURL(String url)
    {
        fillField(URL_FIELD, url);
    }

    private void click(By locator)
    {
        drone.waitUntilElementPresent(locator, 5);
        WebElement element = drone.findAndWait(locator);
        drone.executeJavaScript("arguments[0].click();", element);
    }

    private void fillField(By selector, String text)
    {
        checkNotNull(selector);
        checkNotNull(text);
        WebElement inputField = drone.findAndWait(selector);
        inputField.clear();
        inputField.sendKeys(text);
    }

    public void selectNrOfItemsToDisplay(NrItems items)
    {
        try
        {
            Select select = new Select(drone.findAndWait(By.cssSelector("select[id$='_default-configDialog-limit']")));
            select.selectByValue(items.getLabel());
        }
        catch (TimeoutException te)
        {
            throw new ShareException("Unable to find drop-down", te);
        }
    }

    public void selectOpenLinkNewWindow()
    {
        try
        {
            WebElement element = drone.findAndWait(CHK_OPEN_IN_NEW_WINDOW);
            element.click();
        }
        catch (TimeoutException te)
        {
            throw new ShareException("Unable to find checkbox", te);
        }
    }
    
    public boolean isLinkNewWindowSelected()
    {
        boolean selected;
        
        try
        {
            WebElement element = drone.findAndWait(CHK_OPEN_IN_NEW_WINDOW);
            selected = element.isSelected();
            return selected;
        }
        catch (TimeoutException te)
        {
            throw new ShareException("Unable to find checkbox", te);
        }
    }
    
    public void clearUrlField()
    {
        try
        {
            WebElement element = drone.find(URL_FIELD);
            element.clear();

        }
        catch (TimeoutException te)
        {
            throw new ShareException("Unable to find Url field", te);
        } 
    }

    public String getValidationMessageFromUrlField(String text)
    {
        try
        {
            WebElement element = drone.find(URL_FIELD);
            element.clear();
            element.sendKeys(text);
            clickOk();

            return getValidationMessage(element);
        }
        catch (TimeoutException te)
        {
            throw new ShareException("Unable to find Url field", te);
        }
    }
    
    /**
     * Wait until check box open in new window disappears
     * 
     * @param url
     */
    public void waitUntilCheckDisapperers()
    {
        drone.waitUntilElementDisappears(CHK_OPEN_IN_NEW_WINDOW, 30);
    }
    
    public boolean isOkButtonEnabled()
    {
        try
        {
            return drone.find(OK_BUTTON).isEnabled();
        }
        catch (NoSuchElementException te)
        {
            throw new ShareException("Unable to find Ok button", te);
        }
    }
}
