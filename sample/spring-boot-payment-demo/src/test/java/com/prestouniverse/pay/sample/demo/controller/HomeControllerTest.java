package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.service.WebPayCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Renders the real {@code index.html} through Spring MVC + Thymeleaf (not just a Java-level unit test)
 * so that {@code th:field}/{@code th:object} binding, the payment method loop, and the category pills
 * are exercised the same way a browser request would; and exercises the one {@code /checkout} submit
 * endpoint's validation-error redisplay for both the hidden-methods and shown-methods paths.
 */
@WebMvcTest(controllers = HomeController.class)
@Import(HomeControllerTest.TestBeans.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebPayCheckoutService checkoutService;

    @Test
    void checkoutPageRendersTheSingleForm() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("MyStore")))
                .andExpect(content().string(containsString("id=\"showPaymentMethods\"")))
                .andExpect(content().string(containsString("action=\"/checkout\"")))
                .andExpect(content().string(containsString("Continue to Payment")))
                .andExpect(content().string(containsString("Choose Payment Method")))
                .andExpect(content().string(containsString("Credit / debit card")));
    }

    /**
     * The payment-method section must be server-side hidden (Tailwind's {@code hidden} class) to match
     * the toggle's default (off) state: a browser applies the class before any script runs, so relying on
     * JavaScript alone to reveal it would leave it invisible until the toggle's change handler fires.
     */
    @Test
    void methodSectionIsServerSideHiddenByDefault() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int tagStart = body.indexOf("id=\"methodSection\"");
        int tagEnd = body.indexOf('>', tagStart);
        String openingTag = body.substring(tagStart, tagEnd);
        assertThat(openingTag).contains("hidden");
    }

    @Test
    void invalidAmountRedisplaysIndexWithError() throws Exception {
        mockMvc.perform(post("/checkout")
                        .param("displayDesc", "Order")
                        .param("amountInRinggit", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Amount must be at least 0.01")));
    }

    @Test
    void missingPaymentMethodRedisplaysIndexWithErrorWhenMethodsShown() throws Exception {
        mockMvc.perform(post("/checkout")
                        .param("displayDesc", "Order")
                        .param("amountInRinggit", "10.00")
                        .param("showPaymentMethods", "true")
                        .param("selectedPaymentMethod", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Select a payment method")));
    }

    @Test
    void hiddenMethodsSubmissionDoesNotRequireASelectedMethod() throws Exception {
        mockMvc.perform(post("/checkout")
                        .param("displayDesc", "Order")
                        .param("amountInRinggit", "0"))
                .andExpect(content().string(not(containsString("Select a payment method"))));
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        PaymentActivityStore paymentActivityStore() {
            return new PaymentActivityStore();
        }
    }
}
