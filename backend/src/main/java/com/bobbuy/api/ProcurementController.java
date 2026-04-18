package com.bobbuy.api;

import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.service.ProcurementHudService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
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

  @GetMapping("/{tripId}/expenses")
  public ResponseEntity<ApiResponse<List<TripExpenseResponse>>> expenses(@PathVariable Long tripId) {
    List<TripExpenseResponse> items = procurementHudService.getTripExpenses(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @PostMapping("/{tripId}/expenses")
  public ResponseEntity<ApiResponse<TripExpenseResponse>> createExpense(@PathVariable Long tripId,
                                                                        @RequestBody TripExpenseRequest request) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.createTripExpense(tripId, request)));
  }

  @PostMapping("/{tripId}/manual-reconcile")
  public ResponseEntity<ApiResponse<ManualReconcileResponse>> manualReconcile(@PathVariable Long tripId,
                                                                               @RequestBody ManualReconcileRequest request) {
    int transferred = procurementHudService.manualReconcile(
        tripId,
        request.getSkuId(),
        request.getFromBusinessId(),
        request.getToBusinessId(),
        request.getQuantity());
    return ResponseEntity.ok(ApiResponse.success(
        new ManualReconcileResponse(
            request.getSkuId(),
            request.getFromBusinessId(),
            request.getToBusinessId(),
            transferred)));
  }

  @GetMapping("/{tripId}/export")
  public ResponseEntity<byte[]> exportTripSettlement(@PathVariable Long tripId,
                                                     @RequestParam(defaultValue = "csv") String format) {
    ProcurementHudResponse hud = procurementHudService.getHudStats(tripId);
    List<TripExpenseResponse> expenses = procurementHudService.getTripExpenses(tripId);
    String csvContent = buildSettlementCsv(hud, expenses);
    if ("pdf".equalsIgnoreCase(format)) {
      byte[] pdfBytes = buildSimplePdf(csvContent);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-" + tripId + "-settlement.pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdfBytes);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-" + tripId + "-settlement.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csvContent.getBytes(StandardCharsets.UTF_8));
  }

  private String buildSettlementCsv(ProcurementHudResponse hud, List<TripExpenseResponse> expenses) {
    StringBuilder builder = new StringBuilder();
    builder.append("tripId,currentFxRate,referenceFxRate,currentPurchasedAmount,totalTripExpenses,totalEstimatedProfit\n");
    builder.append(hud.getTripId()).append(",")
        .append(hud.getCurrentFxRate()).append(",")
        .append(hud.getReferenceFxRate()).append(",")
        .append(hud.getCurrentPurchasedAmount()).append(",")
        .append(hud.getTotalTripExpenses()).append(",")
        .append(hud.getTotalEstimatedProfit()).append("\n\n");
    builder.append("expenseId,category,cost,createdAt\n");
    for (TripExpenseResponse expense : expenses) {
      builder.append(expense.getId()).append(",")
          .append(csvSafe(expense.getCategory())).append(",")
          .append(expense.getCost()).append(",")
          .append(expense.getCreatedAt()).append("\n");
    }
    return builder.toString();
  }

  private String csvSafe(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private byte[] buildSimplePdf(String text) {
    String safeText = text
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("\r", "")
        .replace("\n", "\\n");
    String content = "BT /F1 10 Tf 40 800 Td (" + safeText + ") Tj ET";
    byte[] streamBytes = content.getBytes(StandardCharsets.UTF_8);
    StringBuilder pdf = new StringBuilder();
    pdf.append("%PDF-1.4\n");
    int obj1 = pdf.length();
    pdf.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
    int obj2 = pdf.length();
    pdf.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
    int obj3 = pdf.length();
    pdf.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n");
    int obj4 = pdf.length();
    pdf.append("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");
    int obj5 = pdf.length();
    pdf.append("5 0 obj << /Length ").append(streamBytes.length).append(" >> stream\n");
    pdf.append(content).append("\nendstream endobj\n");
    int xref = pdf.length();
    pdf.append("xref\n0 6\n0000000000 65535 f \n")
        .append(String.format("%010d 00000 n \n", obj1))
        .append(String.format("%010d 00000 n \n", obj2))
        .append(String.format("%010d 00000 n \n", obj3))
        .append(String.format("%010d 00000 n \n", obj4))
        .append(String.format("%010d 00000 n \n", obj5))
        .append("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n")
        .append(xref)
        .append("\n%%EOF");
    return pdf.toString().getBytes(StandardCharsets.UTF_8);
  }
}
