package deneme.radar.gl;

import java.util.Arrays;

/** Dunya koordinatindan NDC'ye donusum matrisleri. */
public final class Matrices {

    private Matrices() {}

    /**
     * centerX/centerY dunya koordinati; widthWorld/heightWorld tam boyut
     * (geometriler -1..1 arasinda oldugu icin).
     * min/max verilen gorunur araligi tanimlar.
     */
    public static void range(float[] matrix, float centerX, float centerY,
                             float widthWorld, float heightWorld,
                             float minX, float maxX, float minY, float maxY) {
        float rangeX = maxX - minX;
        float rangeY = maxY - minY;

        float translateX = 2f * (centerX - minX) / rangeX - 1f;
        float translateY = 2f * (centerY - minY) / rangeY - 1f;

        Arrays.fill(matrix, 0f);
        matrix[0]  = widthWorld / rangeX;    // x olcegi
        matrix[5]  = heightWorld / rangeY;   // y olcegi
        matrix[10] = 1f;
        matrix[12] = translateX;             // konum
        matrix[13] = translateY;
        matrix[15] = 1f;
    }

    public static void identity(float[] matrix) {
        Arrays.fill(matrix, 0f);
        matrix[0]  = 1f;
        matrix[5]  = 1f;
        matrix[10] = 1f;
        matrix[15] = 1f;
    }
}
