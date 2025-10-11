package com.example.tmdt.controller.client;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.tmdt.domain.NewsItem;
import com.example.tmdt.service.RssService;

@Controller
public class NewsController {

    @Autowired
    private RssService rssService;

    @GetMapping("/news")
    public String listNews(
            @RequestParam(value = "source", required = false, defaultValue = "vnexpress") String source,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "hasImage", required = false, defaultValue = "false") boolean hasImage,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        List<NewsItem> items = rssService.getNews(source, limit);

        // apply filters
        List<NewsItem> filtered = items.stream()
                .filter(n -> {
                    if (q != null && !q.isBlank()) {
                        String low = q.toLowerCase();
                        String title = n.getTitle() == null ? "" : n.getTitle().toLowerCase();
                        String desc = n.getDescription() == null ? "" : n.getDescription().toLowerCase();
                        if (!title.contains(low) && !desc.contains(low)) return false;
                    }
                    if (hasImage) {
                        String img = n.getImageUrl();
                        if (img == null || img.isBlank()) return false;
                    }
                    if (from != null || to != null) {
                        if (n.getPublishedDate() == null) return false;
                        LocalDate published = n.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (from != null && published.isBefore(from)) return false;
                        if (to != null && published.isAfter(to)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        model.addAttribute("newsList", filtered);
        model.addAttribute("source", source);

        // keep filter values for the form
        model.addAttribute("q", q);
        model.addAttribute("hasImage", hasImage);
        model.addAttribute("from", from == null ? null : from.toString());
        model.addAttribute("to", to == null ? null : to.toString());

        return "client/news/list";
    }
}