package opengl.util;

import static opengl.GL.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

/**
 * Stellt Methoden zur Erzeugung von Geometrie bereit.
 * @author Sascha Kolodzey, Nico Marniok
 */
public class GeometryFactory {    
    /**
     * Erzeugt ein Vierexk in der xy-Ebene. (4 Indizes)
     * @return VertexArrayObject ID
     */
    public static Geometry createScreenQuad() {        
        int vaid = glGenVertexArrays();
        glBindVertexArray(vaid);        
        
        // vertexbuffer
        FloatBuffer vertexData = BufferUtils.createFloatBuffer(8);
        vertexData.put(new float[] {
            -1.0f, -1.0f,
            +1.0f, -1.0f,
            -1.0f, +1.0f,
            +1.0f, +1.0f,
        });
        vertexData.position(0);
        
        // indexbuffer
        IntBuffer indexData = BufferUtils.createIntBuffer(4);
        indexData.put(new int[] { 0, 1, 2, 3, });
        indexData.position(0);
        
        Geometry geo = new Geometry();
        geo.setIndices(indexData, GL_TRIANGLE_STRIP);
        geo.setVertices(vertexData);
        geo.addVertexAttribute(ShaderProgram.ATTR_POS, 2, 0);
        return geo;
    }
    
    public static Geometry createCube() {
    	float[] cubeVertices  = {
       		 0.5f,  0.5f,  0.5f,	1.0f, 1.0f, 1.0f, 1.0f, // front top right
       		-0.5f,  0.5f,  0.5f, 	0.0f, 1.0f, 1.0f, 1.0f, // front top left
       		 0.5f, -0.5f,  0.5f,	1.0f, 0.0f, 1.0f, 1.0f, // front bottom right
       		-0.5f, -0.5f,  0.5f, 	0.0f, 0.0f, 1.0f, 1.0f, // front bottom left
       		
       		 0.5f,  0.5f, -0.5f,	1.0f, 1.0f, 0.0f, 1.0f, // back top right
       		-0.5f,  0.5f, -0.5f,	0.0f, 1.0f, 0.0f, 1.0f, // back top left
       		 0.5f, -0.5f, -0.5f,	1.0f, 0.0f, 0.0f, 1.0f, // back bottom right
       		-0.5f, -0.5f, -0.5f,	0.0f, 0.0f, 0.0f, 1.0f  // back bottom left		
       };
       
       int[] cubeIndices = {
    		5, 4, 7, 6, 2, 4, 0, 
    		5, 1, 7, 3, 2, 1, 0
       };
       
       FloatBuffer cubeVertBuf = BufferUtils.createFloatBuffer(cubeVertices.length);
       IntBuffer cubeIndBuf = BufferUtils.createIntBuffer(cubeIndices.length);
       cubeVertBuf.put(cubeVertices);
       cubeVertBuf.flip();
       cubeIndBuf.put(cubeIndices);
       cubeIndBuf.flip();
       
       Geometry geo = new Geometry();
       geo.setIndices(cubeIndBuf, GL_TRIANGLE_STRIP);
       geo.setVertices(cubeVertBuf);
       geo.addVertexAttribute(ShaderProgram.ATTR_POS, 3, 0);
       geo.addVertexAttribute(ShaderProgram.ATTR_COLOR, 4, 12);
       return geo;
    }
    
    public static Geometry createTexturedCube() {
    	float size = 3.0f;
    	float[] cubeVertices  = {
       		 size,  size,  size,	-1.0f, -1.0f, -1.0f,	 0.5f, 1.0f/3.0f,  // front top right		0
       		-size,  size,  size, 	 0.0f, -1.0f, -1.0f,	0.25f, 1.0f/3.0f,  // front top left		1
       		 size, -size,  size,	-1.0f,  0.0f, -1.0f,	 0.5f, 2.0f/3.0f,  // front bottom right	2
       		-size, -size,  size, 	 0.0f,  0.0f, -1.0f,	0.25f, 2.0f/3.0f,  // front bottom left	3
       		
       		 size,  size, -size,	-1.0f, -1.0f, 0.0f,		0.75f, 1.0f/3.0f,  // back top right		4
       		-size,  size, -size,	 0.0f, -1.0f, 0.0f,		 1.0f, 1.0f/3.0f,  // back top left		5
       		 size, -size, -size,	-1.0f,  0.0f, 0.0f,		0.75f, 2.0f/3.0f,  // back bottom right	6
       		-size, -size, -size,	 0.0f,  0.0f, 0.0f,		 1.0f, 2.0f/3.0f,  // back bottom left	7
       		
       		 size,  size, -size,	-1.0f, -1.0f, 0.0f,		 0.5f,      0.0f,  // back top right		8
      		-size,  size, -size,	 0.0f, -1.0f, 0.0f,		0.25f,      0.0f,  // back top left		9
      		 size, -size, -size,	-1.0f,  0.0f, 0.0f,		 0.5f,      1.0f,  // back bottom right	10
      		-size, -size, -size,	 0.0f,  0.0f, 0.0f,		0.25f,      1.0f,  // back bottom left	11
      		
//     		 0.5f,  0.5f, -0.5f,	-1.0f, -1.0f, 0.0f,		*.25f, 1.0f/3.0f,  // back top right		12
     		-size,  size, -size,	 0.0f, -1.0f, 0.0f,		 0.0f, 1.0f/3.0f,  // back top left		13 12!
//     		 0.5f, -0.5f, -0.5f,	-1.0f,  0.0f, 0.0f,		*.75f, 1.0f/3.0f,  // back bottom right	14
     		-size, -size, -size,	 0.0f,  0.0f, 0.0f,		 0.0f, 2.0f/3.0f   // back bottom left	15 13!
       };
       
//       int[] cubeIndicesO = {
//    		5, 4, 7, 6, 2, 4, 0, 
//    		5, 1, 7, 3, 2, 1, 0
//       };
       int[] cubeIndices = {
       		 5,  4,  7,  6,  -1,
       		11, 10,  2,      -1,
       		 6,  2,  4,  0,  -1,
       		 8,  0,  9,  1,  -1,
       		12,  1, 13,  3,  -1,
       		11,  3,  2,  1,  0
       };
       
       FloatBuffer cubeVertBuf = BufferUtils.createFloatBuffer(cubeVertices.length);
       IntBuffer cubeIndBuf = BufferUtils.createIntBuffer(cubeIndices.length);
       cubeVertBuf.put(cubeVertices);
       cubeVertBuf.flip();
       cubeIndBuf.put(cubeIndices);
       cubeIndBuf.flip();
       
       Geometry geo = new Geometry();
       geo.setIndices(cubeIndBuf, GL_TRIANGLE_STRIP);
       geo.setVertices(cubeVertBuf);
       geo.addVertexAttribute(ShaderProgram.ATTR_POS, 3, 0);
       geo.addVertexAttribute(ShaderProgram.ATTR_NORMAL, 3, 12);
       geo.addVertexAttribute(ShaderProgram.ATTR_TEX, 2, 24);
       return geo;
    }
}