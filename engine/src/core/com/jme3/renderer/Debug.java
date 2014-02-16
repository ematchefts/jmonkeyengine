package com.jme3.renderer;

import java.util.concurrent.atomic.AtomicInteger;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GREMEDYStringMarker.*;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GREMEDYFrameTerminator;

public class Debug {
    
    private static final AtomicInteger idCounter = new AtomicInteger();
    
    public static void printDebug(String message) {
        /*
        if (GLContext.getCapabilities() != null) {
            if (GLContext.getCapabilities().GL_ARB_debug_output) {
                int id = idCounter.getAndIncrement();
                glDebugMessageInsertARB(GL_DEBUG_SOURCE_APPLICATION_ARB,
                        GL_DEBUG_TYPE_OTHER_ARB, id,
                        GL_DEBUG_SEVERITY_MEDIUM_ARB,
                        message);

            } else if (GLContext.getCapabilities().GL_GREMEDY_string_marker) {
                glStringMarkerGREMEDY(message);
            }
        }
        */        
    }
    
    public static void endFrame() {
        /*
        if (GLContext.getCapabilities() != null) {
            if (GLContext.getCapabilities().GL_GREMEDY_frame_terminator) {
                GREMEDYFrameTerminator.glFrameTerminatorGREMEDY();
            }
        }
        */
    }
}
