**Concurrent Set Game**

The goal of this assignment is to practice concurrent programming in a Java environment.

**All user interface (UI), graphics, and keyboard handling have been pre-implemented**

**Game Rules**

The game consists of a deck of 81 cards, each containing a drawing with four features: color, number, shape, and shading.
The game starts with 12 drawn cards placed on a 3x4 grid on the table.
The objective is to find a combination of three cards from the table that constitute a "legal set."
A "legal set" is defined as a set of three cards where, for each feature (color, number, shape, shading), the three cards must display that feature as either all the same or all different.
Features' possible values:
Color: red, green, or purple.
Number of shapes: 1, 2, or 3.
Geometry of shapes: squiggle, diamond, or oval.
Shading of shapes: solid, partial, or empty.
Players aim to identify and mark legal sets by placing tokens on cards.
If a player correctly identifies a legal set, they receive a point and the cards forming the set are replaced with three new cards from the deck.
Periodically, if no legal sets are present, the dealer reshuffles the deck and draws new cards.
The game continues until no legal sets remain, at which point the player with the most points wins.
Player Actions
Each player controls 12 unique keys on the keyboard, corresponding to the 3x4 card slots on the table. Key presses signify actions to place or remove tokens from cards.
Player A - QWER
            ASDF
            ZXCV
Player B - UIOP
            JKL;
            M,./

**Player Types**

The game supports two player types: human and non-human.

Human Players: Input is taken from the physical keyboard.
Non-Human Players: Simulated by threads generating random key presses.

**Getting Started**

Clone this repository to your local machine.
Go to main and start the game.

**Contributing**

This project was developed by Yarden Levi and Bar Zuckerman.

**License**

This project is licensed as homework for BGU. All rights reserved.
