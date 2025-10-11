package com.example.tmdt.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.example.tmdt.domain.NewsItem;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class RssService {

    private static final long TTL_MS = 5 * 60 * 1000; // cache 5 phút

    private static class Cache {
        long fetchedAt;
        List<NewsItem> items;
    }

    private final HashMap<String, Cache> cache = new HashMap<>();

    // Các url mẫu (bổ sung / thay đổi theo nhu cầu)
    public String getFeedUrlForSource(String source) {
    switch (source.toLowerCase()) {
        case "dantri":
            return "https://dantri.com.vn/trangchu.rss";
        case "technology":
        case "tech":
            return "https://vnexpress.net/rss/so-hoa.rss";
        case "genk":
            return "https://genk.vn/index.rss";
        case "thanhnien":
            return "https://thanhnien.vn/rss.html";
        case "ttxvn":
            return "https://baotintuc.vn/rss.htm";
        case "vtv":
            return "https://vtv.vn/rss.htm";
        case "agiletech":
            return "https://agiletech.vn/feed";
        case "vts":
            return "https://vietnamtechsociety.substack.com/feed";
        case "aht":
            return "https://blog.arrowhitech.com/feed";
        case "techvify":
            return "https://techvify-software.com/news";
        case "vninsider":
            return "https://vninsider.vn/rss/tech";
        case "znews":
            return "https://znews.vn/rss/technology";
        case "kenh14":
            return "https://kenh14.vn/rss/technology.rss";
        case "soha":
            return "https://soha.vn/rss/technology.rss";
        case "vietnamplus":
            return "https://vietnamplus.vn/rss/technology.rss";
        case "vna":
            return "https://vnanet.vn/rss/science-technology.rss";
        case "synodus":
            return "https://synodus.vn/rss";
        case "saigontech":
            return "https://saigontech.vn/rss";
        default:
            return "https://vnexpress.net/rss/tin-moi-nhat.rss";
    }
}


    public List<NewsItem> getNews(String source, int limit) {
        String url = getFeedUrlForSource(source);
        Cache c = cache.get(url);
        long now = System.currentTimeMillis();
        if (c != null && (now - c.fetchedAt) < TTL_MS) {
            return c.items;
        }
        List<NewsItem> items = fetchFeed(url, limit);
        Cache nc = new Cache();
        nc.fetchedAt = now;
        nc.items = items;
        cache.put(url, nc);
        return items;
    }

    private List<NewsItem> fetchFeed(String feedUrl, int limit) {
        List<NewsItem> list = new ArrayList<>();
        try (XmlReader reader = new XmlReader(new URL(feedUrl))) {
            SyndFeedInput input = new SyndFeedInput();
            var feed = input.build(reader);

            int added = 0;
            for (SyndEntry e : feed.getEntries()) {
                if (limit > 0 && added >= limit) break;

                String title = e.getTitle();
                String link = e.getLink();
                Date pubDate = e.getPublishedDate();

                // try enclosure first (media)
                String image = null;
                if (e.getEnclosures() != null && !e.getEnclosures().isEmpty()) {
                    for (SyndEnclosure enc : e.getEnclosures()) {
                        String type = enc.getType();
                        if (type != null && type.startsWith("image")) {
                            image = enc.getUrl();
                            break;
                        }
                    }
                }

                // try description html -> first img
                if (image == null) {
                    SyndContent desc = e.getDescription();
                    if (desc != null && desc.getValue() != null) {
                        Document doc = Jsoup.parse(desc.getValue());
                        Element img = doc.selectFirst("img");
                        if (img != null) {
                            String src = img.attr("src");
                            if (src != null && !src.isBlank()) image = src;
                        }
                    }
                }

                String descText = null;
                if (e.getDescription() != null) descText = Jsoup.parse(e.getDescription().getValue()).text();

                list.add(new NewsItem(title, link, pubDate, image, descText));
                added++;
            }
        } catch (Exception ex) {
            // log if cần
            ex.printStackTrace();
        }
        return list;
    }
}