package com.fbrl.application.port.out;

import com.fbrl.domain.model.OutboxEvent;

public interface LoadOutboxEventsPort {
  PagedResult<OutboxEvent> loadPage(int page, int size);
}
