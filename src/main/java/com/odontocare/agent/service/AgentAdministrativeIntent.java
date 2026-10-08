package com.odontocare.agent.service;

public final class AgentAdministrativeIntent {
  private AgentAdministrativeIntent() {}

  public static boolean clinical(String body) {
    String text = AgentIdentityService.normalize(body);
    return text.matches(
        "(?s).*(historial"
            + " clinico|diagnostic|recet|medicamento|dolor|sangrado|hinchazon|urgencia|cuanto"
            + " debo|saldo|deuda|descuento|reembolso).*");
  }

  public static boolean human(String body) {
    return AgentIdentityService.normalize(body)
        .matches(
            "(?s).*(hablar con (?:una persona|recepcion|el profesional|un humano)|quiero (?:un"
                + " humano|hacer un reclamo)|reclam|queja).*");
  }

  public static boolean changed(String body) {
    return AgentIdentityService.normalize(body)
        .matches(
            "(?s).*(cambie de opinion|ahora prefiero|mejor (?:para|cancel)|ya no quiero|otra"
                + " persona|otro paciente).*");
  }
}
