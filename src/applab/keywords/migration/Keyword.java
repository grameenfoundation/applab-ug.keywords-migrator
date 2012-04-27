package applab.keywords.migration;

/**
 * class that represents a Keyword row from the Keywords table.
 * in the search database
 *  Copyright (C) 2012 Grameen Foundation
 */
public class Keyword {
    private String id;
    private String label;
    private String content;

    public Keyword(String id, String label, String content) {
        this.setId(id);
        this.setLabel(label);
        this.setContent(content);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}
	
	public boolean equals(Object object) {
		if (object instanceof Keyword && ((Keyword)object).getLabel().equalsIgnoreCase(this.getLabel())) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public int hashCode() {
		return this.getLabel().length();
	}
}
