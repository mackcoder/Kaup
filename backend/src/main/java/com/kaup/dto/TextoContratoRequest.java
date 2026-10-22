package com.kaup.dto;

/** Corpo da requisição quando o texto do contrato já está disponível (sem OCR). */
public record TextoContratoRequest(String texto) { }
