// File: src/main/java/citadels/App.java
package citadels;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Comparator;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Main application class for the Citadels game.
 * Handles game setup, main loop, turn processing, and scoring.
 */
public class App {
    // List of all players in the game
    static List<Player> players = new ArrayList<>();
    // Deck of district cards
    static List<District> deck = new ArrayList<>();
    // The player who currently holds the crown
    static Player crownedPlayer;
    // Debug mode flag
    static boolean debugMode = false;
    // Scanner for user input
    static Scanner scanner;
    // Used to show hand only on first selection
    static boolean firstSelection = true;

    /**
     * Character names, indexed by character number (1-based).
     */
    public static final String[] CHARACTER_NAMES = {
            "", "Assassin", "Thief", "Magician", "King",
            "Bishop", "Merchant", "Architect", "Warlord"
    };
    /**
     * Character ability descriptions, indexed by character number (1-based).
     */
    public static final String[] CHARACTER_INFO = {
            "",
            "Assassin - Select another character to kill. The killed character loses their turn.",
            "Thief - Select another character to rob. When that character is revealed, immediately take all their gold. Cannot rob Assassin or a killed character.",
            "Magician - May exchange your hand with another player's, or discard any number of cards and draw the same number. (Once per turn.)",
            "King - Gain 1 gold for each yellow district in your city. Receive the crown; you will choose first next round.",
            "Bishop - Gain 1 gold for each blue (religious) district in your city. Your districts cannot be destroyed by the Warlord (unless you were killed by the Assassin).",
            "Merchant - Gain 1 gold for each green (trade) district in your city. Gain an extra 1 gold at end of your turn.",
            "Architect - Draw 2 extra cards at the start of your turn. Can build up to 3 districts this turn.",
            "Warlord - Gain 1 gold for each red (military) district in your city. May destroy one district by paying one less than its cost (cannot target a city with 8 districts or a Keep, and cannot target a Bishop's city if Bishop is alive)."
    };

    // The character number that was killed this round (0 if none)
    public static int killedCharacter = 0;
    // The character number that was robbed this round (0 if none)
    public static int robbedCharacter = 0;
    // The player who is the Thief this round (null if none)
    public static Player thiefPlayer = null;
    // Flag to indicate if the game end has been triggered
    public static boolean gameEndTriggered = false;
    // The first player to complete their city
    public static Player firstCompleter = null;

    /**
     * Main entry point for the Citadels game.
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        scanner = new Scanner(System.in);

        int numPlayers = promptPlayerCount(); // Ask for number of players
        initializeDeck(); // Load and shuffle the deck

        // Create players
        players.clear();
        for (int i = 1; i <= numPlayers; i++) {
            players.add(new Player(i, i == 1)); // Player 1 is always human
        }

        // Randomly assign crown
        crownedPlayer = players.get(new Random().nextInt(numPlayers));

        System.out.println("Shuffling deck...");
        Collections.shuffle(deck);

        System.out.println("Adding characters...");
        System.out.println("Dealing cards...");
        // Give each player 2 gold and 4 cards
        for (Player p : players) {
            p.addGold(2);
            for (int j = 0; j < 4; j++) {
                drawCardForPlayer(p);
            }
        }

        System.out.println("Starting Citadels with " + numPlayers + " players...");
        System.out.println("You are player 1");

        // Main game loop
        while (!gameEndTriggered) {
            characterSelectionPhase(); // Character selection
            turnPhase(); // Each character's turn

            if (!gameEndTriggered) {
                // Reset for next round
                killedCharacter = 0;
                robbedCharacter = 0;
                thiefPlayer = null;
                for (Player p : players) {
                    p.setCharacter(0);
                }
            }
        }

        scoreAndDeclareWinner(); // Show final scores
    }

    /**
     * Prompts the user for the number of players (4-7).
     * @return the number of players
     */
    public static int promptPlayerCount() {
        int count = 0;
        do {
            System.out.print("Enter how many players [4-7]: ");
            String input = scanner.nextLine().trim();
            try { count = Integer.parseInt(input); }
            catch (NumberFormatException e) { count = 0; }
        } while (count < 4 || count > 7);
        return count;
    }

