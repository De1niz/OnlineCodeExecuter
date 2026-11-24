# Online Java Code Executer - Professional Project Presentation

## 📋 Project Overview

### 🎯 Project Purpose

Online Java Code Executer is a professional online IDE solution that
allows users to write and execute Java code through a web browser, with
real-time results and professional-grade features.

## 🏗️ Technology Stack

### Backend

-   **Java 21** - Modern Java version for high performance
-   **Spring Boot 3.2.0** - Enterprise-level application framework
-   **Spring Data JPA** - Database operations
-   **Spring WebSocket** - Real-time communication
-   **H2 Database** - In-memory database (development)

### Frontend

-   **HTML5/CSS3** - Modern responsive design
-   **JavaScript ES6+** - Interactive features
-   **ACE Editor** - Professional code editor
-   **WebSocket (STOMP)** - Real-time updates
-   **SockJS** - WebSocket fallback support

### Development Tools

-   **Maven** - Dependency management and build
-   **Docker** - Containerization
-   **Git** - Version control

## 🚀 Project Setup and Execution

### 1. Project Build and Run

``` bash
./mvnw clean spring-boot:run
```

### 2. Alternative Execution Methods

``` bash
./mvnw clean package
java -jar target/onlinecodeexecuter-0.0.1-SNAPSHOT.jar
docker-compose up --build
```

## 🐳 Docker Container Setup

### Current Running Containers

-   **onlinecodeexect** -- code-execute\
-   **mysql-1** -- mysql8.0

### Docker Container Details

#### 🚀 Online Code Execute

-   Container name: onlinecodeexect\
-   Image: code-execute\
-   Port: 8080:8080

#### 🗄️ MySQL Database

-   Container name: mysql-1\
-   Image: mysql8.0\
-   Port: 3309:3306

### Docker Kullanımı

``` bash
docker ps
docker logs [id]
docker exec -it [id] bash
docker stop [id]
docker start [id]
```

## 🌐 Erişim Bilgileri

-   API: http://localhost:8080\
-   MySQL: localhost:3309

## 🏗️ System Architecture

    Frontend ↔ Spring Boot ↔ H2 Database
    ↑ WebSocket       ↑ REST API     ↑ JPA

### Code Execution Flow

1.  User writes code\
2.  Frontend sends POST request\
3.  Backend compiles & executes\
4.  WebSocket sends result

## 📊 Database Structure

### CodeExecutionRequest Entity

-   id\
-   code\
-   status\
-   output\
-   errorDetails\
-   executionTimeMs\
-   timestamps\
-   lineCount\
-   characterCount

## 🔧 Core Features

### 1. 🎨 Professional Code Editor

-   Syntax highlighting\
-   Auto-completion\
-   Themes\
-   Error highlighting

### 2. ⚡ Real-time Code Execution

-   Async\
-   Live output\
-   Progress tracking

### 3. 🎯 Smart Code Processing

Automatic wrapper class

### 4. 📊 Performance Metrics

## 🛠️ Development Process

Phases 1--4 completed successfully.

## 🎨 User Experience Features

-   Real-time stats\
-   Themes\
-   Code examples

## 🔒 Security

-   Input validation\
-   Sandbox\
-   Time limits

## 📈 Performance

-   Compilation: 100--500ms\
-   Execution: 50--1000ms

## 🔮 Future Enhancements

-   Python/JS/C++\
-   User accounts\
-   Collaboration\
-   Debugger

## 📚 Educational Value

Technologies learned: Spring Boot, WebSocket, Docker, REST, full-stack
integration.

## 🏆 Conclusion

Project is complete and production-ready.
Date : 19 November 2025
Developer : Deniz Erdoğan
