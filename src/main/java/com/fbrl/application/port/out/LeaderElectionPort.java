package com.fbrl.application.port.out;

import java.util.function.Consumer;

public interface LeaderElectionPort {
  void participateInElection(
      Runnable onStartLeading, Runnable onStopLeading, Consumer<String> onNewLeader);
}
