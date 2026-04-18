package com.bobbuy.service;

import com.bobbuy.api.ProcurementHudResponse;
import com.bobbuy.api.TripExpenseResponse;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class FinancialPdfService {
  private static final Logger log = LoggerFactory.getLogger(FinancialPdfService.class);

  private final String cjkFontPath;

  public FinancialPdfService(@Value("${bobbuy.pdf.cjk-font-path:fonts/NotoSansCJKsc-Regular.otf}") String cjkFontPath) {
    this.cjkFontPath = cjkFontPath;
  }

  public byte[] buildTripSettlementPdf(ProcurementHudResponse hud, List<TripExpenseResponse> expenses) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document();
      PdfWriter.getInstance(document, out);
      document.open();

      Font title = new Font(resolveBaseFont(), 16, Font.BOLD);
      Font body = new Font(resolveBaseFont(), 10, Font.NORMAL);
      document.add(new Paragraph("行程结算单 / Trip Settlement", title));
      document.add(new Paragraph(" "));
      document.add(new Paragraph("Trip ID: " + hud.getTripId(), body));
      document.add(new Paragraph("Estimated Profit: " + hud.getTotalEstimatedProfit(), body));
      document.add(new Paragraph("Purchased Amount: " + hud.getCurrentPurchasedAmount(), body));
      document.add(new Paragraph("FX: " + hud.getCurrentFxRate() + " / " + hud.getReferenceFxRate(), body));
      document.add(new Paragraph("Total Expenses: " + hud.getTotalTripExpenses(), body));
      document.add(new Paragraph(" "));
      document.add(new Paragraph("支出明细 / Expenses", title));
      document.add(buildTripExpenseTable(expenses, body));

      document.close();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate settlement PDF", ex);
    }
  }

  public byte[] buildCustomerStatementPdf(OrderHeader order) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document();
      PdfWriter.getInstance(document, out);
      document.open();

      Font title = new Font(resolveBaseFont(), 16, Font.BOLD);
      Font body = new Font(resolveBaseFont(), 10, Font.NORMAL);
      document.add(new Paragraph("客户对账单 / Customer Statement", title));
      document.add(new Paragraph(" "));
      document.add(new Paragraph("Business ID: " + order.getBusinessId(), body));
      document.add(new Paragraph("Customer ID: " + order.getCustomerId(), body));
      document.add(new Paragraph("Trip ID: " + order.getTripId(), body));
      document.add(new Paragraph(" "));
      document.add(buildCustomerLineTable(order.getLines(), body));
      document.add(new Paragraph(" "));

      double totalReceivable = calculateTotalReceivable(order);
      double paidDeposit = order.getPaymentStatus() != null && order.getPaymentStatus().name().equals("PAID")
          ? totalReceivable : 0D;
      double outstanding = Math.max(totalReceivable - paidDeposit, 0D);
      document.add(new Paragraph("总应收 / Total Receivable: " + round2(totalReceivable), body));
      document.add(new Paragraph("已付定金 / Paid Deposit: " + round2(paidDeposit), body));
      document.add(new Paragraph("待收尾款 / Outstanding: " + round2(outstanding), body));

      document.close();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate customer statement PDF", ex);
    }
  }

  private PdfPTable buildTripExpenseTable(List<TripExpenseResponse> expenses, Font body) {
    PdfPTable table = new PdfPTable(new float[] {1.2F, 2.2F, 1.2F, 2.4F});
    table.setWidthPercentage(100);
    table.addCell(cell("ID", body));
    table.addCell(cell("Category", body));
    table.addCell(cell("Cost", body));
    table.addCell(cell("Created At", body));
    for (TripExpenseResponse expense : expenses) {
      table.addCell(cell(String.valueOf(expense.getId()), body));
      table.addCell(cell(expense.getCategory(), body));
      table.addCell(cell(String.valueOf(round2(expense.getCost())), body));
      table.addCell(cell(String.valueOf(expense.getCreatedAt()), body));
    }
    return table;
  }

  private PdfPTable buildCustomerLineTable(List<OrderLine> lines, Font body) {
    PdfPTable table = new PdfPTable(new float[] {1.8F, 2.4F, 1F, 1F, 1.2F});
    table.setWidthPercentage(100);
    table.addCell(cell("SKU", body));
    table.addCell(cell("商品 / Item", body));
    table.addCell(cell("数量", body));
    table.addCell(cell("单价", body));
    table.addCell(cell("小计", body));
    if (lines != null) {
      for (OrderLine line : lines) {
        int settledQty = Math.max(line.getPurchasedQuantity(), 0) > 0 ? line.getPurchasedQuantity() : line.getQuantity();
        double lineTotal = settledQty * line.getUnitPrice();
        table.addCell(cell(line.getSkuId(), body));
        table.addCell(cell(line.getItemName(), body));
        table.addCell(cell(String.valueOf(settledQty), body));
        table.addCell(cell(String.valueOf(round2(line.getUnitPrice())), body));
        table.addCell(cell(String.valueOf(round2(lineTotal)), body));
      }
    }
    return table;
  }

  private PdfPCell cell(String text, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    return cell;
  }

  private BaseFont resolveBaseFont() {
    try {
      ClassPathResource resource = new ClassPathResource(cjkFontPath);
      if (resource.exists()) {
        byte[] bytes;
        try (var in = resource.getInputStream()) {
          bytes = in.readAllBytes();
        }
        return BaseFont.createFont(resource.getFilename(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
      }
    } catch (IOException ex) {
      log.warn("Failed to read CJK font '{}', fallback to Helvetica", cjkFontPath, ex);
    } catch (Exception ex) {
      log.warn("Failed to initialize CJK font '{}', fallback to Helvetica", cjkFontPath, ex);
    }
    try {
      return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize fallback PDF font", ex);
    }
  }

  private double calculateTotalReceivable(OrderHeader order) {
    if (order.getLines() == null) {
      return 0D;
    }
    return order.getLines().stream()
        .mapToDouble(line -> {
          int settledQty = Math.max(line.getPurchasedQuantity(), 0) > 0 ? line.getPurchasedQuantity() : line.getQuantity();
          return settledQty * line.getUnitPrice();
        })
        .sum();
  }

  private double round2(double value) {
    return Math.round(value * 100D) / 100D;
  }
}
