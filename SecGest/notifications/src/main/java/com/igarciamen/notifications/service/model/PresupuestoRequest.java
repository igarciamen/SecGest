package com.igarciamen.notifications.service.model;

import java.math.BigDecimal;

// Correo especifico del Bloque 5: aviso de presupuesto. Usa su propia
// plantilla ("presupuesto.html"), no la generica de CorreoRequest.
public class PresupuestoRequest {
    private String destinatario;
    private String nombreCliente;
    private String tituloTarea;
    private String nombreCategoria;
    private BigDecimal precio;

    public PresupuestoRequest() {}

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTituloTarea() { return tituloTarea; }
    public void setTituloTarea(String tituloTarea) { this.tituloTarea = tituloTarea; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
