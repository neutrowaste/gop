package br.com.gop.integration.gt06;

/**
 * Um quadro GT06 já validado (CRC e terminador), sem os bytes de envelope.
 */
public record Gt06Quadro(int protocolo, int serial, byte[] info) {
}
