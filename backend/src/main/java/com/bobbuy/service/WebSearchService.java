package com.bobbuy.service;

import java.util.List;

public interface WebSearchService {
  List<SearchResult> search(String query);

  record SearchResult(String title, String url, String snippet, List<String> imageUrls) {
  }
}
