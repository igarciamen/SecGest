package com.igarciamen.tasks.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RedsysServiceTest {

    private RedsysService buildService() {
        RedsysService service = new RedsysService();
        // Mismos valores que application.properties (comercio de pruebas publico de Redsys).
        ReflectionTestUtils.setField(service, "merchantCode", "999008881");
        ReflectionTestUtils.setField(service, "terminal", "1");
        ReflectionTestUtils.setField(service, "secretKeyBase64", "sq7HjrUOBfKmC576ILgskD5srU870gJ7");
        ReflectionTestUtils.setField(service, "currency", "978");
        ReflectionTestUtils.setField(service, "gatewayUrl", "https://sis-t.redsys.es:25443/sis/realizarPago");
        ReflectionTestUtils.setField(service, "urlOk", "http://localhost:4200/payment/ok");
        ReflectionTestUtils.setField(service, "urlKo", "http://localhost:4200/payment/ko");
        ReflectionTestUtils.setField(service, "notifyUrl", "http://localhost:8082/api/tasks/payments/redsys/notify");
        return service;
    }

    @Test
    void generateOrderNumber_empiezaPorCuatroDigitosYNoSuperaDoceCaracteres() {
        RedsysService service = buildService();

        String order = service.generateOrderNumber(42L);

        assertTrue(order.length() >= 4 && order.length() <= 12);
        assertTrue(order.substring(0, 4).chars().allMatch(Character::isDigit));

        System.out.println("=== generateOrderNumber ===");
        System.out.println("Ds_Order generado: " + order);
    }

    @Test
    void buildMerchantParametersBase64_incluyeElImporteEnCentimos() {
        RedsysService service = buildService();

        String base64 = service.buildMerchantParametersBase64("000112345678", new BigDecimal("25.50"), "Test");
        String json = new String(Base64.getDecoder().decode(base64));

        assertTrue(json.contains("\"DS_MERCHANT_AMOUNT\":\"2550\""));
        assertTrue(json.contains("\"DS_MERCHANT_MERCHANTCODE\":\"999008881\""));
        assertTrue(json.contains("\"DS_MERCHANT_ORDER\":\"000112345678\""));

        System.out.println("=== buildMerchantParametersBase64 ===");
        System.out.println("JSON decodificado: " + json);
    }

    @Test
    void generateSignature_esDeterministaParaLosMismosDatos() {
        RedsysService service = buildService();
        String params = service.buildMerchantParametersBase64("000112345678", new BigDecimal("10.00"), "Test");

        String firma1 = service.generateSignature("000112345678", params);
        String firma2 = service.generateSignature("000112345678", params);

        assertEquals(firma1, firma2);
        assertFalse(firma1.isBlank());

        System.out.println("=== generateSignature: determinista ===");
        System.out.println("Firma: " + firma1);
    }

    @Test
    void generateSignature_cambiaSiCambiaElPedido() {
        RedsysService service = buildService();
        String params = service.buildMerchantParametersBase64("000112345678", new BigDecimal("10.00"), "Test");

        String firmaOrder1 = service.generateSignature("000112345678", params);
        String firmaOrder2 = service.generateSignature("000198765432", params);

        assertNotEquals(firmaOrder1, firmaOrder2);
    }

    @Test
    void verifyNotificationSignature_aceptaUnaFirmaValidaCorrectamenteRecalculada() {
        RedsysService service = buildService();
        String order = "000112345678";
        String params = service.buildMerchantParametersBase64(order, new BigDecimal("10.00"), "Test");
        String signature = service.generateSignature(order, params);

        assertTrue(service.verifyNotificationSignature(order, params, signature));

        System.out.println("=== verifyNotificationSignature: firma valida ===");
        System.out.println("Aceptada correctamente");
    }

    @Test
    void verifyNotificationSignature_rechazaUnaFirmaManipulada() {
        RedsysService service = buildService();
        String order = "000112345678";
        String params = service.buildMerchantParametersBase64(order, new BigDecimal("10.00"), "Test");

        assertFalse(service.verifyNotificationSignature(order, params, "firma-invalida-manipulada"));

        System.out.println("=== verifyNotificationSignature: firma invalida ===");
        System.out.println("Rechazada correctamente");
    }

    @Test
    void decodeMerchantParameters_devuelveElJsonOriginal() {
        RedsysService service = buildService();
        String base64 = service.buildMerchantParametersBase64("000112345678", new BigDecimal("10.00"), "Test");

        String decoded = service.decodeMerchantParameters(base64);

        assertTrue(decoded.contains("DS_MERCHANT_ORDER"));
    }
}
