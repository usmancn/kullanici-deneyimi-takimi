package deneme.Detection;

/**
 * Obje dedektoru ile Simulation arasindaki sinyal arayuzu.
 *
 * <p>Dedektor bir obje tespit edince konumunu buradan sorar; Simulation o konumda
 * bir hedef varsa ve hedefin ID'si varsa ID'yi, yoksa {@code null} (false) doner.
 */
public interface TargetIdentifier {

    /**
     * @param x tespit edilen objenin merkez x'i
     * @param y tespit edilen objenin merkez y'si
     * @return hedefin ID'si (ID varsa), yoksa {@code null} (false)
     */
    String identify(int x, int y);
}
