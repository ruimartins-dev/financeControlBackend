# Financial Control Backend

A simple Spring Boot backend for personal financial management.

## Features

- User registration and authentication (HTTP Basic)
- Wallet management (create, list, view)
- Transaction management with filtering
- PostgreSQL database
- Docker support

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security (HTTP Basic Auth)
- Spring Data JPA
- PostgreSQL
- Maven
- Docker & Docker Compose

## Project Structure

```
src/main/java/com/example/financialcontrol/
├── config/                 # Security and exception handling configuration
├── controller/             # REST API endpoints
├── dto/                    # Data Transfer Objects (request/response)
├── entity/                 # JPA entities (database models)
├── repository/             # Database access layer
├── service/                # Business logic layer
└── FinancialControlApplication.java  # Main application class
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL (for local development without Docker)

### Running with Docker (Recommended)

1. Build and start the containers:
   ```bash
   docker-compose up --build
   ```

2. The application will be available at `http://localhost:8080`

3. To stop the containers:
   ```bash
   docker-compose down
   ```

### Running Locally (Without Docker)

1. Start a PostgreSQL database:
   - Create a database named `financial_control`
   - Default credentials: `postgres/postgres`

2. Build and run the application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register a new user | No |
| POST | `/api/auth/login` | Verify credentials | No |

### Wallets

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/wallets` | List user's wallets | Yes |
| POST | `/api/wallets` | Create a new wallet | Yes |
| GET | `/api/wallets/{id}` | Get wallet details | Yes |

### Transactions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/wallets/{walletId}/transactions` | List transactions | Yes |
| POST | `/api/wallets/{walletId}/transactions` | Create transaction | Yes |
| DELETE | `/api/transactions/{id}` | Delete transaction | Yes |

### Transaction Filters

When listing transactions, you can use these query parameters:
- `type`: Filter by `DEBIT` or `CREDIT`
- `fromDate`: Start date (format: `YYYY-MM-DD`)
- `toDate`: End date (format: `YYYY-MM-DD`)

Example: `GET /api/wallets/1/transactions?type=DEBIT&fromDate=2024-01-01&toDate=2024-12-31`

## API Examples

### Register a New User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "password123"
  }'
```

### Login (Verify Credentials)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "password123"
  }'
```

### Create a Wallet

```bash
curl -X POST http://localhost:8080/api/wallets \
  -H "Content-Type: application/json" \
  -u john:password123 \
  -d '{
    "name": "Main Account",
    "currency": "EUR"
  }'
```

### List Wallets

```bash
curl -X GET http://localhost:8080/api/wallets \
  -u john:password123
```

### Create a Transaction

```bash
curl -X POST http://localhost:8080/api/wallets/1/transactions \
  -H "Content-Type: application/json" \
  -u john:password123 \
  -d '{
    "type": "DEBIT",
    "category": "Food",
    "subcategory": "Groceries",
    "amount": 50.00,
    "description": "Weekly groceries",
    "date": "2024-01-15"
  }'
```

### List Transactions with Filters

```bash
curl -X GET "http://localhost:8080/api/wallets/1/transactions?type=DEBIT&fromDate=2024-01-01" \
  -u john:password123
```

### Delete a Transaction

```bash
curl -X DELETE http://localhost:8080/api/transactions/1 \
  -u john:password123
```

## Authentication

This application uses HTTP Basic Authentication. After registering, include your credentials in every request:

- **Header**: `Authorization: Basic base64(username:password)`
- **Curl**: Use the `-u username:password` flag

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| DATABASE_URL | PostgreSQL connection URL | jdbc:postgresql://localhost:5432/financial_control |
| DATABASE_USERNAME | Database username | postgres |
| DATABASE_PASSWORD | Database password | postgres |
| SERVER_PORT | Application port | 8080 |

## Data Models

### User
- `id`: Unique identifier
- `username`: Unique username for login
- `email`: Unique email address
- `password`: BCrypt hashed password
- `createdAt`: Account creation timestamp

### Wallet
- `id`: Unique identifier
- `userId`: Owner user ID
- `name`: Wallet name (e.g., "Main Account")
- `currency`: Currency code (e.g., "EUR")
- `createdAt`: Creation timestamp

### Transaction
- `id`: Unique identifier
- `walletId`: Parent wallet ID
- `type`: DEBIT (expense) or CREDIT (income)
- `category`: Transaction category
- `subcategory`: More specific category
- `amount`: Transaction amount
- `description`: Optional description
- `date`: Transaction date
- `createdAt`: Record creation timestamp

## License

This project is for educational purposes.

