package deneme.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deniz + gemi modeli.
 * Deniz her yerde gain 0.0'dir; gemiler kendi gain factor'lerini
 * uzerinde durduklari piksellere basar. Gemiler rastgele cesitlerden
 * secilir, rastgele yerlestirilir ve surekli ufak ufak hareket eder.
 */
public class Simulation {

    private final float worldSize;
    private final List<Ship> ships = new ArrayList<>();
    private final Random random = new Random();

    public Simulation(int shipCount, float worldSize) {
        this.worldSize = worldSize;
        ShipType[] types = ShipType.values();
        for (int i = 0; i < shipCount; i++) {
            ShipType type = types[random.nextInt(types.length)];
            float half = type.size / 2f;
            float x = half + random.nextFloat() * (worldSize - 2f * half);
            float y = half + random.nextFloat() * (worldSize - 2f * half);
            ships.add(new Ship(x, y, type, random));
        }
    }

    public float worldSize() { return worldSize; }

    /** Tum gemileri dtSeconds kadar ilerletir. */
    public void update(float dtSeconds) {
        if (dtSeconds <= 0f) return;
        for (int i = 0; i < ships.size(); i++) {
            ships.get(i).update(dtSeconds, worldSize, random);
        }
    }

    /** Cizim icin gemilerin anlik kopyasi. */
    public List<Contact> snapshot() {
        List<Contact> out = new ArrayList<>(ships.size());
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            out.add(new Contact(s.x(), s.y(), s.gain(), s.size(), s.type()));
        }
        return out;
    }

    /**
     * (px,py) pikselindeki gain factor:
     * uzerinde gemi varsa en yuksek gain, yoksa deniz = 0.0.
     */
    public float gainAt(float px, float py) {
        float best = 0.0f;
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            float half = s.size() / 2f;
            if (px >= s.x() - half && px <= s.x() + half
             && py >= s.y() - half && py <= s.y() + half) {
                if (s.gain() > best) best = s.gain();
            }
        }
        return best;
    }
}
