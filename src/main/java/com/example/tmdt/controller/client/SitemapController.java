package com.example.tmdt.controller.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tmdt.domain.Product;
import com.example.tmdt.repository.ProductRepository;

@RestController
public class SitemapController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        // Homepage
        sitemap.append("<url>")
                .append("<loc>").append("https://laptopshop247.click/").append("</loc>")
                .append("<changefreq>daily</changefreq>")
                .append("<priority>1.0</priority>")
                .append("</url>");

        // Products
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            sitemap.append("<url>")
                    .append("<loc>").append("https://laptopshop247.click/product/" + p.getSlug()).append("</loc>")
                    .append("<changefreq>weekly</changefreq>")
                    .append("<priority>0.8</priority>")
                    .append("</url>");
        }

        sitemap.append("</urlset>");
        return sitemap.toString();
    }
}

