package opengl.util;

import static opengl.GL.*;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author nico3000
 */
public class ShaderProgram {
	private final boolean DEBUG = false;
    private int id, vs, fs;
    private String vertexShader, fragmentShader;
    public ShaderProgram(String vertexShader, String fragmentShader) {
        this.createShaderProgram(vertexShader, fragmentShader);
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }
    
    public void use() {
        glUseProgram(this.id);
    }            
    
    public int getID(){
    	return this.id;
    }
    
    public void setUniform3fv(String varName, FloatBuffer fb) {
        int loc = glGetUniformLocation(this.id, varName);
        if(loc != -1) {
            GL20.glUniform3(loc, fb);
        } else {
        	if(DEBUG)
        		System.err.println("Uniform3fv: "+ varName +", ShaderProgram: "+this.id+",\n VertexShader: " + vertexShader + ", FragmentShader: " + fragmentShader);
        }
        
    }
    
    /**
     * Hilfsmethode, um eine Matrix in eine Uniform zu schreiben. Das
     * zugehoerige Programmobjekt muss aktiv sein.
     * @param matrix Quellmatrix
     * @param varName Zielvariable im Shader
     */
    public void setUniform(String varName, Matrix4f matrix) {
        int loc = glGetUniformLocation(this.id, varName);
        if(loc != -1) {
            Util.MAT_BUFFER.position(0);
            matrix.store(Util.MAT_BUFFER);
            Util.MAT_BUFFER.position(0);
            glUniformMatrix4(loc, false, Util.MAT_BUFFER);
            Util.MAT_BUFFER.position(0);
        } else {
        	if(DEBUG)
        		System.err.println("Uniform4f: "+ varName +", ShaderProgram: "+this.id+",\n VertexShader: " + vertexShader + ", FragmentShader: " + fragmentShader);
        }
    }
    
    /**
     * Hilfsmethode, um einen Vektor in eine Uniform zu schreiben. Das
     * zugehoerige Programmobjekt muss aktiv sein.
     * @param vector Vektor
     * @param varName Zielvariable im Shader
     */
    public void setUniform(String varName, Vector3f vector) {
        int loc = glGetUniformLocation(this.id, varName);
        if(loc != -1) {
            glUniform3f(loc, vector.x, vector.y, vector.z);
        }else {
        	if(DEBUG)
        		System.err.println("Uniform3f: "+ varName +", ShaderProgram: "+this.id+",\n VertexShader: " + vertexShader + ", FragmentShader: " + fragmentShader);
        }
    }
    
    /**
     * Hilfsmethode, um einen Float in eine Uniform zu schreiben. Das
     * zugehoerige Programmobjekt muss aktiv sein.
     * @param f Float
     * @param varName Zielvariable im Shader
     */
    public void setUniform(String varName, float f) {
        int loc = glGetUniformLocation(this.id, varName);
        if(loc != -1) {
            glUniform1f(loc, f);
        }else {
        	if(DEBUG)
        		System.err.println("Uniform1f: "+ varName +", ShaderProgram: "+this.id+",\n VertexShader: " + vertexShader + ", FragmentShader: " + fragmentShader);
        }
    }
    
    /**
     * Hilfsmethode, um einen Integer in eine Uniform zu schreiben. Das
     * zugehoerige Programmobjekt muss aktiv sein.
     * @param i Integer
     * @param varName Zielvariable im Shader
     */
    public void setUniform(String varName, int i) {
        int loc = glGetUniformLocation(this.id, varName);
        if(loc != -1) {
            glUniform1i(loc, i);
        }else {
        	if(DEBUG)
        		System.err.println("Uniform1i: "+ varName +", ShaderProgram: "+this.id+",\n VertexShader: " + vertexShader + ", FragmentShader: " + fragmentShader);
        }
    }
    
    /**
     * Hilfsmethode, um eine Textur in eine Uniform zu schreiben. Das
     * zugehoerige Programmobjekt muss aktiv sein.
     * @param texture Textur
     * @param varName Zielvariable im Shader
     */
    public void setUniform(String varName, Texture texture) {
        int loc = glGetUniformLocation(this.id, varName);
        if(loc != -1) {
            texture.bind();
            glUniform1i(loc, texture.getUnit());
        }else {
        	if(DEBUG)
        		System.err.println("UniformTex: "+ varName +", ShaderProgram: "+this.id+",\n VertexShader: " + vertexShader + ", FragmentShader: " + fragmentShader);
        }
    }
    
    /**
     * Attribut Index von positionMC
     */
    public static final int ATTR_POS = 0;

    /**
     * Attribut Index von normalMC
     */
    public static final int ATTR_NORMAL = 1;

    /**
     * Attribut Index von vertexColor
     */
    public static final int ATTR_COLOR = 2;
    
    /**
     * Attribut Index von vertexColor2
     */
    public static final int ATTR_COLOR2 = 3;
    
    /**
     * Attribut Index von vertexTexCoords
     */
    public static final int ATTR_TEX = 4;
    
    /**
     * Attribut Index von instancedData
     */
    public static final int ATTR_INSTANCE = 5;
    
    /**
     * Erzeugt ein ShaderProgram aus einem Vertex- und Fragmentshader.
     * @param vs Pfad zum Vertexshader
     * @param fs Pfad zum Fragmentshader
     * @return ShaderProgram ID
     */
    private void createShaderProgram(String vs, String fs) {
        this.id = glCreateProgram();
        
        this.vs = glCreateShader(GL_VERTEX_SHADER);
        this.fs = glCreateShader(GL_FRAGMENT_SHADER);
        
        glAttachShader(this.id, this.vs);
        glAttachShader(this.id, this.fs);
        
        String vertexShaderContents = Util.getFileContents(vs);
        String fragmentShaderContents = Util.getFileContents(fs);
        
        glShaderSource(this.vs, vertexShaderContents);
        glShaderSource(this.fs, fragmentShaderContents);
        
        glCompileShader(this.vs);
        glCompileShader(this.fs);
        
        String log;
        log = glGetShaderInfoLog(this.vs, 1024);
        System.out.print(log);
        log = glGetShaderInfoLog(this.fs, 1024);
        System.out.print(log);
        
        glBindAttribLocation(this.id, ATTR_POS, "positionMC");
        glBindAttribLocation(this.id, ATTR_NORMAL, "normalMC");        
        glBindAttribLocation(this.id, ATTR_COLOR, "vertexColor");
        glBindAttribLocation(this.id, ATTR_COLOR2, "vertexColor2");
        glBindAttribLocation(this.id, ATTR_TEX, "vertexTexCoords");
        glBindAttribLocation(this.id, ATTR_INSTANCE, "instancedData");
        
        glLinkProgram(this.id);        
        
        log = glGetProgramInfoLog(this.id, 1024);
        System.out.print(log);
    }
    
    public void delete() {
        GL20.glDetachShader(this.id, this.fs);
        GL20.glDetachShader(this.id, this.vs);
        GL20.glDeleteShader(this.fs);
        GL20.glDeleteShader(this.vs);
        GL20.glDeleteProgram(this.id);
    }
    
    /**
     * Gibt die ID des ShaderPrograms zur�ck.
     * @return id
     */
    public int getId() {
    	return this.id;
    }
}
