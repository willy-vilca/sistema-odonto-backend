package com.odontocare.finance.service;

import com.odontocare.audit.service.AuditService;
import com.odontocare.clinical.service.ClinicalAccess;
import com.odontocare.documents.repository.DocumentPolicyRepository;
import com.odontocare.documents.service.DocumentValidator;
import com.odontocare.finance.dto.PaymentContracts.*;
import com.odontocare.finance.model.*;
import com.odontocare.finance.repository.*;
import com.odontocare.installation.repository.*;
import com.odontocare.patients.repository.PatientRepository;
import com.odontocare.shared.pagination.*;
import com.odontocare.shared.web.ApiException;
import java.math.BigDecimal;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FinancialDocumentService {
  private final FinancialDocumentRepository documents;
  private final FinancialContentRepository contents;
  private final MoneyMovementRepository movements;
  private final ChargeEntryRepository charges;
  private final MoneyApplicationRepository applications;
  private final InstallationProfileRepository profiles;
  private final InstallationLogoRepository logos;
  private final PatientRepository patients;
  private final DocumentValidator validator;
  private final DocumentPolicyRepository policies;
  private final FinancialPdfService pdf;
  private final ClinicalAccess access;
  private final AuditService audit;
  private final FinanceOperationRepository operations;
  private final InstallmentService installments;

  public FinancialDocumentService(
      FinancialDocumentRepository documents,
      FinancialContentRepository contents,
      MoneyMovementRepository movements,
      ChargeEntryRepository charges,
      MoneyApplicationRepository applications,
      InstallationProfileRepository profiles,
      InstallationLogoRepository logos,
      PatientRepository patients,
      DocumentValidator validator,
      DocumentPolicyRepository policies,
      FinancialPdfService pdf,
      ClinicalAccess access,
      AuditService audit,
      FinanceOperationRepository operations,
      InstallmentService installments) {
    this.documents = documents;
    this.contents = contents;
    this.movements = movements;
    this.charges = charges;
    this.applications = applications;
    this.profiles = profiles;
    this.logos = logos;
    this.patients = patients;
    this.validator = validator;
    this.policies = policies;
    this.pdf = pdf;
    this.access = access;
    this.audit = audit;
    this.operations = operations;
    this.installments = installments;
  }

  public String nextCode() {
    var p = profiles.lockInstallation().orElseThrow();
    String code =
        p.getReceiptPrefix()
            + "-"
            + String.format(java.util.Locale.ROOT, "%06d", p.getReceiptNextNumber());
    if (documents.existsByFileNameAndGeneratedTrue(code + ".pdf"))
      throw ApiException.conflict(
          "El correlativo ya fue emitido. Configura otro prefijo o número siguiente de"
              + " constancia.");
    if (p.getReceiptNextNumber() == Integer.MAX_VALUE)
      throw ApiException.conflict("Configura un nuevo correlativo de constancias.");
    p.setReceiptNextNumber(p.getReceiptNextNumber() + 1);
    profiles.saveAndFlush(p);
    return code;
  }

  public void receipt(MoneyMovement m, List<Allocation> rows, String reason) {
    var patient = patients.findById(m.getPatientId()).orElseThrow();
    var lines = new ArrayList<String>();
    lines.add("Paciente: " + patient.getFullName() + " · " + patient.getCode());
    lines.add("Fecha: " + m.getOccurredOn() + " · Responsable: " + m.getActorName());
    lines.add("Movimiento: " + kind(m.getKind()) + " · Medio: " + method(m.getMethod()));
    lines.add("Importe: " + m.getCurrency() + " " + money(m.getAmount()));
    lines.add("Referencia: " + m.getReference());
    lines.add("Concepto: " + m.getDescription());
    lines.add("Motivo: " + reason);
    if (m.getOriginalId() != null)
      lines.add(
          "Pago original: " + movements.findById(m.getOriginalId()).orElseThrow().getReceiptCode());
    BigDecimal allocated = BigDecimal.ZERO;
    for (var row : rows) {
      lines.add(
          charges.findById(row.chargeId()).orElseThrow().getDescription()
              + " · "
              + money(row.amount()));
      allocated = allocated.add(row.amount());
    }
    if (m.getKind().equals("PAYMENT"))
      lines.add("Anticipo al emitir: " + money(m.getAmount().subtract(allocated)));
    store(
        m.getId(),
        m.getPatientId(),
        m.getReceiptCode() + ".pdf",
        "application/pdf",
        render("Constancia de " + kind(m.getKind()), m.getReceiptCode(), lines),
        "Constancia interna " + m.getReceiptCode(),
        true);
  }

  private String money(BigDecimal amount) {
    return amount.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
  }

  private String kind(String value) {
    return Map.of(
            "PAYMENT",
            "Abono",
            "REFUND",
            "Devolución",
            "REVERSAL",
            "Reversión de pago",
            "EXPENSE",
            "Egreso",
            "EXPENSE_REVERSAL",
            "Reversión de egreso")
        .get(value);
  }

  private String method(String value) {
    return Map.of(
            "CASH", "Efectivo", "TRANSFER", "Transferencia", "CARD", "Tarjeta", "OTHER", "Otro")
        .get(value);
  }

  private byte[] render(String title, String code, List<String> lines) {
    var identity = profiles.lockInstallation().orElseThrow();
    var logo = logos.findById((short) 1).map(l -> l.getContent()).orElse(null);
    return pdf.render(identity, logo, title, code, lines);
  }

  private FinancialDocument store(
      UUID movement,
      UUID patient,
      String name,
      String type,
      byte[] bytes,
      String description,
      boolean generated) {
    try {
      var d = new FinancialDocument();
      d.setMovementId(movement);
      d.setPatientId(patient);
      d.setFileName(name);
      d.setMediaType(type);
      d.setByteSize((long) bytes.length);
      d.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
      d.setDescription(description.strip());
      d.setActorName(access.actor().getDisplayName());
      d.setGenerated(generated);
      documents.saveAndFlush(d);
      var content = new FinancialContent();
      content.setId(d.getId());
      content.setContent(bytes);
      contents.saveAndFlush(content);
      return d;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public DocumentResponse upload(SupportRequest r, MultipartFile file) {
    var m = movements.findById(r.movementId()).orElseThrow(ApiException::notFound);
    String needed = m.getPatientId() == null ? "EXPENSES_WRITE" : "PAYMENTS_WRITE";
    if (!access.actor().getPermissions().contains(needed)) throw ApiException.forbidden();
    var validated =
        validator.validate(file, policies.findById((short) 1).orElseThrow().getMaxFileMiB());
    var d =
        store(
            m.getId(),
            m.getPatientId(),
            validated.name(),
            validated.mediaType(),
            validated.bytes(),
            r.description(),
            false);
    audit.record(
        "FINANCIAL_DOCUMENT_UPLOADED",
        "FINANCIAL_DOCUMENT",
        d.getId(),
        "Adjuntó sustento financiero");
    return response(d);
  }

  @Transactional
  public DocumentResponse statement(StatementRequest r) {
    patients.lockById(r.patientId()).orElseThrow(ApiException::notFound);
    String hash = fingerprint(r);
    var prior = operations.findByRequestKey(r.requestKey());
    if (prior.isPresent()) {
      if (!prior.get().getAction().equals("STATEMENT")
          || !prior.get().getFingerprint().equals(hash))
        throw ApiException.conflict("La clave ya corresponde a otra operación.");
      return response(documents.findById(prior.get().getResultId()).orElseThrow());
    }
    var patient = patients.findById(r.patientId()).orElseThrow();
    var lines = new ArrayList<String>();
    lines.add("Paciente: " + patient.getFullName() + " · " + patient.getCode());
    lines.add("Emitido: " + access.today() + " · Responsable: " + access.actor().getDisplayName());
    lines.add("Moneda: " + r.currency());
    var spec =
        SearchSpecifications.<ChargeEntry>text("")
            .and(SearchSpecifications.equal("patientId", r.patientId()))
            .and(SearchSpecifications.equal("currency", r.currency()));
    if (charges.count(spec) > 5000)
      throw ApiException.badRequest("El estado supera 5000 movimientos de cargos.");
    for (int page = 0; ; page++) {
      var result =
          charges.findAll(
              spec,
              org.springframework.data.domain.PageRequest.of(
                  page, 100, org.springframework.data.domain.Sort.by("createdAt", "id")));
      result.forEach(
          c ->
              lines.add(
                  Map.of(
                              "PLAN",
                              "Cargo de plan",
                              "SERVICE",
                              "Cargo por servicio",
                              "ADJUSTMENT",
                              "Ajuste",
                              "CANCELLATION",
                              "Cancelación")
                          .get(c.getKind())
                      + " · "
                      + c.getDescription()
                      + " · "
                      + money(c.getAmount())));
      if (!result.hasNext()) break;
    }
    var moneySpec =
        SearchSpecifications.<MoneyMovement>text("")
            .and(SearchSpecifications.equal("patientId", r.patientId()))
            .and(SearchSpecifications.equal("currency", r.currency()));
    if (movements.count(moneySpec) > 5000)
      throw ApiException.badRequest("El estado supera 5000 movimientos de dinero.");
    for (int page = 0; ; page++) {
      var result =
          movements.findAll(
              moneySpec,
              org.springframework.data.domain.PageRequest.of(
                  page, 100, org.springframework.data.domain.Sort.by("createdAt", "id")));
      result.forEach(
          m ->
              lines.add(
                  m.getOccurredOn()
                      + " · "
                      + kind(m.getKind())
                      + " · "
                      + m.getReceiptCode()
                      + " · "
                      + money(m.getAmount())));
      if (!result.hasNext()) break;
    }
    lines.add("");
    var applicationSpec =
        SearchSpecifications.<MoneyApplication>text("")
            .and(SearchSpecifications.equal("patientId", r.patientId()));
    if (applications.count(applicationSpec) > 5000)
      throw ApiException.badRequest("El estado supera 5000 aplicaciones.");
    lines.add("Aplicaciones y liberaciones:");
    for (int index = 0; ; index++) {
      var page =
          applications.findAll(
              applicationSpec,
              org.springframework.data.domain.PageRequest.of(
                  index, 100, org.springframework.data.domain.Sort.by("createdAt", "id")));
      for (var application : page) {
        var original = charges.findById(application.getChargeId()).orElseThrow();
        if (original.getCurrency().equals(r.currency()))
          lines.add(
              movements.findById(application.getPaymentId()).orElseThrow().getReceiptCode()
                  + " · "
                  + original.getDescription()
                  + " · "
                  + money(application.getAmount()));
      }
      if (!page.hasNext()) break;
    }
    lines.add("Cuotas y vencimientos vigentes:");
    for (int index = 0; index < 51; index++) {
      var q = new PageQuery();
      q.setPage(index);
      q.setSize(100);
      q.setSort("dueOn");
      var dues = installments.list(r.patientId(), null, true, null, null, q);
      dues.items().stream()
          .filter(i -> i.currency().equals(r.currency()))
          .forEach(
              i ->
                  lines.add(
                      "Cuota "
                          + i.position()
                          + " · "
                          + i.dueOn()
                          + " · Importe "
                          + money(i.amount())
                          + " · Pagado "
                          + money(i.paid())
                          + " · Pendiente "
                          + money(i.pending())));
      if (index + 1 >= dues.totalPages()) break;
      if (index == 50) throw ApiException.badRequest("El estado supera 5000 cuotas.");
    }
    var debt =
        charges
            .charges(r.patientId(), r.currency())
            .add(charges.adjustments(r.patientId(), r.currency()));
    var applied = applications.applied(r.patientId(), r.currency());
    var received = movements.received(r.patientId(), r.currency());
    lines.add("");
    lines.add("Deuda generada: " + money(debt));
    lines.add("Dinero recibido neto: " + money(received));
    lines.add("Aplicado a cargos: " + money(applied));
    lines.add("Saldo pendiente: " + money(debt.subtract(applied)));
    lines.add("Anticipo disponible: " + money(received.subtract(applied)));
    String code = nextCode();
    var d =
        store(
            null,
            r.patientId(),
            code + ".pdf",
            "application/pdf",
            render("Estado de cuenta", code, lines),
            "Estado de cuenta " + code,
            true);
    var op = new FinanceOperation();
    op.setRequestKey(r.requestKey());
    op.setAction("STATEMENT");
    op.setFingerprint(hash);
    op.setResultId(d.getId());
    op.setReason("Emitió estado de cuenta");
    op.setActorName(access.actor().getDisplayName());
    operations.saveAndFlush(op);
    audit.record(
        "STATEMENT_ISSUED", "FINANCIAL_DOCUMENT", d.getId(), "Emitió estado de cuenta histórico");
    return response(d);
  }

  public void cashReport(CashSession session) {
    String code = nextCode();
    var lines = new ArrayList<String>();
    lines.add("Apertura: " + session.getCreatedAt() + " · " + session.getOpenedBy());
    lines.add("Cierre: " + session.getClosedAt() + " · " + session.getClosedBy());
    lines.add("Moneda: " + session.getCurrency());
    lines.add("Fondo inicial: " + money(session.getOpening()));
    lines.add("Efectivo esperado: " + money(session.getExpected()));
    lines.add("Efectivo contado: " + money(session.getCounted()));
    lines.add("Diferencia: " + money(session.getDifference()));
    lines.add("Otros medios (neto): " + money(movements.nonCashNet(session.getId())));
    lines.add("Observaciones: " + session.getReason());
    lines.add("");
    var spec =
        SearchSpecifications.<MoneyMovement>text("")
            .and(SearchSpecifications.equal("cashSessionId", session.getId()));
    if (movements.count(spec) > 5000)
      throw ApiException.badRequest("El arqueo supera 5000 movimientos.");
    for (int page = 0; ; page++) {
      var result =
          movements.findAll(
              spec,
              org.springframework.data.domain.PageRequest.of(
                  page, 100, org.springframework.data.domain.Sort.by("createdAt", "id")));
      result.forEach(
          m ->
              lines.add(
                  m.getOccurredOn()
                      + " · "
                      + kind(m.getKind())
                      + " · "
                      + method(m.getMethod())
                      + " · "
                      + money(m.getAmount())
                      + " · "
                      + m.getDescription()));
      if (!result.hasNext()) break;
    }
    var bytes = render("Arqueo de caja", code, lines);
    storeCash(session.getId(), code, bytes);
  }

  private void storeCash(UUID cash, String code, byte[] bytes) {
    try {
      var d = new FinancialDocument();
      d.setCashSessionId(cash);
      d.setFileName(code + ".pdf");
      d.setMediaType("application/pdf");
      d.setByteSize((long) bytes.length);
      d.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
      d.setDescription("Arqueo de caja " + code);
      d.setActorName(access.actor().getDisplayName());
      d.setGenerated(true);
      documents.saveAndFlush(d);
      var content = new FinancialContent();
      content.setId(d.getId());
      content.setContent(bytes);
      contents.saveAndFlush(content);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public Download cashReport(UUID id) {
    var d = documents.findByCashSessionIdAndGeneratedTrue(id).orElseThrow(ApiException::notFound);
    audit.record("CASH_REPORT_READ", "CASH", id, "Descargó arqueo histórico");
    return new Download(
        d.getFileName(), d.getMediaType(), contents.findById(d.getId()).orElseThrow().getContent());
  }

  private String fingerprint(Object r) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(new tools.jackson.databind.ObjectMapper().writeValueAsBytes(r)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public PageResponse<DocumentResponse> list(
      UUID patient, UUID movement, Boolean generated, PageQuery query) {
    var spec =
        SearchSpecifications.<FinancialDocument>text(
                query.getSearch(), "fileName", "description", "actorName")
            .and(SearchSpecifications.equal("patientId", patient))
            .and(SearchSpecifications.equal("movementId", movement))
            .and(SearchSpecifications.equal("generated", generated));
    audit.record(
        "FINANCIAL_DOCUMENT_LIST", "FINANCIAL_DOCUMENT", patient, "Consultó metadatos financieros");
    return PageResponse.of(
        documents
            .findAll(
                spec,
                query.pageable(
                    Map.of("name", "fileName", "createdAt", "createdAt", "byteSize", "byteSize")))
            .map(this::response));
  }

  @Transactional
  public Download download(UUID id) {
    var d = documents.findById(id).orElseThrow(ApiException::notFound);
    audit.record(
        "FINANCIAL_DOCUMENT_READ", "FINANCIAL_DOCUMENT", id, "Recuperó documento financiero");
    return new Download(
        d.getFileName(), d.getMediaType(), contents.findById(id).orElseThrow().getContent());
  }

  @Transactional
  public DocumentResponse receipt(UUID id) {
    return response(
        documents.findByMovementIdAndGeneratedTrue(id).orElseThrow(ApiException::notFound));
  }

  private DocumentResponse response(FinancialDocument d) {
    return new DocumentResponse(
        d.getId(),
        d.getMovementId(),
        d.getPatientId(),
        d.getFileName(),
        d.getMediaType(),
        d.getByteSize(),
        d.getSha256(),
        d.getDescription(),
        d.getActorName(),
        d.getGenerated(),
        d.getCreatedAt());
  }
}
