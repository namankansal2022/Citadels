package citadels;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {
    /**
     * Test saving and loading with all fields populated.
     */
    @Test
    void saveAndLoadGame_fullState() throws Exception {
        Player p = new Player(1, true);
        p.addGold(3);
        p.addCardToHand(new District("Tavern", "green", 1, "desc"));
        p.getCity().add(new District("Cathedral", "blue", 5, "desc"));
        App.players.clear();
        App.players.add(p);
        App.deck.clear();
        App.deck.add(new District("Market", "green", 2, "desc"));
        App.crownedPlayer = p;
        App.firstCompleter = p;
        App.gameEndTriggered = true;
        App.debugMode = true;

        String filename = "test_full_save.json";
        Serializer.saveGame(filename);
        // Clear state
        App.players.clear();
        App.deck.clear();
        App.crownedPlayer = null;
        App.firstCompleter = null;
        App.gameEndTriggered = false;
        App.debugMode = false;

        Serializer.loadGame(filename);

        assertEquals(1, App.players.size());
        assertEquals(1, App.deck.size());
        assertEquals(1, App.crownedPlayer.getId());
        assertEquals(1, App.firstCompleter.getId());
        assertTrue(App.gameEndTriggered);
        assertTrue(App.debugMode);

        // Cleanup
        new File(filename).delete();
    }

    /**
     * Test saving and loading with firstCompleter == null.
     */
    @Test
    void saveAndLoadGame_firstCompleterNull() throws Exception {
        Player p = new Player(1, true);
        App.players.clear();
        App.players.add(p);
        App.deck.clear();
        App.crownedPlayer = p;
        App.firstCompleter = null;
        App.gameEndTriggered = false;
        App.debugMode = false;

        String filename = "test_null_firstCompleter.json";
        Serializer.saveGame(filename);
        // Clear state
        App.players.clear();
        App.crownedPlayer = null;

        Serializer.loadGame(filename);

        assertNull(App.firstCompleter);

        // Cleanup
        new File(filename).delete();
    }

    /**
     * Test saving and loading with empty deck, empty players, empty city/hand.
     */
    @Test
    void saveAndLoadGame_emptyState() throws Exception {
        App.players.clear();
        App.deck.clear();
        App.crownedPlayer = null;
        App.firstCompleter = null;
        App.gameEndTriggered = false;
        App.debugMode = false;

        // Add a player with empty hand/city
        Player p = new Player(1, true);
        App.players.add(p);
        App.crownedPlayer = p;

        String filename = "test_empty_save.json";
        Serializer.saveGame(filename);
        // Clear state
        App.players.clear();
        App.deck.clear();
        App.crownedPlayer = null;

        Serializer.loadGame(filename);

        assertEquals(1, App.players.size());
        assertEquals(0, App.deck.size());
        assertEquals(1, App.crownedPlayer.getId());
        assertNull(App.firstCompleter);

        // Cleanup
        new File(filename).delete();
    }

    /**
     * Test IOException on save (unwritable file).
     */
    @Test
    void saveGame_throwsIOException() {
        assertThrows(IOException.class, () -> {
            // Try to write to a directory (should fail)
            Serializer.saveGame("/");
        });
    }

    /**
     * Test IOException on load (missing file).
     */
    @Test
    void loadGame_throwsIOException() {
        assertThrows(IOException.class, () -> {
            Serializer.loadGame("nonexistent_file.json");
        });
    }

    /**
     * Test ParseException on load (corrupt file).
     */
    @Test
    void loadGame_throwsParseException() throws IOException {
        String filename = "corrupt.json";
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write("not a json");
        }
        assertThrows(ParseException.class, () -> {
            Serializer.loadGame(filename);
        });
        new File(filename).delete();
    }
} 