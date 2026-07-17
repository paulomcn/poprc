# Configuracao dos alertas externos

As notificacoes internas funcionam sem configuracao adicional. O envio por e-mail fica desabilitado por padrao e deve ser ativado somente depois que um servidor SMTP valido estiver disponivel.

## Variaveis de ambiente do e-mail

```text
ALERT_EMAIL_ENABLED=true
ALERT_EMAIL_FROM=alertas@empresa.com
MAIL_HOST=smtp.empresa.com
MAIL_PORT=587
MAIL_USERNAME=usuario-smtp
MAIL_PASSWORD=senha-smtp
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLED=true
```

Depois de reiniciar o backend, a pagina de configuracao de notificacoes exibira o canal de e-mail como habilitado. O destinatario continua sendo definido no campo `E-mail` dessa pagina.

O motor cria primeiro a notificacao interna e envia e-mail somente para alertas novos. Assim, executar novamente a varredura nao repete mensagens de um alerta que ja foi registrado.

## WhatsApp

O canal permanece indisponivel ate que seja escolhido e configurado um provedor oficial. O sistema nao simula envio e nao apresenta um numero salvo como canal ativo.
