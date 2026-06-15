CREATE DATABASE IF NOT EXISTS travel_buddy;
USE travel_buddy;

CREATE TABLE IF NOT EXISTS planned_trips (
    id INT AUTO_INCREMENT PRIMARY KEY,
    destination VARCHAR(255) NOT NULL,
    image_res INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(destination)
);
