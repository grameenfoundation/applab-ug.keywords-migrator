package applab.keywords.migration;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * class that represents a Keyword row from the Keywords table. in the search database Copyright (C) 2012 Grameen
 * Foundation
 */
public class Keyword {
    private String id;
    private String breadcrumb;
    private String content;
    private boolean isActive;
    private String attribution;
    private String updatedDate;
    private String createdDate;
   // private Calendar.

    public Keyword(String id, String breadcrumb, String content, String attribution, int isActive, String createdDate, String updatedDate) {
        this.setId(id);
        this.setBreadcrumb(breadcrumb);
        this.setContent(content);
        this.setAttribution(attribution);
        this.setActive(isActive);
        this.setUpdatedDate(updatedDate);
        this.setCreatedDate(createdDate);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setBreadcrumb(String breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    public String getBreadcrumb() {
        return breadcrumb;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(int isActive) {
        this.isActive = isActive == 1 ? false : true;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean equals(Object object) {
        if (object instanceof Keyword && ((Keyword)object).getBreadcrumb().equalsIgnoreCase(this.getBreadcrumb())) {
            return true;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return this.getBreadcrumb().length();
    }
}
