package com.igarciamen.notifications.service;

import com.igarciamen.notifications.service.model.CorreoRequest;
import com.igarciamen.notifications.service.model.PresupuestoRequest;

public interface IEmailService {

    // Correo generico (plantilla email.html).
    void enviarCorreo(CorreoRequest correoRequest);

    // Correo especifico de presupuesto (plantilla presupuesto.html).
    void enviarPresupuesto(PresupuestoRequest presupuestoRequest);
}
