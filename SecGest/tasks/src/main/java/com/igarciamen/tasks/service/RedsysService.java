package com.igarciamen.tasks.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Implementacion del algoritmo de firma real de Redsys (la pasarela que hay
// detras del TPV Virtual de BBVA y de la mayoria de bancos espanoles), version
// "SHA-256" / HMAC_SHA256_V1, tal como lo documenta publicamente Redsys en su
// "Guia de Integracion para Comercios".
//
// Resumen del algoritmo:
//   1) Se rellena el numero de pedido (Ds_Order) con ceros hasta multiplo de 8 bytes.
//   2) Se cifra ese resultado con 3DES-CBC (IV de ceros) usando la clave secreta
//      del comercio -> esa salida es una clave derivada, unica para este pedido.
//   3) Se calcula HMAC-SHA256 de los Ds_MerchantParameters (ya en Base64) usando
//      esa clave derivada -> el resultado, en Base64, es la Ds_Signature.
@Service
public class RedsysService {

    @Value("${redsys.merchant-code}")
    private String merchantCode;

    @Value("${redsys.terminal}")
    private String terminal;

    @Value("${redsys.secret-key}")
    private String secretKeyBase64;

    @Value("${redsys.currency}")
    private String currency;

    @Value("${redsys.gateway-url}")
    private String gatewayUrl;

    @Value("${redsys.url-ok}")
    private String urlOk;

    @Value("${redsys.url-ko}")
    private String urlKo;

    @Value("${redsys.notify-url}")
    private String notifyUrl;

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    // Genera un Ds_Order valido: 4-12 caracteres, los 4 primeros numericos
    // (exigencia de Redsys), unico por operacion (no solo por tarea: si un pago
    // falla y se reintenta, hace falta un Ds_Order nuevo).
    public String generateOrderNumber(Long taskId) {
        String numericPrefix = String.format("%04d", System.currentTimeMillis() % 10000);
        String suffix = Long.toString(taskId, 36); // alfanumerico, compacto
        String order = numericPrefix + suffix;
        return order.length() > 12 ? order.substring(0, 12) : order;
    }

    // Construye el JSON de Ds_MerchantParameters (a mano, sin libreria de JSON,
    // para no depender de si el proyecto usa Jackson 2 o 3 -- ver incidencias
    // del Bloque 5/6 sobre este mismo problema) y lo devuelve ya en Base64.
    public String buildMerchantParametersBase64(String orderNumber, BigDecimal amount, String description) {
        // Redsys exige el importe en la unidad minima de la divisa (centimos para EUR),
        // sin decimales.
        long amountInCents = amount.movePointRight(2).longValueExact();

        String json = "{"
                + "\"DS_MERCHANT_AMOUNT\":\"" + amountInCents + "\","
                + "\"DS_MERCHANT_ORDER\":\"" + orderNumber + "\","
                + "\"DS_MERCHANT_MERCHANTCODE\":\"" + merchantCode + "\","
                + "\"DS_MERCHANT_CURRENCY\":\"" + currency + "\","
                + "\"DS_MERCHANT_TRANSACTIONTYPE\":\"0\","
                + "\"DS_MERCHANT_TERMINAL\":\"" + terminal + "\","
                + "\"DS_MERCHANT_MERCHANTURL\":\"" + notifyUrl + "\","
                + "\"DS_MERCHANT_URLOK\":\"" + urlOk + "\","
                + "\"DS_MERCHANT_URLKO\":\"" + urlKo + "\","
                + "\"DS_MERCHANT_PRODUCTDESCRIPTION\":\"" + escapeJson(description) + "\""
                + "}";

        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public String generateSignature(String orderNumber, String merchantParamsBase64) {
        try {
            byte[] key = Base64.getDecoder().decode(secretKeyBase64);
            byte[] derivedKey = encrypt3DES(key, padTo8Bytes(orderNumber));

            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(derivedKey, "HmacSHA256"));
            byte[] result = hmac.doFinal(merchantParamsBase64.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Error generando la firma de Redsys: " + e.getMessage(), e);
        }
    }

    // Para verificar la notificacion que envia Redsys: recalcula la firma esperada
    // a partir del Ds_Order y los Ds_MerchantParameters recibidos, y la compara
    // (en Base64 "URL-safe", que es como Redsys codifica la firma de vuelta).
    public boolean verifyNotificationSignature(String orderNumber, String merchantParamsBase64, String receivedSignature) {
        String expected = generateSignature(orderNumber, merchantParamsBase64);
        String expectedUrlSafe = toUrlSafeBase64(expected);
        String receivedNormalized = toUrlSafeBase64(receivedSignature);
        return expectedUrlSafe.equals(receivedNormalized);
    }

    public String decodeMerchantParameters(String merchantParamsBase64) {
        byte[] decoded = Base64.getUrlDecoder().decode(normalizeBase64(merchantParamsBase64));
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private byte[] encrypt3DES(byte[] key, byte[] data) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(new byte[8]); // IV de ceros, segun especificacion Redsys
        SecretKeySpec keySpec = new SecretKeySpec(key, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        return cipher.doFinal(data);
    }

    private byte[] padTo8Bytes(String order) {
        byte[] raw = order.getBytes(StandardCharsets.UTF_8);
        int blockSize = 8;
        int paddedLength = ((raw.length + blockSize - 1) / blockSize) * blockSize;
        byte[] result = new byte[paddedLength]; // el resto queda relleno con ceros por defecto
        System.arraycopy(raw, 0, result, 0, raw.length);
        return result;
    }

    private String toUrlSafeBase64(String standardBase64) {
        return standardBase64.replace('+', '-').replace('/', '_');
    }

    private String normalizeBase64(String urlSafeOrStandard) {
        return urlSafeOrStandard.replace('-', '+').replace('_', '/');
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
