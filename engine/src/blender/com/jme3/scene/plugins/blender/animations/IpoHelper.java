package com.jme3.scene.plugins.blender.animations;

import com.jme3.animation.BoneTrack;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.curves.BezierCurve;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.List;

/**
 * This class helps to compute values from interpolation curves for features like animation or constraint influence. The
 * curves are 3rd degree bezier curves.
 * @author Marcin Roguski
 */
public class IpoHelper extends AbstractBlenderHelper {

	/**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public IpoHelper(String blenderVersion) {
        super(blenderVersion);
    }

    /**
     * This method creates an ipo object used for interpolation calculations.
     * @param ipoStructure
     *        the structure with ipo definition
     * @param blenderContext
     *        the blender context
     * @return the ipo object
     * @throws BlenderFileException
     *         this exception is thrown when the blender file is somehow corrupted
     */
    public Ipo createIpo(Structure ipoStructure, BlenderContext blenderContext) throws BlenderFileException {
        Structure curvebase = (Structure) ipoStructure.getFieldValue("curve");

        //preparing bezier curves
        Ipo result = null;
        List<Structure> curves = curvebase.evaluateListBase(blenderContext);//IpoCurve
        if (curves.size() > 0) {
            BezierCurve[] bezierCurves = new BezierCurve[curves.size()];
            int frame = 0;
            for (Structure curve : curves) {
                Pointer pBezTriple = (Pointer) curve.getFieldValue("bezt");
                List<Structure> bezTriples = pBezTriple.fetchData(blenderContext.getInputStream());
                int type = ((Number) curve.getFieldValue("adrcode")).intValue();
                bezierCurves[frame++] = new BezierCurve(type, bezTriples, 2);
            }
            curves.clear();
            result = new Ipo(bezierCurves);
            blenderContext.addLoadedFeatures(ipoStructure.getOldMemoryAddress(), ipoStructure.getName(), ipoStructure, result);
        }
        return result;
    }

    /**
     * This method creates an ipo with only a single value. No track type is specified so do not use it for calculating
     * tracks.
     * @param constValue
     *        the value of this ipo
     * @return constant ipo
     */
    public Ipo createIpo(float constValue) {
        return new ConstIpo(constValue);
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
    	return true;
    }
    
    /**
     * Ipo constant curve. This is a curve with only one value and no specified type. This type of ipo cannot be used to
     * calculate tracks. It should only be used to calculate single value for a given frame.
     * @author Marcin Roguski
     */
    private class ConstIpo extends Ipo {

        /** The constant value of this ipo. */
        private float constValue;

        /**
         * Constructor. Stores the constant value of this ipo.
         * @param constValue
         *        the constant value of this ipo
         */
        public ConstIpo(float constValue) {
            super(null);
            this.constValue = constValue;
        }

        @Override
        public float calculateValue(int frame) {
            return constValue;
        }

        @Override
        public float calculateValue(int frame, int curveIndex) {
            return constValue;
        }

        @Override
        public int getCurvesAmount() {
            return 0;
        }

        @Override
        public BoneTrack calculateTrack(int boneIndex, int startFrame, int stopFrame, int fps) {
            throw new IllegalStateException("Constatnt ipo object cannot be used for calculating bone tracks!");
        }
    }
}
