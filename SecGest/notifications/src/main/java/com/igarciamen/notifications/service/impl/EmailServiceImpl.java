package com.igarciamen.notifications.service.impl;

import com.igarciamen.notifications.service.IEmailService;
import com.igarciamen.notifications.service.model.CorreoRequest;
import com.igarciamen.notifications.service.model.PresupuestoRequest;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class EmailServiceImpl implements IEmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailServiceImpl(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void enviarCorreo(CorreoRequest correoRequest) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(correoRequest.getDestinatario());
            helper.setSubject(correoRequest.getAsunto());
            Context context = new Context();
            context.setVariable("mensaje", correoRequest.getMensaje());
            String contenidoHtml = templateEngine.process("email", context);
            helper.setText(contenidoHtml, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }

    @Override
    public void enviarPresupuesto(PresupuestoRequest req) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(req.getDestinatario());
            helper.setSubject("Presupuesto de tu encargo: " + req.getTituloTarea());

            Context context = new Context();
            context.setVariable("nombreCliente", req.getNombreCliente());
            context.setVariable("tituloTarea", req.getTituloTarea());
            context.setVariable("nombreCategoria", req.getNombreCategoria());
            context.setVariable("precioFormateado", formatearPrecio(req.getPrecio()));

            String contenidoHtml = templateEngine.process("presupuesto", context);
            helper.setText(contenidoHtml, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo de presupuesto: " + e.getMessage(), e);
        }
    }

    private String formatearPrecio(java.math.BigDecimal precio) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return formatter.format(precio);
    }
}
