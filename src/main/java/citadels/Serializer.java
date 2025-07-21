// File: src/main/java/citadels/Serializer.java
package citadels;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Provides JSON serialization and deserialization of the Citadels game
 * state (players, deck, crown holder, flags).
 */
public class Serializer {

    /**
     * Serializes the entire game state (all players, deck, crown, flags)
     * to the given file in JSON format.
     *
     * @param filename path of the file to write
     * @throws IOException if an I/O error occurs while writing
     */
    @SuppressWarnings("unchecked")
    public static void saveGame(String filename) throws IOException {
        JSONObject root = new JSONObject();

        // Serialize players
        JSONArray playersArray = new JSONArray();
        for (Player p : App.players) {
            JSONObject pObj = new JSONObject();
            pObj.put("id", p.getId());
            pObj.put("isHuman", p.isHuman());
            pObj.put("gold", p.getGold());
            pObj.put("character", p.getCharacter());

            // Serialize hand cards
            JSONArray handArr = new JSONArray();
            for (District d : p.getHand()) {
                JSONObject dObj = new JSONObject();
                dObj.put("name", d.getName());
                dObj.put("color", d.getColor());
                dObj.put("cost", d.getCost());
                dObj.put("description", d.getDescription());
                handArr.add(dObj);
            }
            pObj.put("hand", handArr);

            // Serialize built city districts
            JSONArray cityArr = new JSONArray();
            for (District d : p.getCity()) {
                JSONObject dObj = new JSONObject();
                dObj.put("name", d.getName());
                dObj.put("color", d.getColor());
                dObj.put("cost", d.getCost());
                dObj.put("description", d.getDescription());
                cityArr.add(dObj);
            }
            pObj.put("city", cityArr);

            playersArray.add(pObj);
        }
        root.put("players", playersArray);

        // Serialize deck
        JSONArray deckArr = new JSONArray();
        for (District d : App.deck) {
            JSONObject dObj = new JSONObject();
            dObj.put("name", d.getName());
            dObj.put("color", d.getColor());
            dObj.put("cost", d.getCost());
            dObj.put("description", d.getDescription());
            deckArr.add(dObj);
        }
        root.put("deck", deckArr);

        // Serialize crown holder and game flags
        root.put("crown", App.crownedPlayer.getId());
        root.put("firstCompleter",
                App.firstCompleter == null ? null : App.firstCompleter.getId());
        root.put("gameEndTriggered", App.gameEndTriggered);
        root.put("debugMode", App.debugMode);

        // Write JSON to file
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(root.toJSONString());
            System.out.println("Game saved to " + filename);
        }
    }

    /**
     * Reads a saved game state from the given JSON file and restores
     * all players, deck, crown holder, and flags into App’s static fields.
     *
     * @param filename path of the file to read
     * @throws IOException     if an I/O error occurs while reading
     * @throws ParseException  if the JSON is malformed
     */
    @SuppressWarnings("unchecked")
    public static void loadGame(String filename)
            throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        try (FileReader fr = new FileReader(filename)) {
            JSONObject root = (JSONObject) parser.parse(fr);

            // Rebuild players from JSON
            App.players.clear();
            Map<Long, Player> idMap = new HashMap<>();
            JSONArray playersArray = (JSONArray) root.get("players");
            for (Object o : playersArray) {
                JSONObject pObj = (JSONObject) o;
                long id       = (Long) pObj.get("id");
                boolean human = (Boolean) pObj.get("isHuman");
                Player p      = new Player((int) id, human);
                p.addGold(((Long) pObj.get("gold")).intValue());
                p.setCharacter(((Long) pObj.get("character")).intValue());

                // Rebuild hand
                for (Object ho : (JSONArray) pObj.get("hand")) {
                    JSONObject dObj = (JSONObject) ho;
                    p.addCardToHand(new District(
                            (String) dObj.get("name"),
                            (String) dObj.get("color"),
                            ((Long) dObj.get("cost")).intValue(),
                            (String) dObj.get("description")));
                }

                // Rebuild city
                for (Object co : (JSONArray) pObj.get("city")) {
                    JSONObject dObj = (JSONObject) co;
                    p.getCity().add(new District(
                            (String) dObj.get("name"),
                            (String) dObj.get("color"),
                            ((Long) dObj.get("cost")).intValue(),
                            (String) dObj.get("description")));
                }

                App.players.add(p);
                idMap.put(id, p);
            }

            // Rebuild deck from JSON
            App.deck.clear();
            for (Object o : (JSONArray) root.get("deck")) {
                JSONObject dObj = (JSONObject) o;
                App.deck.add(new District(
                        (String) dObj.get("name"),
                        (String) dObj.get("color"),
                        ((Long) dObj.get("cost")).intValue(),
                        (String) dObj.get("description")));
            }

            // Restore crown holder and game flags
            long crownId = (Long) root.get("crown");
            App.crownedPlayer    = idMap.get(crownId);
            App.firstCompleter   = root.get("firstCompleter") == null
                    ? null
                    : idMap.get((Long) root.get("firstCompleter"));
            App.gameEndTriggered = (Boolean) root.get("gameEndTriggered");
            App.debugMode        = (Boolean) root.get("debugMode");

            System.out.println("Game loaded from " + filename);
        }
    }
}
