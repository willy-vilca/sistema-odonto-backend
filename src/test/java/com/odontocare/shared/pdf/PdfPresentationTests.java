package com.odontocare.shared.pdf;

import static com.odontocare.shared.pdf.PdfContent.*;
import static org.assertj.core.api.Assertions.*;

import com.odontocare.installation.model.InstallationProfile;
import java.io.IOException;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.*;
import org.junit.jupiter.api.Test;

class PdfPresentationTests {
  private final InstitutionalPdfRenderer renderer = new InstitutionalPdfRenderer();

  private InstallationProfile identity() {
    var profile = new InstallationProfile() {};
    profile.setDisplayName("Clínica Sonrisa Integral");
    profile.setLegalName("Consultorio de atención odontológica");
    profile.setAddress("Av. Los Jardines 245, Lima");
    profile.setPhone("+51 900 000 000");
    profile.setEmail("recepcion@example.test");
    profile.setDocumentFooter("Documento emitido por el consultorio.");
    return profile;
  }

  @Test
  void longTablesRepeatHeadersAndKeepEveryRowInsideThePage() throws Exception {
    var rows = new ArrayList<List<String>>();
    for (int i = 1; i <= 90; i++)
      rows.add(
          List.of(
              "11/10/2026",
              "Tratamiento " + i + " · " + "Detalle clínico legible y revisado. ".repeat(4),
              "9,999,999,999.99"));
    var content =
        new PdfContent()
            .field("Paciente", "María Pérez Ñandú")
            .field("Código", "PAC-000018")
            .total("Saldo pendiente", "9,999,999,999.99")
            .table(
                "Cargos y ajustes",
                List.of(column("Fecha", 1), column("Tratamiento", 3), amount("Importe", 1)),
                rows);
    try (var document =
        Loader.loadPDF(
            renderer.render(identity(), null, "Estado de cuenta", "REC-000120", content, false))) {
      int total = document.getNumberOfPages();
      assertThat(total).isGreaterThan(3);
      String text = new PDFTextStripper().getText(document);
      assertThat(text).contains("Tratamiento 90", "9,999,999,999.99", "11/10/2026");
      for (int number = 1; number <= total; number++) {
        var stripper = new PDFTextStripper();
        stripper.setStartPage(number);
        stripper.setEndPage(number);
        String page = stripper.getText(document);
        assertThat(page).contains("Página " + number + " de " + total, "Clínica Sonrisa Integral");
        if (page.contains("Tratamiento ")) assertThat(page).contains("Importe", "Fecha");
      }
      checkMargins(document);
    }
  }

  @Test
  void aVeryLongCellContinuesWithoutLosingItsEndOrOverlappingFooters() throws Exception {
    var content =
        new PdfContent()
            .table(
                "Detalle del movimiento",
                List.of(column("Descripción", 3), amount("Importe", 1)),
                List.of(
                    List.of(
                        "Inicio " + "TextoExtensoSinEspacios".repeat(300) + " FINAL-CONSERVADO",
                        "250.00")));
    try (var document =
        Loader.loadPDF(
            renderer.render(
                identity(), null, "Constancia de abono", "REC-000121", content, false))) {
      assertThat(document.getNumberOfPages()).isGreaterThan(1);
      assertThat(new PDFTextStripper().getText(document))
          .contains("FINAL-CONSERVADO", "continuación");
      checkMargins(document);
    }
  }

  @Test
  void emptyLandscapeReportsHaveAnExplicitResultAndInstitutionalInformation() throws Exception {
    var content =
        new PdfContent()
            .field("Período", "01/10/2026 a 11/10/2026")
            .total("Registros", "0")
            .table("Detalle", List.of(column("Paciente", 2), amount("Saldo", 1)), List.of());
    try (var document =
        Loader.loadPDF(renderer.render(identity(), null, "Saldos", "", content, true))) {
      assertThat(new PDFTextStripper().getText(document))
          .contains("Sin registros.", "Clínica Sonrisa Integral", "Página 1 de 1");
      assertThat(document.getPage(0).getMediaBox().getWidth())
          .isGreaterThan(document.getPage(0).getMediaBox().getHeight());
      checkMargins(document);
    }
  }

  private void checkMargins(org.apache.pdfbox.pdmodel.PDDocument document) throws IOException {
    for (int page = 0; page < document.getNumberOfPages(); page++) {
      final float width = document.getPage(page).getMediaBox().getWidth(),
          height = document.getPage(page).getMediaBox().getHeight();
      var positions =
          new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> glyphs) throws IOException {
              for (var glyph : glyphs) {
                assertThat(glyph.getXDirAdj()).isGreaterThanOrEqualTo(28);
                assertThat(glyph.getXDirAdj() + glyph.getWidthDirAdj())
                    .isLessThanOrEqualTo(width - 27);
                assertThat(glyph.getYDirAdj()).isGreaterThanOrEqualTo(22);
                assertThat(glyph.getYDirAdj() + glyph.getHeightDir())
                    .isLessThanOrEqualTo(height - 19);
              }
              super.writeString(text, glyphs);
            }
          };
      positions.setStartPage(page + 1);
      positions.setEndPage(page + 1);
      positions.getText(document);
    }
  }
}
