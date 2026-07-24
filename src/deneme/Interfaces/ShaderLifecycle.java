package deneme.Interfaces;

import com.jogamp.opengl.GL2;

public interface ShaderLifecycle {

    void init(GL2 gl);

    void dispose(GL2 gl);
}
