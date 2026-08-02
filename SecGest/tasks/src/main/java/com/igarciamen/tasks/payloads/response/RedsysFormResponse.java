package com.igarciamen.tasks.payloads.response;

// Datos que el frontend necesita para construir un <form> y auto-enviarlo
// (POST) contra la pasarela de Redsys -- asi es como funciona de verdad esta
// integracion: no es una llamada AJAX, es una redireccion completa del navegador.
public class RedsysFormResponse {

    private String actionUrl;
    private String dsSignatureVersion = "HMAC_SHA256_V1";
    private String dsMerchantParameters;
    private String dsSignature;

    public RedsysFormResponse() {}

    public RedsysFormResponse(String actionUrl, String dsMerchantParameters, String dsSignature) {
        this.actionUrl = actionUrl;
        this.dsMerchantParameters = dsMerchantParameters;
        this.dsSignature = dsSignature;
    }

    public String getActionUrl() { return actionUrl; }
    public String getDsSignatureVersion() { return dsSignatureVersion; }
    public String getDsMerchantParameters() { return dsMerchantParameters; }
    public String getDsSignature() { return dsSignature; }
}
