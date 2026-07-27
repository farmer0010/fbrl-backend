package com.fbrl.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.in.web.dto.TransferRequest;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

public class DummyTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();
  private TransferMoneyUseCase transferMoneyUseCase;

  @BeforeEach
  void setUp() {
    transferMoneyUseCase = Mockito.mock(TransferMoneyUseCase.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new TransferController(transferMoneyUseCase)).build();
  }

  @Test
  void transferSuccess() throws Exception {
    TransferRequest request = new TransferRequest("111-111", "222-222", BigDecimal.valueOf(10000));
    willDoNothing().given(transferMoneyUseCase).transfer(any());
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}
