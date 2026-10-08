package com.odontocare.agent.service;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AgentToolDefinitions {
  public List<Map<String, Object>> all() {
    return List.of(
        tool(
            "verificar_paciente",
            "Verifica el nombre completo expresamente informado y la relación del contacto"
                + " autenticado. SELF=para mí; GUARDIAN=mi hijo y soy su responsable. No adivines"
                + " nombres ni relaciones.",
            Map.of(
                "patient_name",
                text("Nombre completo informado"),
                "relationship",
                text("SELF o GUARDIAN")),
            List.of("patient_name", "relationship")),
        tool(
            "consultar_mis_citas",
            "Consulta próximas citas solamente del paciente verificado. Devuelve appointment_ref"
                + " temporal; no acepta teléfonos ni otros pacientes.",
            Map.of(
                "search",
                text("Buscar por servicio o profesional"),
                "page",
                integer("Página desde 0")),
            List.of("search")),
        tool(
            "proponer_reprogramacion",
            "Prepara un cambio solicitado expresamente. No cambia la cita hasta confirmar el"
                + " resumen. Consultar_mis_citas y consultar_horarios antes.",
            Map.of(
                "appointment_ref",
                text("Referencia temporal de consultar_mis_citas"),
                "slot_id",
                text("Horario devuelto para esta reprogramación"),
                "reason",
                text("Motivo informado del cambio")),
            List.of("appointment_ref", "slot_id", "reason")),
        tool(
            "proponer_cancelacion",
            "Prepara cancelación expresamente solicitada de una cita propia verificada; requiere"
                + " confirmación posterior.",
            Map.of(
                "appointment_ref",
                text("Referencia temporal de consultar_mis_citas"),
                "reason",
                text("Motivo informado de cancelación")),
            List.of("appointment_ref", "reason")),
        tool(
            "derivar_recepcion",
            "Pausa el agente y deriva a recepción solicitudes clínicas, reclamos, identidad dudosa"
                + " o casos no resolubles. No diagnostiques.",
            Map.of("reason", text("Motivo administrativo sin diagnósticos ni datos sensibles")),
            List.of("reason")),
        tool(
            "consultar_servicios",
            "Busca servicios activos reservables y sus precios. Usa una palabra corta como"
                + " limpieza; páginas de 5.",
            Map.of("search", text("Texto de búsqueda"), "page", integer("Página desde 0")),
            List.of("search")),
        tool(
            "pacientes_contacto",
            "Busca fichas activas vinculadas al teléfono autenticado, sin historia clínica. Pide al"
                + " usuario para quién es la cita; no asumas su identidad.",
            Map.of("search", text("Nombre para buscar o vacío"), "page", integer("Página desde 0")),
            List.of("search")),
        tool(
            "consultar_horarios",
            "Consulta horarios reales de un servicio. Devuelve referencias slot_id que puedes"
                + " proponer. Para mañana usa days_from_today=1. Si la hora está ocupada devuelve"
                + " alternativas.",
            Map.of(
                "service_id",
                text("ID del servicio obtenido del catálogo"),
                "dentist_id",
                text("ID opcional del profesional; omitir para consultar disponibles"),
                "dentist_name",
                text(
                    "Nombre solicitado, por ejemplo Julia Huaytalla. Usa este campo si no conoces"
                        + " su ID; nunca pongas un nombre en dentist_id."),
                "date",
                text("Fecha absoluta YYYY-MM-DD, omitir si es relativa"),
                "days_from_today",
                integer("Días desde hoy: hoy=0, mañana=1, pasado mañana=2"),
                "preferred_time",
                text("Hora HH:mm opcional"),
                "appointment_ref",
                text(
                    "Referencia de consultar_mis_citas para reprogramación; conserva duración y"
                        + " excluye solo esa cita")),
            List.of("service_id")),
        tool(
            "proponer_cita",
            "Prepara resumen y código de confirmación para un horario consultado. NO reserva."
                + " Requiere paciente identificado o nombre completo informado para ficha"
                + " provisional.",
            Map.of(
                "slot_id",
                text("Referencia recibida en consultar_horarios"),
                "patient_id",
                text("ID del paciente del contacto, omitir para paciente nuevo"),
                "patient_name",
                text("Nombre completo expresamente indicado por el usuario")),
            List.of("slot_id", "patient_name")),
        tool(
            "descartar_propuesta",
            "Descarta la propuesta pendiente si el usuario niega la reserva o cambia los datos. No"
                + " cancela citas ya creadas.",
            Map.of(),
            List.of()));
  }

  private Map<String, Object> text(String description) {
    return Map.of("type", "string", "description", description);
  }

  private Map<String, Object> integer(String description) {
    return Map.of("type", "integer", "description", description);
  }

  private Map<String, Object> tool(
      String name, String description, Map<String, Object> properties, List<String> required) {
    return Map.of(
        "type",
        "function",
        "function",
        Map.of(
            "name",
            name,
            "description",
            description,
            "parameters",
            Map.of(
                "type",
                "object",
                "properties",
                properties,
                "required",
                required,
                "additionalProperties",
                false)));
  }
}
