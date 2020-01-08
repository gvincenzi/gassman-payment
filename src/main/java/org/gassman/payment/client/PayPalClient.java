package org.gassman.payment.client;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.gassman.payment.dto.OrderDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayPalClient {
    @Value("${paypal.clientId}")
    private String clientId;
    @Value("${paypal.clientSecret}")
    private String clientSecret;
    @Value("${paypal.active}")
    private Boolean active;


    public Map<String, Object> createPayment(OrderDTO orderDTO) throws PayPalRESTException {
        checkActive();

        Map<String, Object> response = new HashMap();
        Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setTotal(orderDTO.computeTotalToPay() != null ? orderDTO.computeTotalToPay().toString() : BigDecimal.ZERO.toString());
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(String.format("GasSMan Payment for Order #%d", orderDTO.getOrderId()));
        transaction.setCustom(orderDTO.getOrderId().toString());
        List<Transaction> transactions = new ArrayList();
        transactions.add(transaction);

        Item item = new Item();
        item.setCurrency("EUR");
        item.setDescription(orderDTO.getProduct().getDescription());
        item.setName(orderDTO.getProduct().getName());
        item.setQuantity(orderDTO.getQuantity().toString());
        item.setPrice(orderDTO.getProduct().getPricePerUnit().toString());

        ItemList itemList = new ItemList();
        itemList.setItems(new ArrayList<>());
        itemList.getItems().add(item);

        transaction.setItemList(itemList);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        payment.setNoteToPayer("Delivery : " + LocalDateTime.parse(orderDTO.getProduct().getDeliveryDateTime()).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl("http://localhost:8881/gassman-payment-service/paypal/cancel");
        redirectUrls.setReturnUrl("http://localhost:8881/gassman-payment-service/paypal/process");
        payment.setRedirectUrls(redirectUrls);
        Payment createdPayment;
        String redirectUrl = "";
        APIContext context = new APIContext(clientId, clientSecret, "sandbox");
        createdPayment = payment.create(context);
        if (createdPayment != null) {
            List<Links> links = createdPayment.getLinks();
            for (Links link : links) {
                if (link.getRel().equals("approval_url")) {
                    redirectUrl = link.getHref();
                    break;
                }
            }
            response.put("status", "success");
            response.put("redirect_url", redirectUrl);
        }
        return response;
    }

    private void checkActive() throws PayPalRESTException {
        if (!active) {
            throw new PayPalRESTException("PayPal Payment not active");
        }
    }

    public Payment completePayment(HttpServletRequest req) throws PayPalRESTException {
        checkActive();

        Payment payment = new Payment();
        payment.setId(req.getParameter("paymentId"));

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(req.getParameter("PayerID"));
        APIContext context = new APIContext(clientId, clientSecret, "sandbox");
        payment = payment.execute(context, paymentExecution);

        return payment;
    }
}
