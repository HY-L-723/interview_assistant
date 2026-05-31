# Deployment

## Backend

Build:

```cmd
cd backend
mvn-local.cmd clean package
```

Required production environment variables:

```cmd
set SPRING_PROFILES_ACTIVE=prod
set DB_URL=jdbc:mysql://your-db-host:3306/interview_assistant?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
set DB_USERNAME=your_db_user
set DB_PASSWORD=your_db_password
set JWT_SECRET=replace-with-a-random-secret-at-least-32-bytes-long
set DEEPSEEK_API_KEY=your_deepseek_api_key
set CORS_ALLOWED_ORIGIN_PATTERNS=https://your-domain.com
set UPLOAD_PATH=C:\srv\interview-assistant\uploads
```

Initialize a new MySQL database with:

```cmd
cd backend\database
init-db.cmd
```

For an existing database, apply the same schema changes manually or with a migration tool before starting the backend. The production profile uses `ddl-auto=validate`, so missing columns will fail fast instead of mutating the schema automatically.

Run:

```cmd
java -jar backend\target\interview-assistant-1.0.0.jar
```

## Frontend

Build:

```cmd
cd frontend
npm ci
npm run build
```

Serve `frontend/dist` with a web server. Because the app uses history routing and relative `/api` requests, configure your reverse proxy like this:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /srv/interview-assistant/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 180s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/uploads/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
}
```
