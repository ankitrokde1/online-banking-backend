# Online Banking Backend

This repository contains the backend of an online banking system, built with Spring Boot and MongoDB. It provides RESTful APIs for user authentication, account management, transaction processing, admin controls, email notifications, and more.

---

## Features

- **User Registration & Authentication**: JWT-secured endpoints for sign-up, login, password management.
- **Account Management**: Create, update, and view bank accounts.
- **Transaction Management**: Deposit, withdraw, transfer funds, and view transaction history.
- **Admin Controls**: Manage users, accounts, and review all transactions.
- **Email Notifications**: Sends alerts and confirmations for key banking activities.
- **Rate Limiting**: Protects from brute-force and abuse attacks.
- **Logging**: File-based logging for activity tracking and debugging.
- **Environment-Based Configurations**: Separate configs for development and production.

---

## Tech Stack

- **Backend Framework**: Spring Boot
- **Database**: MongoDB (via Spring Data MongoDB)
- **Security**: JWT, Spring Security
- **Email**: SMTP (Gmail)
- **Rate Limiting**: Custom filter
- **Logging**: File logging via Spring Boot
- **Environment Management**: `application.yml`, `.env` support

---

## Getting Started

### 1. Clone the Repository

```sh
git clone https://github.com/ankitrokde1/online-banking-backend.git
cd online-banking-backend
```

### 2. Set Up Environment Variables

Create a `.env` file in the project root (do **NOT** commit this file):

```
MONGODB_URI=your-mongodb-uri
MONGODB_DATABASE=online_banking_db
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
JWT_SECRET=your-jwt-secret
FRONTEND_URL=http://localhost:5173
```

### 3. Configure Spring Profiles

- `application.yml`: Common settings.
- `application-dev.yml`: Development overrides.
- `application-prod.yml`: Production overrides (keep secrets out of repo).

Spring Boot will automatically pick the profile based on the `spring.profiles.active` environment variable.

### 4. Build and Run

```sh
./mvnw spring-boot:run
```
or
```sh
./gradlew bootRun
```
or run from your IDE.

---

## API Endpoints

The main endpoints include:

- `/api/auth/*` – User authentication
- `/api/accounts/*` – Account management
- `/api/transactions/*` – Transactions
- `/api/admin/*` – Admin operations

See the code for full details.

---

## Logging

Logs are written to `logs/online-banking-app.log` as configured in `application.yml`.

---

## Security & Best Practices

- **Never commit real secrets**: Use `.env` and keep it in `.gitignore`.
- **Rotate secrets** if they were accidentally pushed.
- **Secure email passwords**: Use app-specific passwords for Gmail.

---

## Contribution

Pull requests and issues are welcome! Please open an issue for bugs, feature requests, or questions.

---

## Contact

For questions or support, open an issue or contact [ankitrokde1](https://github.com/ankitrokde1).
