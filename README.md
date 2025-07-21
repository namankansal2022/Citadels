# Citadels-Inspired Turn-Based Strategy Game (Java CLI)

A command-line, multiplayer, turn-based strategy game inspired by the Citadels card game, featuring role selection, city-building, resource management, and AI-driven opponents.

## 🎮 Game Features

- Role selection phase with unique character abilities  
- City-building mechanics tied to strategy and economy  
- Resource management involving gold and district cards  
- AI players with rule-based logic for:
  - Strategic character selection
  - Gold vs. card decisions
  - Dynamic use of abilities
  - Building priorities and endgame logic  
- Game state persistence using JSON serialization (save/load support)  
- Interactive command-line interface with:
  - Rich command parsing
  - In-game help
  - Turn summaries and score breakdowns

## 🛠️ Technologies Used

- **Java** — Core logic and OOP-based game architecture  
- **Gradle** — Build automation and modular structure  
- **JUnit + JaCoCo** — Unit testing and code coverage analysis  
- **simple-json** — Lightweight JSON library for save/load functionality

## 🧱 Project Structure

Built with a modular design to clearly separate concerns such as game logic, input handling, AI strategy, and persistence.

## 🚀 Getting Started

Clone the repository and build using Gradle:

```bash
git clone https://github.com/your-username/citadels-strategy-game.git
cd citadels-strategy-game
./gradlew build
