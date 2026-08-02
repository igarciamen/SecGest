package com.igarciamen.notifications.service;

import com.igarciamen.notifications.service.impl.EmailServiceImpl;
import com.igarciamen.notifications.service.model.CorreoRequest;
import com.igarciamen.notifications.service.model.PresupuestoRequest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void enviarCorreo_procesaLaPlantillaEmailYManda() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email"), any(Context.class))).thenReturn("<html>contenido</html>");

        CorreoRequest req = new CorreoRequest();
        req.setDestinatario("cliente@mail.com");
        req.setAsunto("Prueba");
        req.setMensaje("Hola, esto es una prueba.");

        emailService.enviarCorreo(req);

        verify(templateEngine).process(eq("email"), any(Context.class));
        verify(javaMailSender).send(mimeMessage);

        System.out.println("=== enviarCorreo: plantilla procesada y enviado ===");
    }

    @Test
    void enviarPresupuesto_procesaLaPlantillaPresupuestoConLasVariablesCorrectas() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("presupuesto"), contextCaptor.capture())).thenReturn("<html>presupuesto</html>");

        PresupuestoRequest req = new PresupuestoRequest();
        req.setDestinatario("cliente@mail.com");
        req.setNombreCliente("Marco");
        req.setTituloTarea("Agendar reunion");
        req.setNombreCategoria("Agenda");
        req.setPrecio(new BigDecimal("20.00"));

        emailService.enviarPresupuesto(req);

        verify(templateEngine).process(eq("presupuesto"), any(Context.class));
        verify(javaMailSender).send(mimeMessage);

        Context usedContext = contextCaptor.getValue();
        // El precio se formatea como moneda (ej. "20,00 €"), no se manda el BigDecimal en crudo.
        assert usedContext.getVariable("precioFormateado") != null;

        System.out.println("=== enviarPresupuesto: plantilla procesada con variables correctas ===");
    }

    @Test
    void enviarCorreo_siJavaMailSenderFalla_lanzaRuntimeException() {
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP caido"));

        CorreoRequest req = new CorreoRequest();
        req.setDestinatario("cliente@mail.com");
        req.setAsunto("Prueba");
        req.setMensaje("Hola");

        assertThrows(RuntimeException.class, () -> emailService.enviarCorreo(req));

        System.out.println("=== enviarCorreo: fallo de SMTP propagado como RuntimeException ===");
    }
}