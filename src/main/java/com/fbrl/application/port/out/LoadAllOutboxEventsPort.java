package com.fbrl.application.port.out;

import com.fbrl.domain.model.OutboxEvent;
import java.util.List;

public interface LoadAllOutboxEventsPort {
  List<OutboxEvent> loadAllOrderedById();
}
