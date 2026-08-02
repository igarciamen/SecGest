-- Se ejecuta solo la PRIMERA vez que se crea el volumen de "db" (contenedor vacio).
-- Crea las bases de datos adicionales que usan "tasks", "categories" y "documents",
-- ademas de la "usersSecGest" que ya crea POSTGRES_DB.
CREATE DATABASE "tasksSecGest";
CREATE DATABASE "categoriesSecGest";
CREATE DATABASE "documentsSecGest";
CREATE DATABASE "messagesSecGest";
