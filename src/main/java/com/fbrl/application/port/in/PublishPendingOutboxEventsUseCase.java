package com.fbrl.application.port.in;

public interface PublishPendingOutboxEventsUseCase {
  PublishResult publishPendingEvents(int limit);

  record PublishResult(int publishedCount, int failedCount) {

    public static PublishResult empty() {
      return new PublishResult(0, 0);
    }
  }
}
