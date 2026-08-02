package com.igarciamen.notifications.controller;

import com.igarciamen.notifications.service.IEmailService;
import com.igarciamen.notifications.service.model.CorreoRequest;
import com.igarciamen.notifications.service.model.PresupuestoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    private final IEmailService emailService;

    public EmailController(IEmailService emailService) {
        this.emailService = emailService;
    }

    @Operation(
            summary = "Envia un correo generico (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/enviar-correo")
    public ResponseEntity<String> enviarCorreo(@RequestBody CorreoRequest correoRequest) {
        try {
            emailService.enviarCorreo(correoRequest);
            return new ResponseEntity<>("Correo enviado exitosamente.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al enviar el correo: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Envia el correo de presupuesto de una tarea (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/notificar-presupuesto")
    public ResponseEntity<String> notificarPresupuesto(@RequestBody PresupuestoRequest presupuestoRequest) {
        try {
            emailService.enviarPresupuesto(presupuestoRequest);
            return new ResponseEntity<>("Correo de presupuesto enviado.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al enviar el correo de presupuesto: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
