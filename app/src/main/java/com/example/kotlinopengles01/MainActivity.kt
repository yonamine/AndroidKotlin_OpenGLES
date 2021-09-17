package com.example.kotlinopengles01

/**
 *
 * References:
 * 1. https://github.com/Danesprite/kotlin-opengl-testing
 *
 */
import android.content.Context
import android.content.res.Configuration
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


val sampleVertexShaderCode =
    """
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
           void main() {
               // the matrix must be included as a modifier of gl_Position
               // Note that the uMVPMatrix factor *must be first* in order
               // for the matrix multiplication product to be correct.
               gl_Position = uMVPMatrix * vPosition;
           }
        """

val sampleFragmentShaderCode =
    """precision mediump float;
            uniform vec4 vColor;
            void main() {
              gl_FragColor = vColor;
            }
        """

/** Wrapper data class for OpenGL Shading Language (GLSL) source code */
data class GLSLSourceCode(val shaderCode: String)

/** Wrapper class for GLESPrograms */
data class GLESProgram(val programRef: Int)

/**
 * Factory function for creating wrapped GLES programs
 * This is here because passing around Ints as programs seems stupid to me
 */
fun createGLESProgram(): GLESProgram {
    // create empty OpenGL ES Program
    return GLESProgram(GLES20.glCreateProgram())
}


/**
 * Function that handles compilation of OpenGL Shading Language (GLSL) code
 * Converted to Kotlin from the code available here:
 * https://developer.android.com/training/graphics/opengl/draw.html
 */
fun loadShader(type: Int, shaderSourceCode: GLSLSourceCode): Int {
    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
    val shader = GLES20.glCreateShader(type)

    // add the source code to the shader and compile it
    GLES20.glShaderSource(shader, shaderSourceCode.shaderCode)
    GLES20.glCompileShader(shader)

    val compilationStatus = IntArray(1) // Only holds one int

    // Note: The last parameter of this method is the index of the IntArray to use
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compilationStatus, 0)

    // Log an error message when a shader doesn't compile
    if ( compilationStatus[0] == GLES20.GL_FALSE ) {
        Log.e("OpenGL", "shader failed to compile")
    }

    return shader
}






/**
 * Parent class of my OpenGL objects
 */
abstract class OpenGLShape {
    protected val myProgram: GLESProgram
    constructor(program: GLESProgram,
                vertexShaderCode: GLSLSourceCode = GLSLSourceCode(sampleVertexShaderCode),
                fragmentShaderCode: GLSLSourceCode = GLSLSourceCode(sampleFragmentShaderCode)) {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        myProgram = program
        GLES20.glAttachShader(myProgram.programRef, vertexShader)
        GLES20.glAttachShader(myProgram.programRef, fragmentShader)
        GLES20.glLinkProgram(myProgram.programRef)
    }
    /** Function called to draw the shape on the device screen */
    abstract fun draw(mMVPMatrix: FloatArray)
}

class Triangle : OpenGLShape {
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0 // Used to access and set the view transformation
    private val vertexBuffer: FloatBuffer
    private var triangleCoords = floatArrayOf(// in counterclockwise order:
         0.0f,  0.622008459f, 0.0f, // top
        -0.5f, -0.311004243f, 0.0f, // bottom left
         0.5f, -0.311004243f, 0.0f  // bottom right
    )
    private val vertexCount = triangleCoords.size / COORDS_PER_VERTEX

    constructor(mGLESProgram: GLESProgram) : super(mGLESProgram)

    init {
        // initialize vertex byte buffer for shape coordinates
        // (number of coordinate values * 4 bytes per float)
        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder())
        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer()
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords)
        // set the buffer to read the first coordinate
        vertexBuffer.position(0)
    }

    override fun draw(mMVPMatrix: FloatArray) {
        val programRef = myProgram.programRef

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(programRef)

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(programRef, "vPosition")

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(programRef, "vColor")

        // Get the handle to the shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programRef, "uMVPMatrix")

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, colour, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    companion object {
        // number of coordinates per vertex in this array
        val COORDS_PER_VERTEX = 3
        internal val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
        // Set color with red, green, blue and alpha (opacity) values
        internal val colour = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)
    }
}


