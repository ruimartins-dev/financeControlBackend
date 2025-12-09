# Finance Control Backend

Backend application for financial control built with Spring Boot and PostgreSQL.

## Pré-requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 12+

## Configuração do PostgreSQL

1. Certifique-se de que o PostgreSQL está rodando na porta 5432
2. Crie um banco de dados chamado `financecontrol`:
```sql
CREATE DATABASE financecontrol;
```

## Configuração da Aplicação

As configurações estão no arquivo `src/main/resources/application.properties`:

- **Database URL**: `jdbc:postgresql://localhost:5432/financecontrol`
- **Username**: `postgres`
- **Password**: `postgres`
- **Server Port**: `8080`

### Configuração com Variáveis de Ambiente (Recomendado para Produção)

Para ambientes de produção, é recomendado usar variáveis de ambiente em vez de credenciais hard-coded:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/financecontrol
export SPRING_DATASOURCE_USERNAME=seu_usuario
export SPRING_DATASOURCE_PASSWORD=sua_senha
```

Ou no `application.properties`:
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/financecontrol}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:postgres}
```

## Como Executar

### Usando Maven

```bash
mvn spring-boot:run
```

### Usando o arquivo JAR

```bash
mvn clean package
java -jar target/financeControlBackend-1.0-SNAPSHOT.jar
```

## Verificação de Status

### Endpoint de Status Personalizado

Depois que a aplicação iniciar, você pode verificar o status da aplicação e do PostgreSQL acessando:

```
GET http://localhost:8080/api/status
```

Resposta exemplo:
```json
{
  "application": "running",
  "message": "Aplicação iniciada com sucesso",
  "database": "connected",
  "databaseMessage": "PostgreSQL conectado e funcionando",
  "databaseType": "PostgreSQL",
  "databaseVersion": "15.x"
}
```

### Endpoint de Health Check (Actuator)

O Spring Boot Actuator também fornece um endpoint de health check:

```
GET http://localhost:8080/actuator/health
```

**Nota de Segurança**: O endpoint de health está configurado para mostrar detalhes completos apenas quando autorizado. Em produção, considere adicionar autenticação para este endpoint.

Resposta exemplo (sem autorização):
```json
{
  "status": "UP"
}
```

## Estrutura do Projeto

```
financeControlBackend/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/
│       │       └── example/
│       │           ├── Main.java                    # Classe principal da aplicação
│       │           └── controller/
│       │               └── StatusController.java    # Controller de status
│       └── resources/
│           └── application.properties               # Configurações da aplicação
└── pom.xml                                          # Dependências Maven
```

## Dependências Principais

- Spring Boot 3.2.0
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Actuator
- PostgreSQL Driver

## Troubleshooting

### Erro de conexão com PostgreSQL

Se você receber um erro de conexão com o PostgreSQL, verifique:

1. PostgreSQL está rodando: `sudo service postgresql status`
2. Porta 5432 está aberta
3. Credenciais no `application.properties` estão corretas
4. O banco de dados `financecontrol` foi criado

### Porta 8080 já está em uso

Se a porta 8080 já estiver em uso, você pode modificar no arquivo `application.properties`:

```properties
server.port=8081
```
