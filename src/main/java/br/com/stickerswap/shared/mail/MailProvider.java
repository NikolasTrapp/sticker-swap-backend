package br.com.stickerswap.shared.mail;

public interface MailProvider {
    void send(String to, String subject, String text);
}