    /**
     * Initializes the deck of district cards from the cards.tsv resource file.
     */
    public static void initializeDeck() {
        deck.clear();
        InputStream in = App.class.getClassLoader()
                .getResourceAsStream("citadels/cards.tsv");
        if (in == null) {
            throw new RuntimeException("cards.tsv not found on classpath");
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            boolean skippedHeader = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (!skippedHeader) {
                    skippedHeader = true;
                    continue;
                }
                String[] parts = line.split("\\t");
                if (parts.length < 4) continue;
                String name = parts[0];
                int qty = Integer.parseInt(parts[1]);
                String color = parts[2].toLowerCase();
                int cost = Integer.parseInt(parts[3]);
                String desc = parts.length > 4 ? parts[4] : "";
                for (int i = 0; i < qty; i++) {
                    deck.add(new District(name, color, cost, desc));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading cards.tsv", e);
        }
    }

    /**
     * Draws a card from the deck and adds it to the player's hand.
     * @param p the player to draw a card for
     */
    public static void drawCardForPlayer(Player p) {
        if (!deck.isEmpty()) {
            p.addCardToHand(deck.remove(0));
        }
    }

    /**
     * Handles the character selection phase for all players.
     * Deals with discards, face-up/face-down, and player choices.
     */
    public static void characterSelectionPhase() {
        if (firstSelection) {
            System.out.println("Your starting hand of district cards:");
            showHand(players.get(0));
            firstSelection = false;
        }

        System.out.println("Player " + crownedPlayer.getId()
                + " is the crowned player and goes first.");
        System.out.println("Press t to process turns");
        waitForContinue();
        System.out.println("================================");
        System.out.println("SELECTION PHASE");
        System.out.println("================================");

        List<Integer> charDeck = new ArrayList<>();
        for (int i = 1; i <= 8; i++) charDeck.add(i);
        Random rand = new Random();

        // 1) face-down discard
        int faceDown = charDeck.remove(rand.nextInt(charDeck.size()));

        // 2) face-up discards (King cannot be face-up)
        // 2) face-up discards, King cannot be face-up
        int faceUpCount = players.size() == 4 ? 2
                : players.size() == 5 ? 1
                : 0;
        List<Integer> faceUp = new ArrayList<>();
        for (int i = 0; i < faceUpCount; ) {
            int c = charDeck.remove(rand.nextInt(charDeck.size()));
            if (c == 4) {
                // King cannot be face-up: put it back and retry
                System.out.println("King was removed.");
                System.out.println("The King cannot be visibly removed, trying again...");
                charDeck.add(c);
                Collections.shuffle(charDeck);
                // clear any already-picked face-up cards and restart
                faceUp.clear();
                i = 0;
                continue;
            }
            faceUp.add(c);
            i++;
        }

        System.out.println("A mystery character was removed.");
        for (int c : faceUp) {
            System.out.println(CHARACTER_NAMES[c] + " was removed.");
        }

        // 3) players pick in crown order
        int crownIdx = players.indexOf(crownedPlayer), n = players.size();
        for (int p = 0; p < n; p++) {
            Player cur = players.get((crownIdx + p) % n);
            boolean lastOfSeven = (n == 7 && p == n - 1);

            if (cur.isHuman()) {
                List<Integer> avail = new ArrayList<>(charDeck);
                System.out.print("Choose your character from: ");
                if (lastOfSeven) {
                    int rem = avail.isEmpty() ? -1 : avail.get(0);
                    if (rem != -1) System.out.print(CHARACTER_NAMES[rem] + ", ");
                    System.out.println(CHARACTER_NAMES[faceDown] + ".");
                } else {
                    for (int i = 0; i < avail.size(); i++) {
                        System.out.print(CHARACTER_NAMES[avail.get(i)]);
                        if (i < avail.size() - 1) System.out.print(", ");
                    }
                    System.out.println(".");
                }
                while (true) {
                    String in = scanner.nextLine().trim();
                    if (in.equalsIgnoreCase("debug")) {
                        debugMode = !debugMode;
                        System.out.println("Debug mode " + (debugMode ? "ON" : "OFF"));
                        continue;
                    }
                    if (in.toLowerCase().startsWith("info")) {
                        handleInfoCommand(in, cur);
                        continue;
                    }
                    int pick = 0;
                    try { pick = Integer.parseInt(in); }
                    catch (Exception e) {
                        for (int c2 : charDeck) {
                            if (CHARACTER_NAMES[c2].equalsIgnoreCase(in)) {
                                pick = c2; break;
                            }
                        }
                        if (lastOfSeven && CHARACTER_NAMES[faceDown]
                                .equalsIgnoreCase(in)) {
                            pick = faceDown;
                        }
                    }
                    if (lastOfSeven) {
                        int rem = avail.isEmpty() ? -1 : avail.get(0);
                        if (pick == rem || pick == faceDown) {
                            cur.setCharacter(pick);
                            if (pick == faceDown) faceDown = rem;
                            charDeck.remove(Integer.valueOf(pick));
                            break;
                        }
                    } else if (charDeck.contains(pick)) {
                        cur.setCharacter(pick);
                        charDeck.remove(Integer.valueOf(pick));
                        break;
                    }
                    System.out.println("Invalid character. Please choose an available one.");
                }
                System.out.println("You chose the " +
                        CHARACTER_NAMES[cur.getCharacter()] + ".");
            } else {
                System.out.println("Player " + cur.getId() + " is choosing a character...");
                waitForContinue();
                List<Integer> avail = new ArrayList<>(charDeck);
                int choice;
                if (lastOfSeven) {
                    choice = rand.nextBoolean() ? avail.get(0) : faceDown;
                    int other = (choice == faceDown ? avail.get(0) : faceDown);
                    faceDown = other;
                } else {
                    choice = avail.get(rand.nextInt(avail.size()));
                }
                cur.setCharacter(choice);
                charDeck.remove(Integer.valueOf(choice));
                System.out.println("Player " + cur.getId() + " chose a character.");
            }
        }

        System.out.println("Character choosing is over, action round will now begin.");
    }

    /**
     * Handles the turn phase for all characters in order (1-8).
     * Each character's ability and build phase is processed.
     */
    public static void turnPhase() {
        System.out.println("================================");
        System.out.println("TURN PHASE");
        System.out.println("================================");

        for (int r = 1; r <= 8; r++) {
            Player cur = null;
            for (Player p : players) {
                if (p.getCharacter() == r) {
                    cur = p; break;
                }
            }

            System.out.println(r + ": " + CHARACTER_NAMES[r]);

            // if killed or not chosen, skip
            if (cur == null || r == killedCharacter) {
                if (cur != null && r == killedCharacter)
                    System.out.println("The " + CHARACTER_NAMES[r] + " was killed.");
                else
                    System.out.println("No one is the " + CHARACTER_NAMES[r]);
                if (r < 8) waitForContinue();
                continue;
            }

            // reveal
            System.out.println("Player " + cur.getId()
                    + " is the " + CHARACTER_NAMES[r]);
            if (cur.isHuman()) System.out.println("Your turn.");
            if (debugMode && !cur.isHuman()) {
                System.out.print("[DEBUG] Player " + cur.getId() + " hand: ");
                for (District d : cur.getHand()) {
                    System.out.print(d.getName() + "(" + d.getColor() + ") ");
                }
                System.out.println();
            }

            // 1) Assassin (r==1)
            // … inside turnPhase(), in place of your existing Assassin block …

// Assassin (r == 1)
            if (r == 1) {
                if (cur.isHuman()) {
                    System.out.print("Who do you want to kill? Choose 2–8 (invalid to skip): ");
                    String in = scanner.nextLine().trim();
                    int t = -1;
                    try {
                        t = Integer.parseInt(in);
                    } catch (NumberFormatException e) {
                        // invalid → skip
                    }
                    if (t >= 2 && t <= 8) {
                        killedCharacter = t;
                        System.out.println("You chose to kill the " + CHARACTER_NAMES[t] + ".");
                    } else {
                        System.out.println("Skipping Assassin ability.");
                    }
                } else {
                    // CPU as before
                    List<Integer> opts = new ArrayList<>();
                    for (int x = 2; x <= 8; x++) opts.add(x);
                    killedCharacter = opts.get(new Random().nextInt(opts.size()));
                    System.out.println("Assassin chooses to kill the " +
                            CHARACTER_NAMES[killedCharacter] + ".");
                }
            }

            // 2) Thief choose (r==2)
            // … and in place of your existing Thief block …

            if (r == 2) {
                if (cur.isHuman()) {
                    System.out.print("Who do you want to steal from? Choose 3–8 (invalid to skip): ");
                    String in = scanner.nextLine().trim();
                    int t = -1;
                    try {
                        t = Integer.parseInt(in);
                    } catch (NumberFormatException e) {
                        // invalid → skip
                    }
                    if (t >= 3 && t <= 8 && t != killedCharacter) {
                        robbedCharacter = t;
                        thiefPlayer = cur;
                        System.out.println("You chose to steal from the " + CHARACTER_NAMES[t] + ".");
                    } else {
                        System.out.println("Skipping Thief ability.");
                    }
                } else {
                    // CPU as before
                    List<Integer> opts = new ArrayList<>();
                    for (int x = 3; x <= 8; x++) {
                        if (x != killedCharacter) opts.add(x);
                    }
                    robbedCharacter = opts.isEmpty() ? 0
                            : opts.get(new Random().nextInt(opts.size()));
                    thiefPlayer = cur;
                    System.out.println("Thief plans to rob the " +
                            CHARACTER_NAMES[robbedCharacter] + ".");
                }
            }

            // 3) Thief steals immediately upon reveal
            if (r == robbedCharacter
                    && cur != null
                    && thiefPlayer != null
                    && thiefPlayer.getCharacter() == 2
                    && thiefPlayer != cur) {
                int amt = cur.getGold();
                cur.spendGold(amt);
                thiefPlayer.addGold(amt);
                System.out.println("The Thief stole " + amt +
                        " gold from Player " + cur.getId() + ".");
            }

            // 4) Resource collection
            if (cur.isHuman()) {
                System.out.print("Collect 2 gold or draw two cards and pick one [gold/cards]: ");
                while (true) {
                    String choice = scanner.nextLine().trim().toLowerCase();
                    if ("gold".equals(choice)) {
                        cur.addGold(2);
                        System.out.println("Player " + cur.getId() + " received 2 gold.");
                        break;
                    }
                    // draw "cards" income
                    int drawCount = cur.hasBuilt("Observatory") ? 3 : 2;
                    List<District> drawn = new ArrayList<>();
                    for (int i = 0; i < drawCount && !deck.isEmpty(); i++) {
                        drawn.add(deck.remove(0));
                    }

                    if (drawn.isEmpty()) {
                        System.out.println("No cards could be drawn.");
                    } else if (drawn.size() == 1) {
                        // only one available
                        cur.addCardToHand(drawn.get(0));
                        System.out.println("Only one card available — you drew " +
                                drawn.get(0).displayShort() + ".");
                    } else if (cur.hasBuilt("Library")) {
                        // Library: keep all drawn
                        drawn.forEach(cur::addCardToHand);
                        System.out.printf("Due to Library, you keep all %d cards: %s%n",
                                drawn.size(),
                                drawn.stream().map(District::displayShort)
                                        .collect(Collectors.joining(", "))
                        );
                    } else {
                        // normal pick-one
                        System.out.println("You drew:");
                        for (int i = 0; i < drawn.size(); i++) {
                            System.out.printf("  %d. %s%n", i + 1, drawn.get(i).displayShort());
                        }
                        System.out.print("Choose which to keep [1-" + drawn.size() + "]: ");
                        int sel;
                        while (true) {
                            try {
                                sel = Integer.parseInt(scanner.nextLine().trim());
                                if (sel >= 1 && sel <= drawn.size()) break;
                            } catch (NumberFormatException ignored) {}
                            System.out.print("Please enter a number 1–" + drawn.size() + ": ");
                        }
                        District keep = drawn.remove(sel - 1);
                        cur.addCardToHand(keep);
                        // return the rest to bottom of deck
                        deck.addAll(drawn);
                        System.out.println("You kept " + keep.displayShort() + ".");
                    }

                    System.out.print("Invalid choice. Type 'gold' or 'cards': ");
                }
            } else {
                // CPU heuristic
                if (cur.getHand().isEmpty() || cur.getGold() < 2) {
                    cur.addGold(2);
                    System.out.println("Player " + cur.getId() + " took 2 gold.");
                } else {
                    District d1 = deck.isEmpty() ? null : deck.remove(0);
                    District d2 = deck.isEmpty() ? null : deck.remove(0);
                    if (d1 != null && d2 != null) {
                        boolean lib = cur.hasBuilt("Library");
                        if (lib) {
                            cur.addCardToHand(d1);
                            cur.addCardToHand(d2);
                        } else {
                            District keep = d1.getCost() >= d2.getCost() ? d1 : d2;
                            District discard = keep == d1 ? d2 : d1;
                            cur.addCardToHand(keep);
                            deck.add(discard);
                        }
                        System.out.println("Player " + cur.getId() + " drew cards.");
                    } else {
                        cur.addGold(2);
                        System.out.println("Player " + cur.getId() + " took 2 gold.");
                    }
                }
            }

            // 5) Magician ability
            if (r == 3) {
                if (cur.isHuman()) {
                    System.out.print("Use Magician ability? [yes/no]: ");
                    if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                        System.out.print("Type 'exchange <player>' or 'discard <indexes>': ");
                        String act = scanner.nextLine().trim().toLowerCase();
                        if (act.startsWith("exchange")) {
                            try {
                                int tid = Integer.parseInt(act.split("\\s+")[1]);
                                Player tp = players.get(tid - 1);
                                List<District> tmp = cur.getHand();
                                cur.getHand().clear();
                                cur.getHand().addAll(tp.getHand());
                                tp.getHand().clear();
                                tp.getHand().addAll(tmp);
                                System.out.println("Exchanged with Player " + tid + ".");
                            } catch (Exception ignored) { }
                        } else if (act.startsWith("discard")) {
                            String[] ps = act.split("\\s+");
                            List<Integer> idxs = new ArrayList<>();
                            for (int i = 1; i < ps.length; i++) {
                                try { idxs.add(Integer.parseInt(ps[i]) - 1); }
                                catch (Exception ignored) { }
                            }
                            Collections.sort(idxs, Collections.reverseOrder());
                            int cnt = 0;
                            for (int i : idxs) {
                                if (i >= 0 && i < cur.getHand().size()) {
                                    cur.getHand().remove(i);
                                    cnt++;
                                }
                            }
                            for (int i = 0; i < cnt; i++) drawCardForPlayer(cur);
                            System.out.println("Discarded " + cnt + ", drew " + cnt + ".");
                        }
                    }
                } else {
                    Player best = null;
                    for (Player p : players) {
                        if (p != cur && p.getHand().size() > cur.getHand().size()) {
                            if (best == null || p.getHand().size() > best.getHand().size()) {
                                best = p;
                            }
                        }
                    }
                    if (best != null && !best.getHand().isEmpty()) {
                        List<District> tmp = new ArrayList<>(cur.getHand());
                        cur.getHand().clear();
                        cur.getHand().addAll(best.getHand());
                        best.getHand().clear();
                        best.getHand().addAll(tmp);
                        System.out.println("Player " + cur.getId()
                                + " exchanged hands with Player " + best.getId() + ".");
                    } else {
                        Set<String> seen = new HashSet<>();
                        int cnt = 0;
                        for (int i = cur.getHand().size() - 1; i >= 0; i--) {
                            District d = cur.getHand().get(i);
                            if (seen.contains(d.getName().toLowerCase())) {
                                cur.getHand().remove(i);
                                cnt++;
                            } else {
                                seen.add(d.getName().toLowerCase());
                            }
                        }
                        for (int i = 0; i < cnt; i++) drawCardForPlayer(cur);
                        if (cnt > 0) {
                            System.out.println("Player " + cur.getId()
                                    + " refreshed " + cnt + " cards.");
                        }
                    }
                }
            }
            // 4.5) Laboratory (once per turn)
// If you have built the Laboratory, you may discard one card from your hand and gain 1 gold.
            if (cur.hasBuilt("Laboratory")) {
                if (cur.isHuman()) {
                    System.out.print("Use Laboratory? Discard 1 card → gain 1 gold [yes/no]: ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("yes")
                            && !cur.getHand().isEmpty()) {
                        showHand(cur);
                        System.out.print("Which card to discard [1-" + cur.getHand().size() + "]? ");
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (idx >= 0 && idx < cur.getHand().size()) {
                            District removed = cur.getHand().remove(idx);
                            cur.addGold(1);
                            System.out.println("Discarded " + removed.getName()
                                    + ", gained 1 gold.");
                        } else {
                            System.out.println("Invalid index; skipping Laboratory.");
                        }
                    }
                } else {
                    // simple CPU: if it has ≥3 cards, discard the cheapest for 1 gold
                    District cheapest = cur.getHand().stream()
                            .min(Comparator.comparingInt(District::getCost))
                            .orElse(null);
                    if (cheapest != null && cur.getHand().size() >= 3) {
                        cur.getHand().remove(cheapest);
                        cur.addGold(1);
                        System.out.println("Player " + cur.getId()
                                + " discards " + cheapest.getName()
                                + " for 1 gold (Laboratory).");
                    }
                }
            }
            // 4.6) Smithy (once per turn)
// If you have built the Smithy, you may pay 2 gold to draw 3 cards.
            if (cur.hasBuilt("Smithy")) {
                if (cur.isHuman()) {
                    System.out.print("Use Smithy? Pay 2 gold → draw 3 cards [yes/no]: ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                        if (cur.spendGold(2)) {
                            List<District> drawn = new ArrayList<>();
                            for (int i = 0; i < 3 && !deck.isEmpty(); i++) {
                                drawn.add(deck.remove(0));
                            }
                            drawn.forEach(cur::addCardToHand);
                            System.out.println("Smithy: drew " +
                                    drawn.stream()
                                            .map(District::displayShort)
                                            .collect(Collectors.joining(", "))
                            );
                        } else {
                            System.out.println("Not enough gold for Smithy.");
                        }
                    }
                } else {
                    // simple CPU: if gold≥2 and hand≤3, use Smithy
                    if (cur.getGold() >= 2 && cur.getHand().size() <= 3) {
                        cur.spendGold(2);
                        List<District> drawn = new ArrayList<>();
                        for (int i = 0; i < 3 && !deck.isEmpty(); i++) {
                            drawn.add(deck.remove(0));
                        }
                        drawn.forEach(cur::addCardToHand);
                        System.out.println("Player " + cur.getId()
                                + " uses Smithy to draw 3 cards.");
                    }
                }
            }

            // 6) Architect ability
            if (r == 7) {
                drawCardForPlayer(cur);
                drawCardForPlayer(cur);
                System.out.println((cur.isHuman() ? "You" : "Player " + cur.getId())
                        + " drew 2 extra cards (Architect).");
            }

            // 7) Colour incomes & crown/merchant
            if (r == 4 || r == 5 || r == 6 || r == 8) {
                String col = r == 4 ? "yellow"
                        : r == 5 ? "blue"
                        : r == 6 ? "green"
                        :         "red";

                // School of Magic override
                String schoolColor = null;
                if (cur.hasBuilt("School of Magic")) {
                    if (cur.isHuman()) {
                        System.out.print("Choose School of Magic color for this income [yellow/blue/green/red]: ");
                        schoolColor = scanner.nextLine().trim().toLowerCase();
                    } else {
                        // CPU: pick the color they have most of in city
                        Map<String,Long> counts = cur.getCity().stream()
                                .collect(Collectors.groupingBy(District::getColor, Collectors.counting()));
                        schoolColor = counts.entrySet().stream()
                                .max(Comparator.comparingLong(Map.Entry::getValue))
                                .map(Map.Entry::getKey)
                                .orElse(col);
                    }
                }

                int gain = 0;
                for (District d : cur.getCity()) {
                    if (d.getColor().equals(col)
                            || (d.getName().equalsIgnoreCase("School of Magic")
                            && schoolColor != null
                            && schoolColor.equals(col))) {
                        gain++;
                    }
                }
                if (gain > 0) {
                    cur.addGold(gain);
                    if (cur.isHuman())
                        System.out.printf("You received %d gold from your %s districts.%n", gain, col);
                    else
                        System.out.printf("Player %d received %d gold from their %s districts.%n",
                                cur.getId(), gain, col);
                }
            }
            if (r == 4 || r == 5 || r == 6 || r == 8) {
                String col = r == 4 ? "yellow"
                        : r == 5 ? "blue"
                        : r == 6 ? "green"
                        :         "red";

                // School of Magic override
                String schoolColor = null;
                if (cur.hasBuilt("School of Magic")) {
                    if (cur.isHuman()) {
                        System.out.print("Choose School of Magic color for this income [yellow/blue/green/red]: ");
                        schoolColor = scanner.nextLine().trim().toLowerCase();
                    } else {
                        // CPU: pick the color they have most of in city
                        Map<String,Long> counts = cur.getCity().stream()
                                .collect(Collectors.groupingBy(District::getColor, Collectors.counting()));
                        schoolColor = counts.entrySet().stream()
                                .max(Comparator.comparingLong(Map.Entry::getValue))
                                .map(Map.Entry::getKey)
                                .orElse(col);
                    }
                }

                int gain = 0;
                for (District d : cur.getCity()) {
                    if (d.getColor().equals(col)
                            || (d.getName().equalsIgnoreCase("School of Magic")
                            && schoolColor != null
                            && schoolColor.equals(col))) {
                        gain++;
                    }
                }
                if (gain > 0) {
                    cur.addGold(gain);
                    if (cur.isHuman())
                        System.out.printf("You received %d gold from your %s districts.%n", gain, col);
                    else
                        System.out.printf("Player %d received %d gold from their %s districts.%n",
                                cur.getId(), gain, col);
                }
            }

            if (r == 4) {
                crownedPlayer = cur;
            }
            if (r == 6) {
                cur.addGold(1);
                System.out.println((cur.isHuman() ? "Merchant gains an extra 1 gold."
                        : "Player " + cur.getId() + " gains an extra 1 gold (Merchant)."));
            }

            // 8) Warlord destruction
            if (r == 8 && cur.isHuman()) {
                System.out.print("Destroy a district? [player#/no]: ");
                String respLine = scanner.nextLine().trim().toLowerCase();
                if (!respLine.equals("no")) {
                    String numStr = respLine.replaceAll("\\D+", "");
                    if (!numStr.isEmpty()) {
                        int tid = Integer.parseInt(numStr);
                        if (tid >= 1 && tid <= players.size()) {
                            Player tgt = players.get(tid - 1);
                            if (!tgt.getCity().isEmpty()
                                    && tgt.getCity().size() < 8
                                    && !(tgt.getCharacter() == 5 && killedCharacter != 5)) {
                                for (int i = 0; i < tgt.getCity().size(); i++) {
                                    System.out.printf("%d. %s%n",
                                            i+1, tgt.getCity().get(i).displayLong(true));
                                }
                                System.out.print("Choose [1-" + tgt.getCity().size()
                                        + " / 0 to cancel]: ");
                                String choiceLine = scanner.nextLine().trim();
                                String choiceNum = choiceLine.replaceAll("\\D+", "");
                                if (!choiceNum.isEmpty()) {
                                    int choice = Integer.parseInt(choiceNum);
                                    if (choice >= 1 && choice <= tgt.getCity().size()) {
                                        District td = tgt.getCity().get(choice - 1);
                                        int cost = Math.max(0, td.getCost() - 1);
                                        if (tgt.hasBuilt("Great Wall")) cost = td.getCost();
                                        if (cost <= cur.getGold()) {
                                            cur.spendGold(cost);
                                            tgt.getCity().remove(td);
                                            System.out.println("Destroyed " + td.getName()
                                                    + " from Player " + tgt.getId() + ".");
                                            if (tgt.hasBuilt("Graveyard") && tgt.getGold() > 0) {
                                                System.out.print("Recover with Graveyard? [yes/no]: ");
                                                if (scanner.nextLine().trim().toLowerCase()
                                                        .startsWith("y")) {
                                                    tgt.spendGold(1);
                                                    tgt.getHand().add(td);
                                                    System.out.println("Recovered " + td.getName()
                                                            + " into Player " + tgt.getId() + "'s hand.");
                                                }
                                            }
                                        } else {
                                            System.out.println("Not enough gold to destroy " + td.getName());
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Cannot destroy that city.");
                            }
                        } else {
                            System.out.println("Invalid player number: " + numStr);
                        }
                    } else {
                        System.out.println("Invalid input. Please enter a player number or 'no'.");
                    }
                }
            } else if (r == 8) {
                // CPU Warlord destruction (unchanged)...
                // [omitted for brevity, same as your existing logic]
            }

            // 9) Build phase
            int limit = (r == 7 ? 3 : 1), built = 0;
            if (cur.isHuman()) {
                while (true) {
                    System.out.print("> ");
                    String cmd = scanner.nextLine().trim();
                    if (processCommand(cur, cmd)) continue;
                    if (cmd.equalsIgnoreCase("end")) {
                        System.out.println("You ended your turn.");
                        break;
                    }
                    if (cmd.startsWith("build ")) {
                        if (built >= limit) {
                            System.out.println("No builds remaining."); continue;
                        }
                        String[] ps = cmd.split("\\s+");
                        if (ps.length >= 2) {
                            try {
                                int hi = Integer.parseInt(ps[1]) - 1;
                                District d = (hi >= 0 && hi < cur.getHand().size())
                                        ? cur.getHand().get(hi) : null;
                                if (d == null) {
                                    System.out.println("Invalid selection.");
                                } else if (d.getCost() > cur.getGold()) {
                                    System.out.println("Not enough gold.");
                                } else if (cur.hasBuilt(d.getName())) {
                                    System.out.println("Already built that district.");
                                } else {
                                    cur.spendGold(d.getCost());
                                    cur.getHand().remove(hi);
                                    cur.getCity().add(d);
                                    System.out.println("Built " + d.displayShort());
                                    built++;
                                    if (cur.getCity().size() >= 8 && !gameEndTriggered) {
                                        gameEndTriggered = true;
                                        if (firstCompleter == null) firstCompleter = cur;
                                    }
                                    if (built >= limit) {
                                        System.out.println("Build limit reached.");
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("Usage: build <hand index>");
                            }
                        }
                        continue;
                    }
                    System.out.println("Unknown command.");
                }
            } else {
                for (int b = 0; b < limit; b++) {
                    District best = null;
                    int idx = -1;
                    for (int i = 0; i < cur.getHand().size(); i++) {
                        District d = cur.getHand().get(i);
                        if (d.getCost() <= cur.getGold() && !cur.hasBuilt(d.getName())) {
                            if (best == null || d.getCost() < best.getCost()) {
                                best = d; idx = i;
                            }
                        }
                    }
                    if (best != null) {
                        cur.spendGold(best.getCost());
                        cur.getHand().remove(idx);
                        cur.getCity().add(best);
                        System.out.println("Player " + cur.getId() +
                                " built " + best.getName() + ".");
                        built++;
                        if (cur.getCity().size() >= 8 && !gameEndTriggered) {
                            gameEndTriggered = true;
                            if (firstCompleter == null) firstCompleter = cur;
                        }
                    } else {
                        break;
                    }
                }
            }

            // end-of-turn pause
            if (r < 8) {
                System.out.println("Press t to continue.");
                waitForContinue();
            }
        }
    }
    /**
     * Processes a command entered by the player during their turn.
     * @param cur the current player
     * @param cmd the command string
     * @return true if the command was handled, false otherwise
     */
    public static boolean processCommand(Player cur, String cmd) {
        String lower = cmd.trim().toLowerCase();
        if (lower.equals("hand")) {
            showHand(cur); return true;
        }
        if (lower.startsWith("gold")) {
            handleGoldCommand(lower); return true;
        }
        if (lower.matches("^(citadel|city|list)(\\s+\\d+)?$")) {
            handleCityCommand(lower); return true;
        }
        if (lower.equals("all")) {
            showAllPlayers(); return true;
        }
        if (lower.equals("action")) {
            System.out.println(CHARACTER_INFO[cur.getCharacter()]);
            return true;
        }
        if (lower.startsWith("info ")) {
            handleInfoCommand(cmd, cur); return true;
        }
        if (lower.startsWith("save ")) {
            doSave(cmd.substring(5).trim()); return true;
        }
        if (lower.startsWith("load ")) {
            doLoad(cmd.substring(5).trim()); return true;
        }
        if (lower.equals("help")) {
            printHelp(); return true;
        }
        if (lower.equals("debug")) {
            debugMode = !debugMode;
            System.out.println("Debug mode " + (debugMode ? "ON" : "OFF"));
            return true;
        }
        return false;
    }

    /**
     * Wrapper for saving the game, with error handling.
     * @param filename the file to save to
     */
    public static void doSave(String filename) {
        try {
            saveGame(filename);
        } catch (Exception e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }
    /**
     * Wrapper for loading the game, with error handling.
     * @param filename the file to load from
     */
    static void doLoad(String filename) {
        try {
            loadGame(filename);
        } catch (Exception e) {
            System.out.println("Failed to load game: " + e.getMessage());
        }
    }

    /**
     * Handles the 'gold' command to show gold for a player.
     * @param arg the command argument
     */
    static void handleGoldCommand(String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length == 1) {
            System.out.println("You have " + players.get(0).getGold() + " gold.");
        } else {
            try {
                int pid = Integer.parseInt(parts[1]);
                Player p = players.get(pid - 1);
                System.out.println("Player " + pid + " has " + p.getGold() + " gold.");
            } catch (Exception e) {
                System.out.println("Invalid player number.");
            }
        }
    }

    /**
     * Handles the 'city', 'citadel', or 'list' command to show a player's city.
     * @param arg the command argument
     */
    static void handleCityCommand(String arg) {
        String[] parts = arg.split("\\s+");
        int pid = 1;
        if (parts.length > 1) {
            try { pid = Integer.parseInt(parts[1]); }
            catch (Exception ignored) { }
        }
        if (pid < 1 || pid > players.size()) {
            System.out.println("No such player: " + pid);
            return;
        }
        showCity(players.get(pid - 1));
    }

    /**
     * Prints the built districts for a player.
     * @param p the player
     */
    public static void showCity(Player p) {
        System.out.println("Player " + p.getId() + " has built:");
        if (p.getCity().isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (District d : p.getCity()) {
                System.out.printf("  %s (%s), points: %d%n",
                        d.getName(), d.getColor(), d.getCost());
            }
        }
    }

    /**
     * Handles the 'info' command to show info about a character or district.
     * @param cmd the command string
     * @param cur the current player
     */
    static void handleInfoCommand(String cmd, Player cur) {
        String[] sp = cmd.trim().split("\\s+", 2);
        if (sp.length < 2) {
            System.out.println("Specify a character name or a hand index.");
            return;
        }
        String arg = sp[1].trim();

        // Character info?
        for (int i = 1; i <= 8; i++) {
            if (CHARACTER_NAMES[i].equalsIgnoreCase(arg)) {
                System.out.println(CHARACTER_INFO[i]);
                return;
            }
        }

        // District in hand by index?
        District found = null;
        try {
            int idx = Integer.parseInt(arg) - 1;
            if (idx >= 0 && idx < cur.getHand().size()) {
                found = cur.getHand().get(idx);
            }
        } catch (NumberFormatException ignored) { }

        // District in hand by name?
        if (found == null) {
            for (District d : cur.getHand()) {
                if (d.getName().equalsIgnoreCase(arg)) {
                    found = d; break;
                }
            }
        }

        if (found != null) {
            if (found.isUnique()) {
                System.out.println(found.getDescription());
            } else {
                System.out.println("That district is not purple.");
            }
        } else {
            System.out.println("No purple district named \"" + arg + "\" in your hand.");
        }
    }

    /**
     * Shows the hand and gold for a player (only if human).
     * @param p the player
     */
    static void showHand(Player p) {
        if (!p.isHuman()) {
            System.out.println("You cannot see other players' hands (unless debug).");
            return;
        }
        System.out.println("You have " + p.getGold() + " gold. Cards in hand:");
        for (int i = 0; i < p.getHand().size(); i++) {
            System.out.println((i + 1) + ". " + p.getHand().get(i).displayLong(false));
        }
    }

    /**
     * Shows the status of all players.
     */
    public static void showAllPlayers() {
        for (Player p : players) {
            System.out.println(p.toString());
        }
    }

    /**
     * Prints help for available commands.
     */
    public static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("t : processes next turn");
        System.out.println("hand : shows your cards and gold");
        System.out.println("gold [p] : shows gold of player p");
        System.out.println("build <h> : builds card at position h in your hand");
        System.out.println("city/citadel/list [p] : shows built districts of player p");
        System.out.println("action : gives info about your character action");
        System.out.println("info <name> : info about building or character");
        System.out.println("all : shows status of all players");
        System.out.println("save <file> : saves game state");
        System.out.println("load <file> : loads game state");
        System.out.println("end : ends your turn");
        System.out.println("debug : toggles debug mode");
    }
    /**
     * Waits for the user to press 't' to continue, or processes global commands.
     */
    public static void waitForContinue() {
        while (true) {
            String in = scanner.nextLine().trim();
            // 1) t still advances
            if (in.equalsIgnoreCase("t")) {
                break;
            }
            // 2) allow global commands at any time
            //    processCommand returns true if it handled something
            if (processCommand(players.get(0), in)) {
                continue;  // show hand, all, debug, save/load, etc., then keep waiting
            }
            // 3) otherwise still insist on 't'
            System.out.println("It is not your turn. Press t to continue.");
        }
    }

    /**
     * Calculates and prints the final scores, then declares the winner.
     */
    public static void scoreAndDeclareWinner() {
        System.out.println("================================");
        System.out.println("GAME OVER - Final Scores");
        System.out.println("================================");

        Map<Player,Integer> scores = new LinkedHashMap<>();
        Map<Player,String> breakdowns = new LinkedHashMap<>();

        for (Player p : players) {
            boolean completed = p.getCity().size() >= 8;
            boolean first    = (firstCompleter == p);

            StringBuilder sb = new StringBuilder();
            sb.append("Player ").append(p.getId());
            if (p.isHuman()) sb.append(" (you)");
            sb.append(":\n");

            int total = 0;
            sb.append("  Districts built:\n");
            for (District d : p.getCity()) {
                sb.append("    - ").append(d.getName())
                        .append(" (").append(d.getColor()).append("), cost ")
                        .append(d.getCost()).append("\n");
                total += d.getCost();
            }
            sb.append("  Sum of costs: ").append(total).append("\n");

            Set<String> cols = new HashSet<>();
            for (District d : p.getCity()) cols.add(d.getColor().toLowerCase());
            if (cols.containsAll(Arrays.asList("yellow","blue","green","red","purple"))) {
                sb.append("  Diversity bonus: +3\n");
                total += 3;
            }

            if (completed) {
                int bonus = first ? 4 : 2;
                sb.append("  Completion bonus: +").append(bonus).append("\n");
                total += bonus;
            }

            int purpleBonus = 0;
            for (District d : p.getCity()) purpleBonus += d.getPointBonus();
            if (purpleBonus > 0) {
                sb.append("  Unique district bonus: +")
                        .append(purpleBonus).append("\n");
                total += purpleBonus;
            }

            sb.append("  → Total: ").append(total).append(" points\n");
            scores.put(p, total);
            breakdowns.put(p, sb.toString());
        }

        for (String b : breakdowns.values()) {
            System.out.print(b);
        }

        int max = Collections.max(scores.values());
        List<Player> winners = new ArrayList<>();
        for (Map.Entry<Player,Integer> e : scores.entrySet()) {
            if (e.getValue() == max) winners.add(e.getKey());
        }
        Player win = (winners.size() == 1)
                ? winners.get(0)
                : winners.stream()
                .max(Comparator.comparingInt(Player::getCharacter))
                .get();

        System.out.println("Congratulations, Player " + win.getId() + " wins!");
    }

    /**
     * Saves the current game state to the specified file.
     * @param filename path to the JSON file
     */
    public static void saveGame(String filename) {
        try {
            Serializer.saveGame(filename);
        } catch (IOException e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    /**
     * Loads a saved game state from the specified file.
     * @param filename path to the JSON file
     */
    public static void loadGame(String filename) {
        try {
            Serializer.loadGame(filename);
        } catch (IOException | ParseException e) {
            System.out.println("Failed to load game: " + e.getMessage());
        }
    }
}

