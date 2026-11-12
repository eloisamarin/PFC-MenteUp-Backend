package com.umc.menteup.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "tb_codigo_2fa")
public class Codigo2FA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(nullable = false)
    private boolean utilizado = false;

    // Erros de digitação deste código. Ao chegar no limite, o código é inutilizado (contra força bruta).
    // Pode ser nulo nas linhas antigas da tabela: getTentativas() trata como 0.
    @Column(name = "tentativas")
    private Integer tentativas = 0;

    // Quantas vezes o código já foi reenviado desde o login com a senha (o reenvio tem limite)
    @Column(name = "reenvios")
    private Integer reenvios = 0;

    // Construtor vazio exigido pelo JPA
    public Codigo2FA() {
    }

    // Construtor usado para gerar um novo código
    public Codigo2FA(
            String usuario,
            String codigo,
            LocalDateTime expiracao
    ) {
        this.usuario = usuario;
        this.codigo = codigo;
        this.expiracao = expiracao;
        this.utilizado = false;
    }

    // Construtor completo, caso precise em algum momento
    public Codigo2FA(
            Long id,
            String usuario,
            String codigo,
            LocalDateTime expiracao,
            boolean utilizado
    ) {
        this.id = id;
        this.usuario = usuario;
        this.codigo = codigo;
        this.expiracao = expiracao;
        this.utilizado = utilizado;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getExpiracao() {
        return expiracao;
    }

    public void setExpiracao(LocalDateTime expiracao) {
        this.expiracao = expiracao;
    }

    public boolean isUtilizado() {
        return utilizado;
    }

    public void setUtilizado(boolean utilizado) {
        this.utilizado = utilizado;
    }

    public int getTentativas() {
        return tentativas == null ? 0 : tentativas;
    }

    public void setTentativas(Integer tentativas) {
        this.tentativas = tentativas;
    }

    public int getReenvios() {
        return reenvios == null ? 0 : reenvios;
    }

    public void setReenvios(Integer reenvios) {
        this.reenvios = reenvios;
    }

    /** Conta um erro; ao chegar no limite, inutiliza o código. Devolve as tentativas que ainda restam. */
    public int registrarTentativaErrada(int limite) {
        this.tentativas = getTentativas() + 1;
        if (this.tentativas >= limite) {
            this.utilizado = true;
        }
        return Math.max(0, limite - this.tentativas);
    }
}