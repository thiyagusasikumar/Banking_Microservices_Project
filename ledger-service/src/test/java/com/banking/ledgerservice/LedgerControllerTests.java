package com.banking.ledgerservice;

import com.banking.ledgerservice.controller.LedgerController;
import com.banking.ledgerservice.exception.LedgerEntryNotFoundException;
import com.banking.ledgerservice.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerControllerTests {
    @Test
    void missingEntryReturns404() throws Exception {
        LedgerService service = mock(LedgerService.class);
        when(service.getEntryById(123L)).thenThrow(new LedgerEntryNotFoundException(123L));
        MockMvcBuilders.standaloneSetup(new LedgerController(service)).build()
                .perform(get("/ledger/entries/123"))
                .andExpect(status().isNotFound());
    }
}
