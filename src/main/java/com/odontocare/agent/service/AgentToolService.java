package com.odontocare.agent.service;

import com.odontocare.agent.dto.AgentContracts.*;
import com.odontocare.agent.model.AgentRun;
import com.odontocare.agent.repository.*;
import com.odontocare.agent.repository.AgentInboxRepository;
import com.odontocare.appointments.service.AvailabilityService;
import com.odontocare.audit.service.AuditService;
import com.odontocare.installation.repository.InstallationProfileRepository;
import com.odontocare.shared.pagination.PageQuery;
import com.odontocare.shared.web.ApiException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class AgentToolService {
  private final AgentRepository runs;
  private final AgentCatalogRepository catalog;
  private final AgentInboxRepository conversations;
  private final InstallationProfileRepository profiles;
  private final AvailabilityService availability;
  private final AuditService audit;
  private final Clock clock;

  public AgentToolService(
      AgentRepository runs,
      AgentCatalogRepository catalog,
      AgentInboxRepository conversations,
      InstallationProfileRepository profiles,
      AvailabilityService availability,
      AuditService audit,
      Clock clock) {
    this.runs = runs;
    this.catalog = catalog;
    this.conversations = conversations;
    this.profiles = profiles;
    this.availability = availability;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(timeout = 10)
  public Object execute(AgentRun run, String name, JsonNode args) {
    if (!args.isObject())
      throw ApiException.badRequest("Los argumentos de la herramienta deben ser un objeto.");
    return switch (name) {
      case "consultar_servicios" -> {
        fields(args, "search", "page");
        yield Map.of(
            "items",
            catalog.services(text(args, "search", 160, false), page(args)),
            "page",
            page(args),
            "page_size",
            5,
            "currency",
            profiles.findById((short) 1).orElseThrow().getCurrency());
      }
      case "pacientes_contacto" -> {
        fields(args, "search", "page");
        String phone =
            conversations.conversation(run.conversationId(), false).orElseThrow().phone();
        yield Map.of(
            "items",
            catalog.patients(phone, text(args, "search", 160, false), page(args)),
            "page",
            page(args),
            "page_size",
            5);
      }
      case "consultar_horarios" -> {
        fields(
            args,
            "service_id",
            "dentist_id",
            "dentist_name",
            "date",
            "days_from_today",
            "preferred_time");
        yield slots(run, args);
      }
      case "proponer_cita" -> {
        fields(args, "slot_id", "patient_id", "patient_name");
        yield propose(run, args);
      }
      case "descartar_propuesta" -> {
        fields(args);
        conversations.conversation(run.conversationId(), true).orElseThrow();
        runs.currentProposal(
                run.conversationId(),
                conversations.message(run.messageId(), false).orElseThrow().source())
            .filter(p -> p.state().equals("PENDING"))
            .ifPresent(p -> runs.proposalState(p.id(), "SUPERSEDED"));
        yield Map.of("discarded", true, "appointment_created", false);
      }
      default -> throw ApiException.forbidden();
    };
  }

  @Transactional(timeout = 10)
  public String conflictAlternatives(AgentRun run) {
    conversations.conversation(run.conversationId(), true).orElseThrow();
    var proposal =
        runs.currentProposal(
                run.conversationId(),
                conversations.message(run.messageId(), false).orElseThrow().source())
            .orElseThrow();
    var previous = runs.slot(proposal.slotId()).orElseThrow();
    runs.proposalState(proposal.id(), "CONFLICT");
    var args = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    args.put("service_id", previous.serviceId().toString());
    args.put("dentist_id", previous.dentistId().toString());
    args.put("date", previous.localStart().toLocalDate().toString());
    @SuppressWarnings("unchecked")
    var result = (Map<String, Object>) slots(run, args);
    runs.step(run.id(), "TOOL", "consultar_horarios", args, result, "OK", clock.instant());
    @SuppressWarnings("unchecked")
    var offered = (List<Map<String, Object>>) result.get("items");
    if (offered.isEmpty())
      return "Ese horario ya no está disponible. No se creó la cita. ¿Qué otra fecha prefieres para"
          + " "
          + previous.serviceName()
          + "?";
    String choices =
        offered.stream()
            .map(x -> LocalDateTime.parse(x.get("local_start").toString()).toLocalTime().toString())
            .distinct()
            .limit(3)
            .reduce((a, b) -> a + ", " + b)
            .orElseThrow();
    return "Ese horario acaba de ocuparse; no se creó otra cita. Para "
        + previous.serviceName()
        + " con "
        + previous.dentistName()
        + " el "
        + previous.localStart().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        + " puedo ofrecerte "
        + choices
        + ". ¿Cuál prefieres?";
  }

  private Object slots(AgentRun run, JsonNode args) {
    UUID service = uuid(args, "service_id", true), dentist = uuid(args, "dentist_id", false);
    var serviceData =
        catalog
            .service(service)
            .orElseThrow(
                () -> ApiException.badRequest("El servicio no está habilitado para el agente."));
    var profile = profiles.findById((short) 1).orElseThrow();
    ZoneId zone = ZoneId.of(profile.getTimeZone());
    var incoming = conversations.message(run.messageId(), false).orElseThrow();
    var receivedDate = incoming.createdAt().atZone(zone).toLocalDate();
    LocalDate today = LocalDate.now(clock.withZone(zone)), date;
    if (args.hasNonNull("days_from_today")) {
      if (args.hasNonNull("date") && !args.path("date").asString("").isBlank())
        throw ApiException.badRequest("Utiliza fecha absoluta o relativa, no ambas.");
      int days = number(args, "days_from_today", 0, 60);
      date = receivedDate.plusDays(days);
    } else {
      try {
        date = LocalDate.parse(text(args, "date", 10, true));
      } catch (DateTimeException failure) {
        throw ApiException.badRequest("La fecha debe ser YYYY-MM-DD.");
      }
    }
    date = AgentRequestedDate.resolve(incoming.body(), receivedDate).orElse(date);
    if (date.isBefore(today) || date.isAfter(today.plusDays(60)))
      throw ApiException.badRequest("Consulta una fecha entre hoy y los próximos 60 días.");
    String preferred = text(args, "preferred_time", 5, false);
    if (!preferred.isBlank())
      try {
        LocalTime.parse(preferred);
      } catch (DateTimeException failure) {
        throw ApiException.badRequest("La hora debe ser HH:mm.");
      }
    var dentists = catalog.dentists(service, dentist, text(args, "dentist_name", 160, false));
    var results = new ArrayList<Map<String, Object>>();
    boolean preferredFound = false;
    for (var professional : dentists) {
      UUID dentistId = (UUID) professional.get("id");
      var query = new PageQuery();
      query.setSize(3);
      query.setSearch(preferred);
      var available = availability.slots(dentistId, service, null, date, null, query);
      boolean exact = !available.items().isEmpty() && !preferred.isBlank();
      preferredFound |= exact;
      if (available.items().isEmpty() && !preferred.isBlank()) {
        query.setSearch("");
        available = availability.slots(dentistId, service, null, date, null, query);
      }
      for (var slot : available.items()) {
        var offered =
            runs.addSlot(
                run.id(),
                run.conversationId(),
                dentistId,
                service,
                slot.localStart(),
                ((Number) serviceData.get("duration_minutes")).intValue(),
                zone.getId(),
                clock.instant().plusSeconds(1800),
                serviceData.get("name").toString(),
                professional.get("full_name").toString());
        results.add(
            Map.of(
                "slot_id",
                offered.id(),
                "dentist_id",
                dentistId,
                "dentist_name",
                professional.get("full_name"),
                "service_name",
                serviceData.get("name"),
                "local_start",
                slot.localStart().toString(),
                "local_end",
                slot.localEnd().toString(),
                "duration_minutes",
                offered.durationMinutes(),
                "matches_preference",
                exact || preferred.isBlank()));
      }
    }
    return Map.of(
        "date",
        date.toString(),
        "time_zone",
        zone.getId(),
        "preferred_time_available",
        preferredFound,
        "items",
        results,
        "message",
        results.isEmpty()
            ? "No hay horarios disponibles ni profesionales habilitados en la fecha consultada."
            : "Solo estos horarios fueron calculados por la agenda; no están retenidos.");
  }

  private Object propose(AgentRun run, JsonNode args) {
    conversations.conversation(run.conversationId(), true).orElseThrow();
    if (!Objects.equals(runs.latestInbound(run.conversationId(), run.messageId()), run.messageId()))
      throw ApiException.conflict(
          "Llegó otro mensaje; procesa la solicitud más reciente antes de proponer.");
    var slot = runs.slot(uuid(args, "slot_id", true)).orElseThrow(ApiException::notFound);
    var inputSource = conversations.message(run.messageId(), false).orElseThrow().source();
    var slotSource =
        conversations
            .message(runs.get(slot.runId(), false).orElseThrow().messageId(), false)
            .orElseThrow()
            .source();
    if (!inputSource.equals(slotSource)) throw ApiException.forbidden();
    if (!slot.conversationId().equals(run.conversationId())
        || !slot.expiresAt().isAfter(clock.instant()))
      throw ApiException.badRequest("Ese horario ofrecido venció o no pertenece al contacto.");
    String phone = conversations.conversation(run.conversationId(), false).orElseThrow().phone();
    UUID patient = uuid(args, "patient_id", false);
    String patientName = text(args, "patient_name", 160, true);
    if (patient != null) {
      var identified = catalog.patient(phone, patient).orElseThrow(ApiException::forbidden);
      patientName = identified.get("full_name").toString();
    }
    if (patientName.length() < 3)
      throw ApiException.badRequest("Solicita el nombre completo del paciente.");
    var service =
        catalog
            .service(slot.serviceId())
            .orElseThrow(
                () -> ApiException.badRequest("El servicio ya no permite reservas del agente."));
    var dentist = catalog.dentists(slot.serviceId(), slot.dentistId());
    if (dentist.isEmpty()) throw ApiException.conflict("El profesional ya no está habilitado.");
    var profile = profiles.findById((short) 1).orElseThrow();
    if (!profile.getTimeZone().equals(slot.timeZone())
        || ((Number) service.get("duration_minutes")).intValue() != slot.durationMinutes())
      throw ApiException.conflict("Cambió la zona o duración; consulta y confirma otro horario.");
    var incoming = conversations.message(run.messageId(), false).orElseThrow();
    var requestedDate =
        AgentRequestedDate.resolve(
            incoming.body(),
            incoming.createdAt().atZone(ZoneId.of(profile.getTimeZone())).toLocalDate());
    if (requestedDate.isPresent() && !requestedDate.get().equals(slot.localStart().toLocalDate()))
      throw ApiException.badRequest(
          "Ese horario no coincide con el día solicitado; consulta la fecha correcta.");
    availability.requireAvailable(
        slot.dentistId(),
        slot.localStart().atZone(ZoneId.of(slot.timeZone())).toInstant(),
        slot.durationMinutes(),
        profile,
        null);
    var prior = runs.proposalByRun(run.id());
    if (prior.isPresent()) {
      var p = prior.get();
      if (!p.slotId().equals(slot.id())
          || !Objects.equals(p.patientId(), patient)
          || !p.patientName().equals(patientName))
        throw ApiException.conflict(
            "Solo puede prepararse una propuesta por mensaje; solicita un mensaje nuevo para"
                + " cambiarla.");
      return proposalResult(p);
    }
    String summary =
        "Paciente: "
            + patientName
            + ". Servicio: "
            + service.get("name")
            + ". Odontólogo: "
            + dentist.getFirst().get("full_name")
            + ". Fecha y hora: "
            + slot.localStart()
                .format(
                    DateTimeFormatter.ofPattern(
                        "EEEE dd/MM/yyyy HH:mm", Locale.forLanguageTag("es")))
            + " ("
            + slot.timeZone()
            + "). Duración: "
            + slot.durationMinutes()
            + " minutos.";
    var p =
        runs.propose(
            run.id(),
            run.conversationId(),
            slot.id(),
            patient,
            patientName,
            summary,
            clock.instant());
    audit.recordAs(
        null,
        "Agente IA",
        "AGENT_PROPOSAL",
        "AGENT_PROPOSAL",
        p.id(),
        "Preparó una propuesta pendiente de confirmación.");
    return proposalResult(p);
  }

  public static Map<String, Object> proposalResult(Proposal p) {
    return Map.of(
        "proposal_id",
        p.id(),
        "summary",
        p.summary(),
        "confirmation_message",
        "CONFIRMO " + p.confirmationCode(),
        "expires_at",
        p.expiresAt().toString(),
        "appointment_created",
        false);
  }

  private void fields(JsonNode args, String... allowed) {
    var set = Set.of(allowed);
    args.propertyNames()
        .forEach(
            field -> {
              if (!set.contains(field))
                throw ApiException.badRequest("La herramienta contiene un argumento no permitido.");
            });
  }

  private int page(JsonNode args) {
    return args.hasNonNull("page") ? number(args, "page", 0, 100) : 0;
  }

  private int number(JsonNode args, String field, int min, int max) {
    var value = args.path(field);
    if (!value.isIntegralNumber() || value.asInt() < min || value.asInt() > max)
      throw ApiException.badRequest("Valor numérico inválido: " + field);
    return value.asInt();
  }

  private String text(JsonNode args, String field, int max, boolean required) {
    var value = args.path(field);
    if (value.isMissingNode() || value.isNull()) {
      if (required) throw ApiException.badRequest("Falta " + field);
      return "";
    }
    if (!value.isString()) throw ApiException.badRequest("El campo " + field + " debe ser texto.");
    String text = value.asString().strip();
    if (text.length() > max || required && text.isBlank())
      throw ApiException.badRequest("Valor inválido: " + field);
    return text;
  }

  private UUID uuid(JsonNode args, String field, boolean required) {
    String text = text(args, field, 36, required);
    if (text.isBlank()) return null;
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException failure) {
      throw ApiException.badRequest("Referencia inválida: " + field);
    }
  }
}
