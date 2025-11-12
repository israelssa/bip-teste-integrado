# 🏦 Sistema de Gerenciamento de Benefícios

Sistema completo para gerenciamento de transferências financeiras entre benefícios, implementando diferentes estratégias de controle de concorrência.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Instalação e Execução](#instalação-e-execução)
- [API Endpoints](#api-endpoints)
- [Estratégias de Locking](#estratégias-de-locking)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Desenvolvimento](#desenvolvimento)

## 🎯 Visão Geral

Este projeto consiste em uma aplicação full-stack para gerenciar transferências entre benefícios financeiros, com foco em controle de concorrência e consistência de dados em ambientes de alta simultaneidade.

### Backend (Java/Spring Boot)
- API REST para operações CRUD de benefícios
- Três estratégias de controle de concorrência
- Validações de negócio e tratamento de exceções
- Documentação Swagger/OpenAPI

### Frontend (Angular)
- Interface moderna e responsiva
- Componentes reativos com Angular Material
- Controle de estado e tratamento de erros
- Comunicação em tempo real com o backend

## ✨ Funcionalidades

### 🔄 Transferências
- ✅ **Transferência entre benefícios** com validações
- 🔒 **3 estratégias de locking** (Optimistic, Pessimistic, Mixed)
- 💰 **Verificação de saldo** em tempo real
- ⚠️ **Tratamento de conflitos** de concorrência

### 📊 Consultas
- 👁️ **Listagem de benefícios** com saldos atualizados
- 💵 **Consulta de saldo** individual
- 🔍 **Controle de versão** para detecção de conflitos
- 📈 **Histórico de consultas**

### 🛡️ Segurança e Validações
- ✅ **Validação de parâmetros**
- ⚠️ **Tratamento de exceções**
- 🔄 **Retentativas automáticas** para conflitos
- 📝 **Logs detalhados** para debugging

## 🛠 Tecnologias

### Backend
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **H2 Database** (desenvolvimento)
- **SpringDoc OpenAPI** (documentação)
- **JUnit 5 & Mockito** (testes)
- **Maven**

### Frontend
- **Angular 17+**
- **Angular Material**
- **TypeScript**
- **RxJS**
- **Vite** (build tool)
- **Jasmine & Karma** (testes)

## 🏗 Arquitetura

### Backend Architecture
```
Controller Layer (REST API)
    ↓
Service Layer (Lógica de Negócio)
    ↓
Repository Layer (Data Access)
    ↓
Entity Layer (JPA Entities)
    ↓
Database (H2/PostgreSQL)
```

### Frontend Architecture
```
Components (UI)
    ↓
Services (HTTP Calls)
    ↓
Interfaces (Data Models)
    ↓
HTTP Client (Angular HttpClient)
```

## 🚀 Instalação e Execução

### Pré-requisitos
- Java 17 ou superior
- Node.js 18+ e npm
- Angular CLI 17+
- Maven 3.6+

### Backend (Spring Boot)

```bash
# Navegue para a pasta do backend
cd backend

# Compile o projeto
mvn clean compile

# Execute a aplicação
mvn spring-boot:run

# Ou execute o JAR
mvn clean package
java -jar target/beneficio-backend-1.0.0.jar
```

**Acesse:**
- Aplicação: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

### Frontend (Angular)

```bash
# Navegue para a pasta do frontend
cd frontend

# Instale as dependências
npm install

# Execute em modo desenvolvimento
ng serve

# Ou com proxy configurado
ng serve --proxy-config proxy.conf.json
```

**Acesse:** http://localhost:4200

### Execução com Docker (Opcional)

```bash
# Backend
docker build -t beneficio-backend .
docker run -p 8080:8080 beneficio-backend

# Frontend
docker build -t beneficio-frontend .
docker run -p 4200:80 beneficio-frontend
```

## 📡 API Endpoints

### Benefícios
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/beneficios` | Listar todos os benefícios |
| `GET` | `/api/v1/beneficios/{id}/saldo` | Consultar saldo |
| `GET` | `/api/v1/beneficios/{id}/versao` | Obter versão |

### Transferências
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/v1/beneficios/transferir` | Transferir (Optimistic) |
| `POST` | `/api/v1/beneficios/transferir/pessimistic` | Transferir (Pessimistic) |
| `POST` | `/api/v1/beneficios/transferir/mixed` | Transferir (Mixed) |

### Controle de Concorrência
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/v1/beneficios/verificar-conflito` | Verificar conflito de versão |
| `GET` | `/api/v1/beneficios/verificar-transferencia` | Verificar possibilidade |

## 🔒 Estratégias de Locking

### 1. Optimistic Locking
- **Uso**: Ambientes com média/baixa contenção
- **Vantagens**: Melhor performance, não bloqueia recursos
- **Desvantagens**: Pode necessitar de retentativas
- **Máx. Tentativas**: 3 com backoff exponencial

### 2. Pessimistic Locking
- **Uso**: Ambientes com alta contenção
- **Vantagens**: Garante consistência, sem conflitos
- **Desvantagens**: Pode causar deadlocks, menor performance

### 3. Mixed Locking
- **Uso**: Cenários com contenção principalmente na origem
- **Estratégia**: Pessimistic na origem + Optimistic no destino
- **Balanceamento**: Performance e consistência

## 📁 Estrutura do Projeto

### Backend
```
backend/
├── src/main/java/com/exemple/backend/
│   ├── controller/          # REST Controllers
│   ├── service/            # Business Logic
│   ├── repository/         # Data Access
│   ├── entity/            # JPA Entities
│   ├── config/            # Configuration
│   └── dto/               # Data Transfer Objects
├── src/test/java/         # Test Classes
└── src/main/resources/
    ├── application.properties
    └── data.sql           # Initial Data
```

### Frontend
```
frontend/
├── src/app/
│   ├── components/        # Angular Components
│   │   ├── beneficio-list/
│   │   ├── transferencia-form/
│   │   ├── saldo-display/
│   │   └── versao-check/
│   ├── services/          # HTTP Services
│   ├── interfaces/        # TypeScript Interfaces
│   └── app.module.ts     # Main Module
├── src/assets/           # Static Files
└── src/environments/     # Environment Configs
```

## 🧪 Testes

### Backend Tests
```bash
# Executar todos os testes
mvn test

# Executar testes com cobertura
mvn jacoco:report

# Executar testes de integração
mvn verify
```

### Frontend Tests
```bash
# Executar testes unitários
ng test

# Executar testes com cobertura
ng test --code-coverage

# Executar testes end-to-end
ng e2e
```

## 🔧 Configuração

### Backend Configuration
**`application.properties`**
```properties
# Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# H2 Console
spring.h2.console.enabled=true

# Server
server.port=8080
```

### Frontend Configuration
**`proxy.conf.json`**
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

## 🚨 Troubleshooting

### Problemas Comuns

1. **Erro CORS**
   - Solução: Configure proxy ou adicione `@CrossOrigin` no backend

2. **Cache do Angular**
   ```bash
   ng cache clean --force
   ```

3. **Portas em uso**
   ```bash
   # Encontrar processos nas portas
   netstat -ano | findstr :8080
   netstat -ano | findstr :4200
   ```

4. **Dependências corrompidas**
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

## 📈 Monitoramento e Logs

### Backend Logs
- **Desenvolvimento**: Logs detalhados no console
- **Produção**: Configurar log levels no `application.properties`

### Frontend Logs
- **Console**: Logs de debug no browser console
- **Network**: Monitorar requisições HTTP no DevTools

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Distribuído sob a licença MIT. Veja `LICENSE` para mais informações.

## 👥 Autores

- **Seu Nome** - [seu.email@empresa.com](mailto:seu.email@empresa.com)

## 📞 Suporte

Em caso de problemas:

1. Verifique a documentação
2. Procure em issues existentes
3. Crie uma nova issue com detalhes do problema

---

**⭐️ Se este projeto foi útil, considere dar uma estrela no repositório!**