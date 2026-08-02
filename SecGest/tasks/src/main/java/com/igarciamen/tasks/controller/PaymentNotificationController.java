package com.igarciamen.tasks.controller;

import com.igarciamen.tasks.service.RedsysService;
import com.igarciamen.tasks.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Endpoint que llama Redsys DIRECTAMENTE desde sus propios servidores (no el
// navegador del cliente, no el frontend): notificacion servidor-a-servidor con
// el resultado real del pago. Por eso esta ruta es publica (sin JWT: Redsys no
// tiene ni puede tener un token nuestro) y valida en su lugar la firma HMAC.
//
// IMPORTANTE (ver README/incidencias del bloque): en desarrollo local, Redsys
// no puede alcanzar http://localhost:8082/... Hace falta exponer este endpoint
// con una herramienta tipo ngrok para probarlo de verdad contra el entorno de
// pruebas de Redsys.
@RestController
public class PaymentNotificationController {

    private static final Pattern ORDER_PATTERN = Pattern.compile("\"DS_ORDER\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"DS_RESPONSE\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    private final TaskService taskService;
    private final RedsysService redsysService;

    public PaymentNotificationController(TaskService taskService, RedsysService redsysService) {
        this.taskService = taskService;
        this.redsysService = redsysService;
    }

    @Operation(summary = "Notificacion servidor-a-servidor de Redsys con el resultado del pago (publico, sin token: lo llama Redsys)")
    @PostMapping(path = "/api/tasks/payments/redsys/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> notify(
            @RequestParam("Ds_SignatureVersion") String signatureVersion,
            @RequestParam("Ds_MerchantParameters") String merchantParameters,
            @RequestParam("Ds_Signature") String signature) {

        String decodedJson = redsysService.decodeMerchantParameters(merchantParameters);

        String orderNumber = extract(ORDER_PATTERN, decodedJson);
        String responseCode = extract(RESPONSE_PATTERN, decodedJson);

        if (orderNumber == null) {
            return ResponseEntity.badRequest().body("Ds_Order no encontrado en la notificacion");
        }

        boolean signatureOk = redsysService.verifyNotificationSignature(orderNumber, merchantParameters, signature);
        if (!signatureOk) {
            // Firma invalida: alguien (o algo) esta mandando una notificacion que no
            // viene realmente de Redsys, o los parametros se han corrompido. No se
            // procesa el pago en ningun caso.
            return ResponseEntity.status(403).body("Firma invalida");
        }

        taskService.confirmRedsysPayment(orderNumber, responseCode);

        // Redsys exige responder 200 con el cuerpo vacio (o "OK") para dar la
        // notificacion por recibida; si no, la reintentara varias veces.
        return ResponseEntity.ok("OK");
    }

    private String extract(Pattern pattern, String json) {
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}
