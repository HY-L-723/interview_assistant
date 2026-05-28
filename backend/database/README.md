# Database Setup

This backend uses MySQL database `interview_assistant`.

Run from this directory after MySQL is installed and running:

```cmd
init-db.cmd
```

Default connection:

- Host: `localhost`
- Port: `3306`
- Database: `interview_assistant`
- Username: `root`
- Password: `root`

Override with environment variables when needed:

```cmd
set DB_HOST=localhost
set DB_PORT=3306
set DB_USERNAME=root
set DB_PASSWORD=your_password
init-db.cmd
```

The Spring Boot backend reads the same credentials through:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
