package com.odontocare.agent.config;

import com.odontocare.agent.service.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentWorker {
  private final AgentProperties config;
  private final AgentQueueService queue;
  private final BookingAgent agent;

  public AgentWorker(AgentProperties config, AgentQueueService queue, BookingAgent agent) {
    this.config = config;
    this.queue = queue;
    this.agent = agent;
  }

  @Scheduled(fixedDelay = 4000, initialDelay = 8000)
  public void processPending() {
    if (!config.ready() || !config.isWorkerEnabled()) return;
    try {
      queue.claim().ifPresent(agent::process);
    } catch (RuntimeException failure) {
      org.slf4j.LoggerFactory.getLogger(AgentWorker.class)
          .warn("Agent task unavailable: {}", failure.getClass().getSimpleName());
    }
  }
}
