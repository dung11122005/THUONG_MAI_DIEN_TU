package com.example.tmdt.domain;

import java.util.Date;

public class NewsItem {
    private String title;
    private String link;
    private Date publishedDate;
    private String imageUrl;
    private String description;

    public NewsItem(String title, String link, Date publishedDate, String imageUrl, String description) {
        this.title = title;
        this.link = link;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getLink() { return link; }
    public Date getPublishedDate() { return publishedDate; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
}
