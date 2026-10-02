package com.odontocare.finance.service;

import com.odontocare.installation.model.InstallationProfile;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.springframework.stereotype.Service;

@Service
public class FinancialPdfService {
  public byte[] render(
      InstallationProfile identity, byte[] logo, String title, String code, List<String> lines) {
    try (var pdf = new PDDocument();
        var fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf");
        var output = new ByteArrayOutputStream()) {
      var font = PDType0Font.load(pdf, fontStream);
      var rows = new ArrayList<String>();
      rows.add(identity.getDisplayName());
      if (!identity.getLegalName().isBlank()) rows.add(identity.getLegalName());
      rows.add(identity.getAddress());
      rows.add(identity.getPhone() + "  " + identity.getEmail());
      rows.add(identity.getDocumentHeader());
      rows.add("");
      rows.add(title + " · " + code);
      rows.add("");
      rows.addAll(lines);
      rows.add("");
      rows.add(identity.getDocumentFooter());
      rows.add("Constancia interna · Sin facturación electrónica.");
      PDPageContentStream stream = null;
      float y = 0;
      int number = 0;
      try {
        for (String value : rows) {
          for (String line : wrap(safe(font, value), font)) {
            if (y < 65) {
              if (stream != null) stream.close();
              var page = new PDPage(PDRectangle.A4);
              pdf.addPage(page);
              stream = new PDPageContentStream(pdf, page);
              y = 750;
              number++;
              stream.setStrokingColor(0.13f, 0.37f, 0.30f);
              stream.moveTo(50, 765);
              stream.lineTo(545, 765);
              stream.stroke();
              text(stream, font, "OdontoCare · Página " + number, 35);
              if (number == 1 && logo != null) {
                var image = PDImageXObject.createFromByteArray(pdf, logo, "logo");
                float width = Math.min(62, 40f * image.getWidth() / image.getHeight());
                float height = width * image.getHeight() / image.getWidth();
                stream.drawImage(image, 545 - width, 785, width, height);
              }
            }
            text(stream, font, line, y);
            y -= 17;
          }
        }
      } finally {
        if (stream != null) stream.close();
      }
      pdf.save(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo generar la constancia interna.", e);
    }
  }

  private String safe(PDType0Font f, String value) {
    var result = new StringBuilder();
    value
        .codePoints()
        .forEach(
            cp -> {
              if (Character.isISOControl(cp)) {
                result.append(' ');
                return;
              }
              try {
                f.encode(new String(Character.toChars(cp)));
                result.appendCodePoint(cp);
              } catch (IllegalArgumentException | IOException e) {
                result.append("?");
              }
            });
    return result.toString();
  }

  private List<String> wrap(String value, PDType0Font font) throws IOException {
    var lines = new ArrayList<String>();
    StringBuilder line = new StringBuilder();
    for (int cp : value.codePoints().toArray()) {
      String character = new String(Character.toChars(cp));
      if (font.getStringWidth(line.toString() + character) / 1000 * 10 > 490) {
        int space = line.lastIndexOf(" ");
        if (space > 0) {
          lines.add(line.substring(0, space));
          line.delete(0, space + 1);
        } else {
          lines.add(line.toString());
          line.setLength(0);
        }
      }
      line.append(character);
    }
    lines.add(line.toString());
    return lines;
  }

  private void text(PDPageContentStream s, PDType0Font f, String value, float y)
      throws IOException {
    s.beginText();
    s.setFont(f, 10);
    s.newLineAtOffset(50, y);
    s.showText(value);
    s.endText();
  }
}
