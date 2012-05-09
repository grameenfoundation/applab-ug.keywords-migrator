package applab.keywords.migration;

public class MenuItem {
    private String parentKeyword;
    private String grandParentKeyword;
    private String menuItem;
    private String content;
    private MenuItem parentMenuItem;
    private String tieBreaker;

    public MenuItem() {
        this.parentKeyword = "";
        this.grandParentKeyword = "";
        this.menuItem = "";
        this.content = "";
        this.parentMenuItem = null;
        this.tieBreaker = "";
    }
    
    public MenuItem(String menuItem, String parentKeyword, String grandParentKeyword, String tieBreaker) {
        this.menuItem = menuItem;
        this.parentKeyword = parentKeyword;
        this.grandParentKeyword = grandParentKeyword;
        this.tieBreaker = tieBreaker;
    }
    public void setParentKeyword(String parentKeyword) {
        this.parentKeyword = parentKeyword;
    }
    public String getParentKeyword() {
        return parentKeyword;
    }
    public void setGrandParentKeyword(String grandParentKeyword) {
        this.grandParentKeyword = grandParentKeyword;
    }
    public String getGrandParentKeyword() {
        return grandParentKeyword;
    }
    public void setMenuItem(String menuItem) {
        this.menuItem = menuItem;
    }
    public String getMenuItem() {
        return menuItem;
    }
    public void setContent(String content) {
        if (content.length() > 251) {
            this.content = content.substring(0,250);
        }
        else {
            this.content = content;
        }
    }
    public String getContent() {
        return content;
    }

    public void setParentMenuItem(MenuItem parentMenuItem) {
		this.parentMenuItem = parentMenuItem;
	}
	public MenuItem getParentMenuItem() {
		return parentMenuItem;
	}
	public String getTieBreaker() {
		return tieBreaker;
	}

	public void setTieBreaker(String tieBreaker) {
		this.tieBreaker = tieBreaker;
	}

	public String toString() {
        return this.menuItem;
    }

    public int hashCode() {
        return this.tieBreaker.length();
    }

    public boolean equals(Object menuItem) {
        //fileUtil.writeLinesToFile("output.txt", new String[]{"this :" + this.tieBreaker.trim() , "that :"+ ((MenuItem)menuItem).tieBreaker.trim()}, true);
        if (menuItem != null && menuItem instanceof MenuItem 
                && ((MenuItem)menuItem).tieBreaker.trim().equalsIgnoreCase(this.tieBreaker.trim())
                && ((MenuItem)menuItem).menuItem.trim().equalsIgnoreCase(this.menuItem.trim())) {
            return true;
        }
        return false;
    }
}
