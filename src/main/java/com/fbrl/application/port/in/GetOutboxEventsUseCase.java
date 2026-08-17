package com.fbrl.application.port.in;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.OutboxEvent;

public interface GetOutboxEventsUseCase {
  PagedResult<OutboxEvent> getEvents(GetOutboxEventsQuery query);

  record GetOutboxEventsQuery(int page, int size) {}
}