/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
class MainActivity : AppCompatActivity() {
    private lateinit var mGLSurfaceView : MyGLSurfaceView
    private var previousXpos: Float = 0.0f
    private var previousYpos: Float = 0.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f
    private var currentlyScaling: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGLSurfaceView = MyGLSurfaceView(this)
        setContentView(mGLSurfaceView)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState?.putFloat(STATE_ROTATION_ANGLE, mGLSurfaceView!!.myRenderer.angle)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mGLSurfaceView?.myRenderer?.angle = savedInstanceState?.getFloat(STATE_ROTATION_ANGLE) as Float
        super.onRestoreInstanceState(savedInstanceState)
    }

    /**
     * View <- GLSurfaceView
     * 'View': where you can draw and manipulate objects using OpenGL API calls
     *         it's similar in function to a 'SurfaceView'
     *  I have to add a 'Renderer' to a instance of 'GLSurfaceView'
     */
    inner class MyGLSurfaceView : GLSurfaceView {
        val myRenderer: MyGLRenderer
        constructor(context: Context?) : super(context) {
            setEGLContextClientVersion(2)

            myRenderer = MyGLRenderer()

            // Adding a 'Renderer' object to this instance of 'GLSurfaceView'
            setRenderer(myRenderer)

            // Render the view only when there's a change in the drawing data
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

            scaleGestureDetector = ScaleGestureDetector(this.context, object: ScaleGestureDetector.OnScaleGestureListener {
                override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                    currentlyScaling = true
                    return true
                }
                override fun onScaleEnd(detector: ScaleGestureDetector?) {
                    currentlyScaling = false
                }
                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    if(detector == null) {
                        return false
                    }
                    scaleFactor *= detector.scaleFactor
                    myRenderer.cameraEyeZ *= scaleFactor
                    scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
                    val orientation = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
                    if(orientation == "landscape") {
                        myRenderer.cameraEyeZ = Math.max(LANDSCAPE_NEAR_LIMIT,
                            Math.min(myRenderer.cameraEyeZ, LANDSCAPE_FAR_LIMIT))
                    }
                    else {
                        myRenderer.cameraEyeZ = Math.max(PORTRAIT_NEAR_LIMIT,
                            Math.min(myRenderer.cameraEyeZ, PORTRAIT_FAR_LIMIT))
                    }
                    requestRender()
                    invalidate()
                    return true
                }
            })

        }

        /**
         * As I want to capture touch screen events, the 'GLSurfaceView' class is
         * extended and touch events are implemented
         */
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event != null) {
                val x: Float = event.x
                val y: Float = event.y

                scaleGestureDetector.onTouchEvent(event)
                when {
                    event.action == MotionEvent.ACTION_MOVE && !currentlyScaling -> {
                        var dx = event.x - previousXpos
                        var dy = event.y - previousYpos

                        // Reverse the direction of rotation above the mid-line
                        if(y > height / 2)
                            dx *= -1
                        // Reverse the direction of rotation to left of the mid-line
                        if(x < width / 2)
                            dy *= -1
                        myRenderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR
                        requestRender()
                    }
                }
                previousXpos = event.x
                previousYpos = event.y
            }
            return true
        }
    }


    /**
     * 'GLSurfaceView.Renderer' code is running on a separate thread from the
     * main user interface thread
     */
    inner class MyGLRenderer : GLSurfaceView.Renderer {

        // '@Volatile': it's a public member, it allows the UI thread to
        // access the public variable in renderer thread
        @Volatile var angle: Float = 0.0f
        @Volatile var cameraEyeZ: Float = 3f

        private val mMVPMatrix = FloatArray(16)
        private val mProjectionMatrix = FloatArray(16)
        private val mViewMatrix = FloatArray(16)

        private var triangle1: OpenGLShape? = null
        private var triangle2: OpenGLShape? = null

        override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            triangle1 = Triangle(createGLESProgram())
            triangle2 = Triangle(createGLESProgram())
        }

        override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)

            val aspectRatio = width / height.toFloat()

            // Is it really a good approach?
            val orientation = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"

            // frustumM - Defines a projection matrix in terms of six clip (clipping) planes.
            if(orientation == "landscape") {
                Matrix.frustumM(mProjectionMatrix,
                    0,
                    -aspectRatio,
                    aspectRatio,
                    -1f,
                    1f,
                    LANDSCAPE_NEAR_LIMIT,
                    LANDSCAPE_FAR_LIMIT)
            }
            else {
                Matrix.frustumM(mProjectionMatrix,
                    0,
                    -aspectRatio,
                    aspectRatio,
                    -1f,
                    1f,
                    PORTRAIT_NEAR_LIMIT,
                    PORTRAIT_FAR_LIMIT)
            }
        }

        override fun onDrawFrame(unused: GL10?) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            // --- CAMERA ---
            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, cameraEyeZ, 0f, 0f, 0f, 0f, 1.0f, 0.0f)


            // Calculate the projection and view transformation
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)

            // Keep a copy of mMVPMatrix
            val mMVPMatrix2 = mMVPMatrix.clone()


            // Calculate the rotation angle
            // val time = (SystemClock.uptimeMillis() % 4000L)
            // angle = 0.09f * time.toInt()

            // TRaSheS - Translate, Rotate, Shear (not in Matrix class), and then Scale
            // Translate the first triangle to the left on the x-axis (in-place method)
            Matrix.translateM(mMVPMatrix, 0, -0.5f, 0f, 0f)

            // Rotate the first triangle
            // rotateM - Rotates matrix m in place by angle a (in degrees) around the axis (x, y, z).
            // second parameter is "offset index into m where the matrix starts"
            Matrix.rotateM(mMVPMatrix, 0, angle, 0f, 0f, -1.0f)

            // Translate the second triangle
            Matrix.translateM(mMVPMatrix2, 0, 0.5f, 0f, 0f)

            // Rotate the second triangle in the opposite direction on the z-axis
            Matrix.rotateM(mMVPMatrix2, 0, angle, 0f, 0f, 1.0f)

            triangle1?.draw(mMVPMatrix)
            triangle2?.draw(mMVPMatrix2)

        }
    }


    companion object {
        private val STATE_ROTATION_ANGLE = "angle"
        private val TOUCH_SCALE_FACTOR = 210.0f / 360
        private val PORTRAIT_NEAR_LIMIT = 1.5f
        private val PORTRAIT_FAR_LIMIT = 12f
        private val LANDSCAPE_NEAR_LIMIT = 2.5f
        private val LANDSCAPE_FAR_LIMIT = 9f
    }

}