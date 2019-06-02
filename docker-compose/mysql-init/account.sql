create database test;
use test;

CREATE TABLE account
(
 user_id INT AUTO_INCREMENT PRIMARY KEY,
 user_name VARCHAR(255) NOT NULL UNIQUE
)