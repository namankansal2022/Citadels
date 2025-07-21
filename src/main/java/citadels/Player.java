// File: src/main/java/citadels/Player.java
package citadels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a player in the Citadels game, encapsulating identity, resources,
 * hand of cards, built city, and selected character.
 */
public class Player {
    // Unique identifier for the player
    private int id;
    // Indicates if the player is human or AI-controlled
    private boolean isHuman;
    // Amount of gold the player currently holds
    private int gold;
    // List of district cards in the player's hand
    private List<District> hand;
    // List of district cards the player has built in their city
    private List<District> city;
    // ID of the character selected for the current round
    private int character;

    /**
     * Constructs a new player with the specified ID and type.
     *
     * @param id      unique identifier of the player
     * @param isHuman true if the player is controlled by a human, false for CPU
     */
    public Player(int id, boolean isHuman) {
        this.id = id;
        this.isHuman = isHuman;
        this.gold = 0;
        this.hand = new ArrayList<>();
        this.city = new ArrayList<>();
        this.character = 0;
    }

    /**
     * Returns the player's unique identifier.
     *
     * @return player ID
     */
    public int getId() {
        return id;
    }

    /**
     * Indicates whether this player is controlled by a human.
     *
     * @return true if human player, false if CPU
     */
    public boolean isHuman() {
        return isHuman;
    }

    /**
     * Returns the amount of gold currently held by the player.
     *
     * @return current gold count
     */
    public int getGold() {
        return gold;
    }

    /**
     * Returns the list of district cards in the player's hand.
     *
     * @return list of hand districts
     */
    public List<District> getHand() {
        return hand;
    }

    /**
     * Returns the list of district cards in the player's city.
     *
     * @return list of built city districts
     */
    public List<District> getCity() {
        return city;
    }

    /**
     * Returns the character ID selected by the player for the current round.
     *
     * @return character ID
     */
    public int getCharacter() {
        return character;
    }

    /**
     * Sets the character ID for the current round.
     *
     * @param character character ID to assign
     */
    public void setCharacter(int character) {
        this.character = character;
    }

    /**
     * Adds the specified amount of gold to the player's resources.
     *
     * @param amt amount of gold to add
     */
    public void addGold(int amt) {
        gold += amt;
    }

    /**
     * Attempts to spend the specified amount of gold.
     *
     * @param amt amount of gold to spend
     * @return true if the player had sufficient gold (and it was deducted),
     *         false otherwise
     */
    public boolean spendGold(int amt) {
        if (amt > gold) {
            return false;
        }
        gold -= amt;
        return true;
    }

    /**
     * Adds a district card to the player's hand.
     *
     * @param d district to add
     */
    public void addCardToHand(District d) {
        hand.add(d);
    }

    /**
     * Checks whether the player has already built a district with the given name.
     *
     * @param name name of the district to check
     * @return true if a built district matches the name (case-insensitive)
     */
    public boolean hasBuilt(String name) {
        return city.stream()
                .anyMatch(d -> d.getName().equalsIgnoreCase(name));
    }

    /**
     * Computes this player's final score at game end.
     *
     * @param firstCompleter true if this player was first to build ≥8 districts
     * @param completed      true if this player built ≥8 districts
     * @return total points based on district costs, color diversity bonus,
     *         completion bonus, and unique district bonuses
     */
    public int calculateScore(boolean firstCompleter, boolean completed) {
        int score = 0;
        Set<String> colorsSeen = new HashSet<>();

        // Add base cost and any bonus points from each district
        for (District d : city) {
            score += d.getCost();
            colorsSeen.add(d.getColor().toLowerCase());
            score += d.getPointBonus();
        }

        // Bonus for having all five district colors
        if (colorsSeen.containsAll(
                Arrays.asList("yellow", "blue", "green", "red", "purple"))) {
            score += 3;
        }

        // Bonus for completing the city (8+ districts)
        if (completed) {
            score += (firstCompleter ? 4 : 2);
        }

        return score;
    }

    /**
     * Returns a human-readable summary of the player's status.
     *
     * @return string summarizing hand size, gold, and built city
     */
    @Override
    public String toString() {
        String cityStr = city.isEmpty()
                ? ""
                : city.stream()
                .map(District::displayShort)
                .collect(Collectors.joining(", "));
        return "Player " + id
                + (isHuman ? " (you)" : "")
                + ": cards=" + hand.size()
                + " gold=" + gold
                + " city=" + cityStr;
    }
}
