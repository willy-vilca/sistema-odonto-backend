package com.odontocare.agent.service;

public class ModelFailure extends RuntimeException {
  private final String code;

  public ModelFailure(String code, String detail) {
    super(detail);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
