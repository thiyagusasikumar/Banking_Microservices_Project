package com.banking.accountservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class AccountServiceApplicationTests {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AccountService accountService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS account_number_seq START WITH 1000000000");
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Long createAccount() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setCustomerId(1L);
        request.setAccountType(AccountType.SAVINGS);
        request.setCurrency("INR");
        return accountService.createAccount(request).getId();
    }

    @Test
    void createReturns201AndLocation() throws Exception {
        mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"accountType\":\"SAVINGS\",\"currency\":\"INR\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.accountStatus").value("PENDING_ACTIVATION"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void readsAndUpdateReturn200() throws Exception {
        Long id = createAccount();
        String number = accountService.getAccountById(id).getAccountNumber();
        mvc.perform(get("/api/v1/accounts/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts/by-number/{number}", number)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts/customer/1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts/me").param("customerId", "1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts/{id}/balance", id)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/accounts/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountType\":\"CURRENT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountType").value("CURRENT"));
    }

    @Test
    void lifecycleReturns200AndPersistsHistory() throws Exception {
        Long id = createAccount();
        String[] actions = {"activate", "freeze", "unfreeze", "close"};
        String[] states = {"ACTIVE", "FROZEN", "ACTIVE", "CLOSED"};
        for (int i = 0; i < actions.length; i++) {
            mvc.perform(post("/api/v1/accounts/{id}/{action}", id, actions[i])
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Customer request\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.accountStatus").value(states[i]));
        }
        mvc.perform(get("/api/v1/accounts/{id}/status-history", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(patch("/api/v1/accounts/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountType\":\"CURRENT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void missingAccountReturns404() throws Exception {
        mvc.perform(get("/api/v1/accounts/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"customerId\":-1,\"accountType\":\"SAVINGS\",\"currency\":\"INR\"}",
            "{\"customerId\":1,\"accountType\":\"SAVINGS\",\"currency\":\"USD\"}", "{bad-json"})
    void invalidCreationReturns400(String body) throws Exception {
        mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidUpdateAndLifecycleReasonReturn400() throws Exception {
        Long id = createAccount();
        mvc.perform(patch("/api/v1/accounts/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/accounts/{id}/activate", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/accounts/{id}/activate", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTransitionReturns409WithoutHistory() throws Exception {
        Long id = createAccount();
        mvc.perform(post("/api/v1/accounts/{id}/freeze", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Customer request\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/accounts/{id}/status-history", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nonzeroBalancePreventsClosure() throws Exception {
        Long id = createAccount();
        mvc.perform(post("/api/v1/accounts/{id}/activate", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Approved\"}")).andExpect(status().isOk());
        entityManager.flush();
        jdbc.update("UPDATE accounts SET balance = 10.00 WHERE id = ?", id);
        // Clear the persistence context so the HTTP request reads the changed balance.
        entityManager.clear();
        mvc.perform(post("/api/v1/accounts/{id}/close", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Customer request\"}"))
                .andExpect(status().isConflict());
    }

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void unsupportedRequestsHaveCorrectStatuses() throws Exception {
        mvc.perform(delete("/api/v1/accounts/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/v1/accounts").contentType(MediaType.TEXT_PLAIN).content("test"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/v1/accounts").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        mvc.perform(get("/api/v1/accounts").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/accounts").param("status", "INVALID")).andExpect(status().isBadRequest());
    }


	@Test
	void contextLoads() {
	}

}
