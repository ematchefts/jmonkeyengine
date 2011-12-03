package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents 'Python' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintPython extends Constraint {
	private static final Logger LOGGER = Logger.getLogger(ConstraintPython.class.getName());
	
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param boneOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintPython(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Animation animation, int targetIndex) {
		// TODO: implement 'Python' constraint
		LOGGER.log(Level.WARNING, "'Python' constraint NOT implemented!");
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_PYTHON;
	}
}
