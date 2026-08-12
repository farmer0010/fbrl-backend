package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.out.SaveEodSnapshotPort;
import com.fbrl.domain.model.EodSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class EodSnapshotItemWriter implements ItemWriter<EodSnapshot> {

  private final SaveEodSnapshotPort saveEodSnapshotPort;

  public EodSnapshotItemWriter(SaveEodSnapshotPort saveEodSnapshotPort) {
    this.saveEodSnapshotPort = saveEodSnapshotPort;
  }

  @Override
  public void write(Chunk<? extends EodSnapshot> chunk) {
    // 스프링 batch 5.x 부터 변경된건데 인터페이스 메서드 형식이 청크라
    // 우리는 리스트에 청크를 담아 변환하는 형태로 오버라이딩 진행함
    List<EodSnapshot> snapshots = new ArrayList<>(chunk.getItems());
    saveEodSnapshotPort.saveAll(snapshots);
  }
}
