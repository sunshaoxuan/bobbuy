package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Category;
import com.bobbuy.service.BobbuyStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile/categories")
public class MobileCategoryController {
    private final BobbuyStore store;

    public MobileCategoryController(BobbuyStore store) {
        this.store = store;
    }

    @GetMapping
    public ApiResponse<List<Category>> list() {
        List<Category> categories = store.listCategories();
        return ApiResponse.success(categories, new ApiMeta(categories.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> get(@PathVariable String id) {
        Category category = store.getCategory(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.category.not_found"));
        return ResponseEntity.ok(ApiResponse.success(category));
    }
}
