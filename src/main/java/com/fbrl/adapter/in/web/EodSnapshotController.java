package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.EodSnapshotResponse;
import com.fbrl.adapter.in.web.dto.PageResponse;
import com.fbrl.application.port.in.GetEodSnapshotsByDateUseCase;
import com.fbrl.application.port.in.GetEodSnapshotsByDateUseCase.GetEodSnapshotsByDateQuery;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.EodSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eod-snapshots")
public class EodSnapshotController {

  private final GetEodSnapshotsByDateUseCase getEodSnapshotsByDateUseCase;

  public EodSnapshotController(GetEodSnapshotsByDateUseCase getEodSnapshotsByDateUseCase) {
    this.getEodSnapshotsByDateUseCase = getEodSnapshotsByDateUseCase;
  }

  @GetMapping
  public ResponseEntity<PageResponse<EodSnapshotResponse>> getByDate(
      @RequestParam LocalDate date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PagedResult<EodSnapshot> result =
        getEodSnapshotsByDateUseCase.getByDate(new GetEodSnapshotsByDateQuery(date, page, size));
    List<EodSnapshotResponse> content =
        result.items().stream().map(EodSnapshotResponse::from).toList();
    return ResponseEntity.ok(PageResponse.of(content, result.totalElements(), page, size));
  }
}
