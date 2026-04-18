package com.bobbuy.api;

import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.service.ProcurementHudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementController {
  private final ProcurementHudService procurementHudService;

  public ProcurementController(ProcurementHudService procurementHudService) {
    this.procurementHudService = procurementHudService;
  }

  @GetMapping("/{tripId}/hud")
  public ResponseEntity<ApiResponse<ProcurementHudResponse>> hud(@PathVariable Long tripId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.getHudStats(tripId)));
  }

  @GetMapping("/{tripId}/deficit")
  public ResponseEntity<ApiResponse<List<ProcurementDeficitItemResponse>>> deficit(@PathVariable Long tripId) {
    List<ProcurementDeficitItemResponse> items = procurementHudService.getDeficitItems(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }
}
