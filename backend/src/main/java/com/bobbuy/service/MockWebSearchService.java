package com.bobbuy.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MockWebSearchService implements WebSearchService {
  @Override
  public List<SearchResult> search(String query) {
    List<SearchResult> results = new ArrayList<>();
    if (query == null || query.isBlank()) {
      return results;
    }

    // Mocking logic for Costco/Matcha etc.
    if (query.toLowerCase().contains("matcha") || query.toLowerCase().contains("kitkat")) {
      results.add(new SearchResult(
          "Nestlé KitKat Mini Matcha Whole Wheat Flour - Official",
          "https://www.nestle.jp/kitkat/matcha",
          "Experience the perfect blend of high-quality Uji matcha and whole wheat flour biscuits. A classic Japanese treat.",
          List.of(
              "https://images.unsplash.com/photo-1582176604447-aa5114e2776a?auto=format&fit=crop&w=800&q=80",
              "https://images.unsplash.com/photo-1627308595229-7830a5c91f9f?auto=format&fit=crop&w=800&q=80"
          )
      ));
    } else if (query.toLowerCase().contains("organic milk") || query.toLowerCase().contains("costco")) {
        results.add(new SearchResult(
            "Kirkland Signature Organic Whole Milk, 3 pk/64 fl oz",
            "https://www.costco.com/kirkland-signature-organic-whole-milk.product.100344145.html",
            "Kirkland Signature Organic Whole Milk is produced on family farms and is USDA certified organic. 3 pack of 64 fl oz.",
            List.of(
                "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1563636619-e91082a119d2?auto=format&fit=crop&w=800&q=80"
            )
        ));
    } else {
      results.add(new SearchResult(
          "Product Detail for " + query,
          "https://www.example.com/search?q=" + query,
          "Automatically generated search result for the identified product: " + query,
          List.of("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80")
      ));
    }

    return results;
  }
}
