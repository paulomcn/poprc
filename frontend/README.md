# RC Operations Hub - Frontend

Frontend em React + Vite para o sistema de gerenciamento de contratos e projetos.

## Stack

- **React 18.2** - Biblioteca de UI
- **Vite** - Build tool e dev server
- **Tailwind CSS** - Styling
- **React Router DOM v6+** - Roteamento
- **Lucide React** - Ícones
- **Axios** - Cliente HTTP

## Instalação

```bash
npm install
```

## Desenvolvimento

```bash
npm run dev
```

O servidor estará disponível em `http://localhost:5173`

## Build

```bash
npm run build
```

## Estrutura do Projeto

```
src/
├── components/          # Componentes reutilizáveis
│   ├── Alert.jsx
│   ├── Header.jsx
│   ├── Layout.jsx
│   ├── LoadingSpinner.jsx
│   ├── Modal.jsx
│   └── Sidebar.jsx
├── pages/              # Páginas/Telas
│   ├── Dashboard.jsx
│   ├── Contratos.jsx
│   ├── Projetos.jsx
│   └── Funcionarios.jsx
├── services/           # Serviços (API)
│   └── api.js
├── App.jsx            # Router principal
├── main.jsx           # Entry point
└── index.css          # Estilos globais
```

## Funcionalidades

### Dashboard
- Overview do sistema
- Estatísticas rápidas
- Ações rápidas

### Contratos
- Listagem em tabela elegante
- Criar novo contrato
- Upload de documentos (edital, proposta, aditivos)

### Projetos
- Listagem em cards
- Criar novo projeto
- Vincular a contrato
- Status com badges coloridas

### Funcionários
- Listagem em tabela
- Cadastro de novos funcionários
- Filtros por função e cidade

## Integração com API

A API está configurada em `src/services/api.js`:

- **Base URL**: `http://localhost:8080/api`
- **Autenticação**: Interceptor 401 redireciona para login Zoho
- **Cookies**: Habilitado com `withCredentials: true`

## Autenticação

O fluxo de autenticação é gerenciado pelo backend (Spring Security + Zoho):

1. Usuário acessa a aplicação
2. Se não autenticado, é redirecionado para `http://localhost:8080/login`
3. Backend gerencia autenticação Zoho
4. Cookies httpOnly são usados para manter a sessão
5. Se resposta 401, usuário é redirecionado novamente para login

## Responsividade

Todos os componentes são totalmente responsivos:
- **Mobile**: Layout em coluna única, sidebar em drawer
- **Tablet**: Layout ajustado
- **Desktop**: Layout completo com sidebar fixa

## Tratamento de Erros

- Exibição de alerts para erros
- Loading spinners durante requisições
- Validação de formulários

## Próximas Melhorias

- [ ] Autenticação com extração do nome do usuário da sessão
- [ ] Paginação nas tabelas
- [ ] Filtros avançados
- [ ] Export de dados (PDF/Excel)
- [ ] Testes unitários e E2E
- [ ] PWA capabilities
