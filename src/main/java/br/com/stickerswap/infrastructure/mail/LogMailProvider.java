package br.com.stickerswap.infrastructure.mail;

import br.com.stickerswap.shared.mail.MailProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class LogMailProvider implements MailProvider {

    @Override
    public void send(String to, String subject, String text) {
        log.info("Mail (log mode) to={} subject='{}'\n{}", to, subject, text);
    }
}
