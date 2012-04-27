package applab.keywords.migration;

/**
 * class that represents a unique Menu Item 
 * as it will be saved in Salesforce. This is one off code 
 * but I recognise that I could have merged this with the Keyword object
 *  Copyright (C) 2012 Grameen Foundation
 */
public class MenuItem {
    private String menuItem;
    private String content;
    private String menuItemPath;

    public MenuItem() {
        this.menuItem = "";
        this.content = "";
        this.menuItemPath = "";
    }
    
    public MenuItem(String menuItem, String parentKeyword, String grandParentKeyword, String tieBreaker) {
        this.menuItem = menuItem.trim().replaceAll("\\s+", " ");
        this.menuItemPath = tieBreaker.trim().replaceAll("\\s+", " ");
    }

    public void setMenuItem(String menuItem) {
        this.menuItem = menuItem.trim().replaceAll("\\s+", " ");
    }

    public String getMenuItem() {
        return menuItem;
    }

    public void setContent(String content) {
            this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getTieBreaker() {
        return menuItemPath;
    }

    public void setMenuItemPath(String menuItemPath) {
        this.menuItemPath = menuItemPath.trim().replaceAll("\\s+", " ");
    }

    public String toString() {
        return this.menuItem;
    }

    public int hashCode() {
        return this.menuItemPath.length();
    }

    public boolean equals(Object menuItem) {
        if (menuItem != null && menuItem instanceof MenuItem 
                && ((MenuItem)menuItem).menuItemPath.trim().equalsIgnoreCase(this.menuItemPath.trim())
                && ((MenuItem)menuItem).menuItem.trim().equalsIgnoreCase(this.menuItem.trim())) {
            return true;
        }
        return false;
    }
}
