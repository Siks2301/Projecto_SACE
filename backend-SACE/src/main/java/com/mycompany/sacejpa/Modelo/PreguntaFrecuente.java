package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.util.List;

/**
 * Pregunta frecuente (FAQ) que puede ser usada por uno o varios
 * {@link Chatbot} para responder de manera automatica a los clientes.
 */
@Entity
@Table(name = "pregunta_frecuente")
public class PreguntaFrecuente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pregunta")
    private String pregunta;

    @Column(name = "respuesta")
    private String respuesta;

    @Column(name = "palabras_clave")
    private String palabrasClave;

    @Column(name = "categoria")
    private String categoria;

    // Chatbots que usan esta pregunta frecuente
    @ManyToMany(mappedBy = "preguntasFrecuentesUsadas")
    private List<Chatbot> chatbots;

    public PreguntaFrecuente() {
    }

    public PreguntaFrecuente(String pregunta, String respuesta, String palabrasClave, String categoria) {
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.palabrasClave = palabrasClave;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getPalabrasClave() {
        return palabrasClave;
    }

    public void setPalabrasClave(String palabrasClave) {
        this.palabrasClave = palabrasClave;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public List<Chatbot> getChatbots() {
        return chatbots;
    }

    public void setChatbots(List<Chatbot> chatbots) {
        this.chatbots = chatbots;
    }

    @Override
    public String toString() {
        return "PreguntaFrecuente{" + "id=" + id + ", pregunta='" + pregunta + '\'' + '}';
    }
}
