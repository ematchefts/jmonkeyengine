package com.jme3.material.util;

public class LightingConstants {
    public static final int MASK_LIGHTTYPE = 0x3,
                            FLAG_LIGHTTYPE_AMBIENT = 0x0,
                            FLAG_LIGHTTYPE_DIRECTIONAL = 0x1,
                            FLAG_LIGHTTYPE_POINT = 0x2,
                            FLAG_LIGHTTYPE_SPOT = 0x3;
    
    public static final int MASK_ATTENUATION = 0x4,
                            FLAG_ATTENUATION = 0x4;
    
    public static final int MAX_LIGHTS = 50;
    public static final int MAX_FLAGS = 0x8;
}
