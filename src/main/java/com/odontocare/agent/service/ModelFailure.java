package com.odontocare.agent.service;

public class ModelFailure extends RuntimeException {
  private final String code;
  private final long retryAfterSeconds;

  public ModelFailure(String code, String detail) {
    this(code, detail, 30);
  }

  public ModelFailure(String code, String detail, long retryAfterSeconds) {
    super(detail);
    this.code = code;
    this.retryAfterSeconds = Math.max(1, Math.min(86400, retryAfterSeconds));
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }

  public String code() {
    return code;
  }
}
