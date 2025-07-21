// File: src/main/java/citadels/District.java
package citadels;

public class District {
    // Name of the district
    private String name;
    // Color category of the district (e.g., red, blue, green, purple)
    private String color;
    // Cost to build the district
    private int cost;
    // Optional description of the district's effect or lore
    private String description;

    // Constructor to initialize a District object
    public District(String name, String color, int cost, String desc) {
        this.name = name;
        this.color = color.toLowerCase(); // Normalize color to lowercase
        this.cost = cost;
        this.description = desc;
    }

    // Getter for the district's name
    public String getName() {
        return name;
    }

    // Getter for the district's color
    public String getColor() {
        return color;
    }

    // Getter for the district's cost
    public int getCost() {
        return cost;
    }

    // Getter for the district's description
    public String getDescription() {
        return description;
    }

    /**
     * Returns true if this is a “purple” (unique) district.
     */
    public boolean isUnique() {
        return "purple".equalsIgnoreCase(color);
    }

    /**
     * Returns any extra end-of-game points this district grants.
     * E.g. University and Dragon Gate each grant +2 at scoring time.
     */
    public int getPointBonus() {
        if (!isUnique()) {
            return 0;
        }
        String ln = name.toLowerCase();
        if (ln.contains("university") || ln.contains("dragon gate")) {
            return 2;
        }
        // add other unique end-game bonuses here if needed
        return 0;
    }

    /**
     * Short display used in city printouts and logs.
     * E.g. "Watchtower [red1]"
     */
    public String displayShort() {
        return name + " [" + color + cost + "]";
    }

    /**
     * Longer display, showing either cost or points depending on context.
     *
     * @param forCity if true, shows "points: cost", otherwise "cost: cost"
     */
    public String displayLong(boolean forCity) {
        if (forCity) {
            return name + " (" + color + "), points: " + cost;
        } else {
            return name + " (" + color + "), cost: " + cost;
        }
    }
}
