package com.odontocare.reports.service;

import com.odontocare.installation.model.InstallationProfile;
import com.odontocare.reports.dto.ReportContracts.*;
import java.io.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

@Component
public class ReportPdfRenderer {
  public byte[] render(
      InstallationProfile profile,
      Definition definition,
      Window window,
      List<Metric> totals,
      List<Row> rows,
      List<String> filters,
      String actor) {
    try (var pdf = new PDDocument();
        var fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf");
        var output = new ByteArrayOutputStream()) {
      var font = PDType0Font.load(pdf, fontStream);
      pdf.getDocumentInformation().setTitle(definition.title() + " - " + profile.getDisplayName());
      pdf.getDocumentInformation().setCreator("OdontoCare");
      try (var layout = new Layout(pdf, font, profile.getDisplayName(), definition.title())) {
        layout.line(profile.getLegalName(), 10);
        layout.line(profile.getAddress(), 9);
        layout.line(profile.getPhone() + "  " + profile.getEmail(), 9);
        layout.paragraph(profile.getDocumentHeader(), 9);
        layout.gap(8);
        layout.heading(definition.title());
        layout.paragraph(definition.description(), 10);
        layout.line(
            "Período: "
                + date(window.from().toString(), profile.getDateFormat())
                + " a "
                + date(window.to().toString(), profile.getDateFormat())
                + " | Fechas del consultorio",
            9);
        layout.line("Preparado por: " + actor, 9);
        for (String filter : filters) layout.paragraph(filter, 9);
        layout.gap(12);
        layout.heading("Totales del resultado filtrado");
        for (var metric : totals)
          layout.line(
              metric.label()
                  + ": "
                  + (metric.currency() == null
                      ? metric.value()
                      : money(metric.value(), metric.currency())),
              10);
        layout.gap(12);
        layout.heading("Detalle");
        if (rows.isEmpty()) layout.line("Sin registros para los filtros seleccionados.", 10);
        int position = 0;
        for (var row : rows) {
          layout.ensure(95);
          layout.heading("Registro " + (++position));
          for (var column : definition.columns()) {
            String value = row.values().getOrDefault(column.key(), "");
            if (column.kind().equals("MONEY") && !value.isBlank())
              value = money(value, row.values().getOrDefault("currency", profile.getCurrency()));
            else if (column.kind().equals("DATE_TIME") && !value.isBlank())
              value =
                  date(value.substring(0, 10), profile.getDateFormat())
                      + " "
                      + value.substring(11, 16);
            else if (column.kind().equals("DATE") && !value.isBlank())
              value = date(value, profile.getDateFormat());
            layout.paragraph(column.label() + ": " + (value.isBlank() ? "-" : value), 10);
          }
          layout.rule();
          layout.gap(10);
        }
        layout.gap(8);
        layout.paragraph(profile.getDocumentFooter(), 9);
      }
      pdf.save(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo preparar el reporte PDF.", e);
    }
  }

  private String money(String value, String currency) {
    try {
      var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));
      format.setCurrency(Currency.getInstance(currency));
      return format.format(new BigDecimal(value));
    } catch (IllegalArgumentException e) {
      return currency + " " + value;
    }
  }

  private String date(String value, String format) {
    return java.time.LocalDate.parse(value)
        .format(
            DateTimeFormatter.ofPattern(
                format.equals("MDY")
                    ? "MM/dd/yyyy"
                    : format.equals("YMD") ? "yyyy-MM-dd" : "dd/MM/yyyy"));
  }

  private static final class Layout implements AutoCloseable {
    private final PDDocument document;
    private final PDType0Font font;
    private final String clinic, title;
    private PDPageContentStream stream;
    private float y;
    private int page;

    Layout(PDDocument document, PDType0Font font, String clinic, String title) throws IOException {
      this.document = document;
      this.font = font;
      this.clinic = clinic;
      this.title = title;
      newPage();
    }

    private void newPage() throws IOException {
      if (stream != null) stream.close();
      var sheet = new PDPage(PDRectangle.A4);
      document.addPage(sheet);
      stream = new PDPageContentStream(document, sheet);
      page++;
      stream.setNonStrokingColor(0.13f, 0.37f, 0.30f);
      text(safe(clinic), 42, 796, 14);
      stream.setNonStrokingColor(0.36f, 0.40f, 0.40f);
      text("OdontoCare | " + safe(title), 42, 776, 9);
      text("Página " + page + " | Reporte administrativo", 42, 35, 9);
      y = 752;
      rule();
    }

    void ensure(float height) throws IOException {
      if (y - height < 64) newPage();
    }

    void gap(float value) {
      y -= value;
    }

    void heading(String value) throws IOException {
      ensure(32);
      stream.setNonStrokingColor(0.13f, 0.37f, 0.30f);
      line(value, 12);
      stream.setNonStrokingColor(0.15f, 0.18f, 0.18f);
      gap(4);
    }

    void line(String value, int size) throws IOException {
      if (value.isBlank()) return;
      paragraph(value, size);
    }

    void paragraph(String value, int size) throws IOException {
      for (String line : wrap(safe(value), size)) {
        ensure(size + 8);
        text(line, 42, y, size);
        y -= size + 6;
      }
    }

    void rule() throws IOException {
      ensure(8);
      stream.setStrokingColor(0.86f, 0.90f, 0.89f);
      stream.moveTo(42, y);
      stream.lineTo(553, y);
      stream.stroke();
      y -= 8;
    }

    private void text(String value, float x, float baseline, int size) throws IOException {
      stream.beginText();
      stream.setFont(font, size);
      stream.newLineAtOffset(x, baseline);
      stream.showText(value);
      stream.endText();
    }

    private String safe(String value) {
      var text = new StringBuilder();
      value
          .codePoints()
          .forEach(
              cp -> {
                if (Character.isISOControl(cp)) {
                  text.append(' ');
                  return;
                }
                String character = new String(Character.toChars(cp));
                try {
                  font.encode(character);
                  text.append(character);
                } catch (IllegalArgumentException | IOException e) {
                  text.append('?');
                }
              });
      return text.toString();
    }

    private List<String> wrap(String value, int size) throws IOException {
      var result = new ArrayList<String>();
      var line = new StringBuilder();
      for (int cp : value.codePoints().toArray()) {
        String next = new String(Character.toChars(cp));
        if (font.getStringWidth(line + next) / 1000 * size > 508) {
          int space = line.lastIndexOf(" ");
          if (space > 0) {
            result.add(line.substring(0, space));
            line.delete(0, space + 1);
          } else {
            result.add(line.toString());
            line.setLength(0);
          }
        }
        line.append(next);
      }
      result.add(line.toString());
      return result;
    }

    @Override
    public void close() throws IOException {
      if (stream != null) stream.close();
    }
  }
}
