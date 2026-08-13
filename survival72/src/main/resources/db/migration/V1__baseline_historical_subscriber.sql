CREATE TABLE subscriber (
    deslizamiento BIT(1) NOT NULL,
    huracan BIT(1) NOT NULL,
    inundacion BIT(1) NOT NULL,
    terremoto BIT(1) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    apellido VARCHAR(255) DEFAULT NULL,
    ciudad VARCHAR(255) DEFAULT NULL,
    email VARCHAR(255) DEFAULT NULL,
    nombre VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
