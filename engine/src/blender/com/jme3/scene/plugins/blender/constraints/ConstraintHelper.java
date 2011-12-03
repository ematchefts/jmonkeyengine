package com.jme3.scene.plugins.blender.constraints;

import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class should be used for constraint calculations.
 * @author Marcin Roguski
 */
public class ConstraintHelper extends AbstractBlenderHelper {
	private static final Logger LOGGER = Logger.getLogger(ConstraintHelper.class.getName());
	
	/**
	 * Helper constructor. It's main task is to generate the affection functions. These functions are common to all
	 * ConstraintHelper instances. Unfortunately this constructor might grow large. If it becomes too large - I shall
	 * consider refactoring. The constructor parses the given blender version and stores the result. Some
	 * functionalities may differ in different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public ConstraintHelper(String blenderVersion, BlenderContext blenderContext) {
		super(blenderVersion);
	}

	/**
	 * This method reads constraints for for the given structure. The constraints are loaded only once for object/bone.
	 * @param ownerOMA
	 *        the owner's old memory address
	 * @param objectStructure
	 *        the structure we read constraint's for
	 * @param blenderContext
	 *        the blender context
	 * @throws BlenderFileException
	 */
	public Map<Long, List<Constraint>> loadConstraints(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
		if (blenderVersion >= 250) {//TODO
			LOGGER.warning("Loading of constraints not yet implemented for version 2.5x !");
			return new HashMap<Long, List<Constraint>>(0);
		}
		
		// reading influence ipos for the constraints
		IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
		Map<String, Map<String, Ipo>> constraintsIpos = new HashMap<String, Map<String, Ipo>>();
		Pointer pActions = (Pointer) objectStructure.getFieldValue("action");
		if (pActions.isNotNull()) {
			List<Structure> actions = pActions.fetchData(blenderContext.getInputStream());
			for (Structure action : actions) {
				Structure chanbase = (Structure) action.getFieldValue("chanbase");
				List<Structure> actionChannels = chanbase.evaluateListBase(blenderContext);
				for (Structure actionChannel : actionChannels) {
					Map<String, Ipo> ipos = new HashMap<String, Ipo>();
					Structure constChannels = (Structure) actionChannel.getFieldValue("constraintChannels");
					List<Structure> constraintChannels = constChannels.evaluateListBase(blenderContext);
					for (Structure constraintChannel : constraintChannels) {
						Pointer pIpo = (Pointer) constraintChannel.getFieldValue("ipo");
						if (pIpo.isNotNull()) {
							String constraintName = constraintChannel.getFieldValue("name").toString();
							Ipo ipo = ipoHelper.createIpo(pIpo.fetchData(blenderContext.getInputStream()).get(0), blenderContext);
							ipos.put(constraintName, ipo);
						}
					}
					String actionName = actionChannel.getFieldValue("name").toString();
					constraintsIpos.put(actionName, ipos);
				}
			}
		}

		Map<Long, List<Constraint>> result = new HashMap<Long, List<Constraint>>();
		
		//loading constraints connected with the object's bones
		Pointer pPose = (Pointer) objectStructure.getFieldValue("pose");//TODO: what if the object has two armatures ????
		if (pPose.isNotNull()) {
			List<Structure> poseChannels = ((Structure) pPose.fetchData(blenderContext.getInputStream()).get(0).getFieldValue("chanbase")).evaluateListBase(blenderContext);
			for (Structure poseChannel : poseChannels) {
				List<Constraint> constraintsList = new ArrayList<Constraint>();
				Long boneOMA = Long.valueOf(((Pointer) poseChannel.getFieldValue("bone")).getOldMemoryAddress());
				
				//the name is read directly from structure because bone might not yet be loaded
				String name = blenderContext.getFileBlock(boneOMA).getStructure(blenderContext).getFieldValue("name").toString();
				List<Structure> constraints = ((Structure) poseChannel.getFieldValue("constraints")).evaluateListBase(blenderContext);
				for (Structure constraint : constraints) {
					String constraintName = constraint.getFieldValue("name").toString();
					Map<String, Ipo> ipoMap = constraintsIpos.get(name);
					Ipo ipo = ipoMap==null ? null : ipoMap.get(constraintName);
					if (ipo == null) {
						float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
						ipo = ipoHelper.createIpo(enforce);
					}
					constraintsList.add(ConstraintFactory.createConstraint(constraint, boneOMA, ipo, blenderContext));
				}
				
				result.put(boneOMA, constraintsList);
				blenderContext.addConstraints(boneOMA, constraintsList);
			}
		}
		// TODO: reading constraints for objects (implement when object's animation will be available)
		List<Structure> constraintChannels = ((Structure)objectStructure.getFieldValue("constraintChannels")).evaluateListBase(blenderContext);
		for(Structure constraintChannel : constraintChannels) {
			System.out.println(constraintChannel);
		}

		//loading constraints connected with the object itself (TODO: test this)
		if(!result.containsKey(objectStructure.getOldMemoryAddress())) {
			List<Structure> constraints = ((Structure)objectStructure.getFieldValue("constraints")).evaluateListBase(blenderContext);
			List<Constraint> constraintsList = new ArrayList<Constraint>(constraints.size());
			
			for(Structure constraint : constraints) {
				String constraintName = constraint.getFieldValue("name").toString();
				String objectName = objectStructure.getName();
				
				Map<String, Ipo> objectConstraintsIpos = constraintsIpos.get(objectName);
				Ipo ipo = objectConstraintsIpos!=null ? objectConstraintsIpos.get(constraintName) : null;
				if (ipo == null) {
					float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
					ipo = ipoHelper.createIpo(enforce);
				}
				constraintsList.add(ConstraintFactory.createConstraint(constraint, null, ipo, blenderContext));
			}
			result.put(objectStructure.getOldMemoryAddress(), constraintsList);
			blenderContext.addConstraints(objectStructure.getOldMemoryAddress(), constraintsList);
		}
		return result;
	}
	
	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return true;
	}
}
