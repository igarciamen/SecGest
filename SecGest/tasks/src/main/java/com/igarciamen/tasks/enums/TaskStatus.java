package com.igarciamen.tasks.enums;

// Ciclo de vida completo de una tarea (ver hoja de ruta del proyecto).
// En este bloque solo se usa PENDIENTE_REVISION; el resto se activaran
// en los bloques siguientes (presupuesto, aceptacion, pago, asignacion...).
public enum TaskStatus {
    PENDIENTE_REVISION,
    PRESUPUESTADA,
    ACEPTADA,
    RECHAZADA,
    PAGADA,
    ASIGNADA,
    EN_PROCESO,
    ENTREGADA,
    COMPLETADA
}
