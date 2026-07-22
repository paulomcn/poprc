# Segurança de acesso

## Perfis internos

- `ADMIN`: acesso administrativo, financeiro e às configurações.
- `SUPERVISOR_TECNICO`: contratos, projetos, OS, obras e acompanhamento de campo.
- `TECNICO`: OS atribuídas, execução de campo, evidências e documentos.
- `ESTOQUE`: materiais, movimentações e ordens de retirada/devolução.
- `AUDITOR`: obras, conciliação e homologação de As-Built.

O perfil, CPF, telefone, e-mail de acesso e situação ativa são definidos no cadastro de funcionários. O CPF é a identificação do login local; o e-mail deve coincidir com a conta da Zoho quando o OAuth for ativado.

Para vincular uma conta Zoho, entre localmente como administrador, abra **Equipes**, edite o funcionário e informe exatamente o e-mail da conta Zoho. O funcionário também precisa permanecer ativo. O login usa somente o escopo `zohoprofile.userinfo.read`, destinado à identificação do perfil, sem solicitar acesso aos módulos do CRM.

## Login por CPF

Ao cadastrar um funcionário, informe um CPF válido e uma senha temporária com ao menos oito caracteres, letras e números. No primeiro acesso, o sistema encaminha o funcionário para **Meu perfil** e exige a definição de uma senha própria.

Para funcionários antigos, use o login de desenvolvimento como administrador, abra **Equipes**, edite cada cadastro para informar CPF e telefone e use **Redefinir senha**. O CPF completo nunca é devolvido pela API; as listagens exibem apenas a versão mascarada.

O sistema bloqueia o login por 15 minutos depois de cinco tentativas inválidas consecutivas.

## Operações sensíveis

Alterações em estoque, retirada/devolução, funcionários, faturamento, arquivamentos, homologações e assinaturas exigem uma autenticação recente. Quando necessário, o frontend solicita a senha e repete a operação automaticamente após a validação. O período padrão é de cinco minutos e pode ser ajustado por `REAUTHENTICATION_MINUTES`.

Cada operação autorizada gera um registro append-only com usuário, perfil, método de autenticação, rota, resultado, IP e horário. Usuários da Zoho também devem ter CPF e senha local configurados para confirmar essas operações.

## Ambiente local

Por decisão de estabilização, a autenticação está suspensa no ambiente local. O frontend entra automaticamente como administrador de teste e o backend não exige sessão enquanto `APP_SECURITY_ENABLED=false`. Esse modo serve apenas para desenvolvimento do fluxo funcional e não pode ser usado em piloto ou produção.

```properties
APP_SECURITY_ENABLED=false
DEV_LOGIN_ENABLED=true
REAUTHENTICATION_MINUTES=5
FRONTEND_URL=http://localhost:5173
```

O código de CPF/senha, Zoho, perfis, CSRF e reautenticação foi preservado para retomada em uma fase isolada. Não use o login local como evidência de segurança enquanto a chave estiver desativada.

## Publicação

1. Cadastre CPF e senha local para todos os usuários e e-mails válidos para quem utilizar a Zoho.
2. Configure as credenciais OAuth da Zoho por variáveis de ambiente.
3. Desative obrigatoriamente o login local com `DEV_LOGIN_ENABLED=false`.
4. Defina `FRONTEND_URL` com a URL pública do frontend.
5. Restrinja `APP_CORS_ALLOWED_ORIGIN_PATTERNS` à URL pública exata.
6. Mantenha `APP_SECURITY_ENABLED=true`.

Uma falha na Zoho ou um e-mail sem vínculo ativo agora interrompe o login; o sistema não gera mais usuário fictício.
As operações de escrita também exigem o token CSRF emitido pelo backend e enviado automaticamente pelo frontend.
