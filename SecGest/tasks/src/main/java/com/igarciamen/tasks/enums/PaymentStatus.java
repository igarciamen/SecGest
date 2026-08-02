package com.igarciamen.tasks.enums;

// SIMULADO_OK: pago simulado (Bloque 7 original), se confirma al instante.
// PENDIENTE/COMPLETADO/FALLIDO: ciclo real del TPV Redsys -- PENDIENTE se crea al
// redirigir al cliente a la pasarela; COMPLETADO o FALLIDO llegan mas tarde, via
// la notificacion servidor-a-servidor de Redsys, no en el momento de la redireccion.
public enum PaymentStatus {
    SIMULADO_OK,
    PENDIENTE,
    COMPLETADO,
    FALLIDO
}
