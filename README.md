# 2D Cooperative Multiplayer Game

A lightweight 2D tile-based cooperative multiplayer game developed in Java, utilizing a client-server architecture with native TCP/IP sockets and multithreading for concurrent gameplay synchronization.

---

## 📌 Project Overview

This project was built to explore and implement foundational concepts of distributed systems and network programming in game development, focusing on:
- Establishing persistent low-latency communication channels between multiple game clients and a central server.
- Managing concurrent player sessions without blocking server execution loops.
- Synchronizing player states, positions, and interactions across a shared grid-based map.

---

## 🚀 Key Architectural Features

- **Client-Server Architecture:** Centralized server managing game sessions, client validation, and authoritative broadcast messages.
- **Multithreaded Network Handling:** Dedicated client-handling threads on the server side to support concurrent incoming packet streams.
- **Socket Communication:** Built directly with native Java `Socket` and `ServerSocket` APIs over TCP/IP.
- **Tile-Based Movement & Collision:** Grid-aligned coordinates with validation rules preventing invalid movements and out-of-bounds navigation.
- **Object-Oriented Design (OOP):** Modular separation between network layers, game entities, and rendering/display logic.

---

## 🛠️ Tech Stack

- **Language:** Java (JDK 17+)
- **Networking:** Java Sockets (`java.net`), TCP/IP
- **Concurrency:** Java Threads & Runnables (`java.util.concurrent`)
- **IDE:** IntelliJ IDEA / VS Code

---

## ⚙️ Architecture & Data Flow

1. **Server Initialization:** The server binds to a specified network port and enters an infinite listening loop (`accept()`).
2. **Client Handshake:** When a player joins, a new thread is spawned exclusively to handle incoming input streams from that specific client.
3. **State Broadcast:** Whenever an action (movement, input event) occurs, the server processes the event, updates the canonical world state, and broadcasts delta packets to all connected peers.
4. **Disconnection Handling:** Graceful disconnect detection to free up sockets and inform active players.

---

## 📦 How to Run

### Prerequisites
- Java Development Kit (JDK) installed (`java -version`).

### 1. Compile the Project
Open terminal in the project root directory:
```bash
javac -d bin src/**/*.java
