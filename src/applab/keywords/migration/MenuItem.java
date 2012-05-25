package applab.keywords.migration;

public class MenuItem {
    private String menuItem;
    private String content;
    private String tieBreaker;

    public MenuItem() {
        this.menuItem = "";
        this.content = "";
        this.tieBreaker = "";
    }
    
    public MenuItem(String menuItem, String parentKeyword, String grandParentKeyword, String tieBreaker) {
        this.menuItem = menuItem;
        this.tieBreaker = tieBreaker;
    }
    public void setMenuItem(String menuItem) {
        this.menuItem = menuItem;
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
        if (menuItem != null && menuItem instanceof MenuItem 
                && ((MenuItem)menuItem).tieBreaker.trim().equalsIgnoreCase(this.tieBreaker.trim())
                && ((MenuItem)menuItem).menuItem.trim().equalsIgnoreCase(this.menuItem.trim())) {
            return true;
        }
        return false;
    }
}
