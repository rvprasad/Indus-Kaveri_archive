
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003 SAnToS Laboratory, Kansas State University
 *
 * This software is licensed under the KSU Open Academic License.
 * You should have received a copy of the license with the distribution.
 * A copy can be found at
 *     http://www.cis.ksu.edu/santos/license.html
 * or you can contact the lab at:
 *     SAnToS Laboratory
 *     234 Nichols Hall
 *     Manhattan, KS 66506, USA
 */

package edu.ksu.cis.indus.toolkits.bandera;

import edu.ksu.cis.bandera.tool.Tool;
import edu.ksu.cis.bandera.tool.ToolConfigurationView;
import edu.ksu.cis.bandera.tool.ToolIconView;

import edu.ksu.cis.bandera.util.BaseObservable;

import edu.ksu.cis.indus.common.soot.ExceptionFlowSensitiveStmtGraphFactory;

import edu.ksu.cis.indus.interfaces.ICallGraphInfo;

import edu.ksu.cis.indus.processing.TagBasedProcessingFilter;

import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.OFAnalyzer;
import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.processors.CallGraph;
import edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzer;
import edu.ksu.cis.indus.staticanalyses.processing.ValueAnalyzerBasedProcessingController;
import edu.ksu.cis.indus.staticanalyses.tokens.TokenUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootField;

import soot.util.Chain;


/**
 * The OFATool provides a Tool for Bandera that provides information such as reachability based on OFA.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author Todd Wallentine tcw AT cis ksu edu
 * @version $Revision$ - $Date$
 */
public final class OFATool
  extends BaseObservable
  implements Tool {
	/**
	 * This key denotes the Scene.
	 */
	public static final String SCENE_INPUT_KEY = "scene";

	/**
	 * This key denotes the Set of SootMethods that are entry points.
	 */
	public static final String ENTRY_POINTS_INPUT_KEY = "entryPoints";

	/**
	 * This key denotes the call graph in the given Scene from the given entry points.
	 */
	public static final String CALL_GRAPH_OUTPUT_KEY = "callgraph";

	/**
	 * This key denotes the map of from reachable classes to the reachable fields in them in the given Scene from the given
	 * entry points.
	 */
	public static final String REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY = "reachableClassesAndFields";

	/**
	 * The list of input parameters.
	 */
	private static List inputParameterList;

	/**
	 * The list of output parameters.
	 */
	private static List outputParameterList;

	static {
		initInputParameters();
		initOutputParameters();
	}

	/**
	 * The call graph.
	 */
	private ICallGraphInfo callgraph;

	/**
	 * <p>
	 * DOCUMENT ME!
	 * </p>
	 */
	private final Map reachableClass2Fields = new HashMap();

	/**
	 * The Scene that holds the application to search in.
	 */
	private Scene scene;

	/**
	 * A set of methods that serve as the entry points to the system represented by the scene.
	 *
	 * @invariant entryPoints.oclIsKindOf(Collection(SootMethods))
	 */
	private Set entryPoints;

	/**
	 * There is no configuration information at this time so the parameter is ignored.
	 *
	 * @param configurationString Ignored at this time.
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#setConfiguration(java.lang.String)
	 */
	public void setConfiguration(final String configurationString) {
	}

	/**
	 * Get the configuration String for this tool.  At this time, it will always return null.
	 *
	 * @return Always returns null.
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#getConfiguration()
	 */
	public String getConfiguration() {
		return null;
	}

	/**
	 * Set the Map of input to use in the next run of this Tool.  This will include a Scene (SCENE_INPUT_KEY) and a Set of
	 * SootMethod objects that represent the entry points (ENTRY_POINTS_INPUT_KEY).
	 *
	 * @param inputMap The Map of input values to use in the next run of this tool.
	 *
	 * @throws Exception <i>to satisfy interface specification</i>
	 * @throws IllegalArgumentException when the input map does not contain information in the required format.
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#setInputMap(java.util.Map)
	 */
	public void setInputMap(final Map inputMap)
	  throws Exception {
		if (inputMap == null) {
			throw new IllegalArgumentException("The input Map cannot be null.");
		}

		final int _inputCount = inputMap.size();

		if (_inputCount < 2) {
			throw new IllegalArgumentException("The input Map must have at least two values.");
		}

		final Object _sceneObject = inputMap.get(SCENE_INPUT_KEY);

		if (_sceneObject == null) {
			throw new IllegalArgumentException("A scene is required.");
		}

		if (!(_sceneObject instanceof Scene)) {
			throw new IllegalArgumentException("A scene of type soot.Scene is required.");
		}

		final Scene _tempScene = (Scene) _sceneObject;
		final Chain _c = _tempScene.getClasses();

		if (_c == null) {
			throw new IllegalArgumentException("Cannot use an empty scene.");
		}

		if (_c.size() < 1) {
			throw new IllegalArgumentException("Cannot use an empty scene.");
		}

		final Object _entryPointsObject = inputMap.get(ENTRY_POINTS_INPUT_KEY);

		if (_entryPointsObject == null) {
			throw new IllegalArgumentException("The set of entry points is required.");
		}

		if (!(_entryPointsObject instanceof Set)) {
			throw new IllegalArgumentException("The set of entry points must be of type Set.");
		}

		final Set _tempEntryPoints = (Set) _entryPointsObject;

		if (_tempEntryPoints.size() < 1) {
			throw new IllegalArgumentException("The set of entry points must have at least one entry point");
		}

		scene = _tempScene;
		entryPoints = _tempEntryPoints;
	}

	/**
	 * Get the list of input parameters for this Tool.
	 *
	 * @return A List of input parameter names for this Tool.
	 *
	 * @post result.oclIsKindOf(Sequence(String))
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#getInputParameterList()
	 */
	public List getInputParameterList() {
		return inputParameterList;
	}

	/**
	 * Get the output Map .
	 *
	 * @return The Map of output generated by this Tool.
	 *
	 * @post result.get(CALL_GRAPH_OUTPUT_KEY).oclIsKindOf(ICallGraphInfo)
	 * @post result.get(CALL_GRAPH_OUTPUT_KEY) != null
	 * @post result.get(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY).oclIsKindOf(Map(SootClass, Collection(SootFields)))
	 * @post result.get(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY) != null
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#getOutputMap()
	 */
	public Map getOutputMap() {
		final Map _m = new HashMap(1);
		_m.put(CALL_GRAPH_OUTPUT_KEY, callgraph);
		_m.put(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY, Collections.unmodifiableMap(reachableClass2Fields));
		return _m;
	}

	/**
	 * Get the list of output parameters for this Tool.
	 *
	 * @return A List of output parameter names for this Tool.
	 *
	 * @post result.oclIsKindOf(Sequence(String))
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#getOutputParameterList()
	 */
	public List getOutputParameterList() {
		return outputParameterList;
	}

	/**
	 * Always return null since there is no ToolConfigurationView at this time.
	 *
	 * @return Always returns null.
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#getToolConfigurationView()
	 */
	public ToolConfigurationView getToolConfigurationView() {
		return null;
	}

	/**
	 * Always return null since there is no ToolIconView at this time.
	 *
	 * @return Always returns null.
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#getToolIconView()
	 */
	public ToolIconView getToolIconView() {
		return null;
	}

	/**
	 * Quit the generation of the Set of reachable SootMethods.  At this point, we cannot quit and just have to wait for it
	 * to complete.
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#quit()
	 */
	public void quit() {
		// TODO Figure out a way to quit? -tcw
	}

	/**
	 * Run the call graph generation to create a Set of SootMethod objects that are reachable.
	 *
	 * @exception IllegalStateException Thrown when the preconditions are not satisfied (this is usually from not calling
	 * 			  with setInputMap with proper inputs).
	 *
	 * @pre scene != null && scene is not empty
	 * @pre entryPoints != null && entryPoints.size() >= 1
	 *
	 * @see edu.ksu.cis.bandera.tool.Tool#run()
	 */
	public void run()
	  throws IllegalStateException {
		if (scene == null) {
			throw new IllegalStateException("Cannot run with a null Scene.");
		}

		if (entryPoints == null) {
			throw new IllegalStateException("Cannot run with a null Set of entry points.");
		}

		final String _tagName = "CallGraphXMLizer:FA";
		final IValueAnalyzer _aa = OFAnalyzer.getFSOSAnalyzer(_tagName, TokenUtil.getTokenManager());
		final ValueAnalyzerBasedProcessingController _pc = new ValueAnalyzerBasedProcessingController();
		final Collection _processors = new ArrayList();
		callgraph = new CallGraph();
		_pc.setAnalyzer(_aa);
		_pc.setProcessingFilter(new TagBasedProcessingFilter(_tagName));
		_pc.setStmtGraphFactory(new ExceptionFlowSensitiveStmtGraphFactory(
				ExceptionFlowSensitiveStmtGraphFactory.SYNC_RELATED_EXCEPTIONS,
				true));

		final Map _info = new HashMap();
		_info.put(ICallGraphInfo.ID, callgraph);
		_aa.reset();
		_aa.analyze(scene, entryPoints);
		((CallGraph) callgraph).reset();
		_processors.clear();
		_processors.add(callgraph);
		_pc.reset();
		_pc.driveProcessors(_processors);
		_processors.clear();

		retrieveReachableClassesAndFields(_aa, _tagName);
	}

	/**
	 * Initialize the List of input parameters.
	 */
	private static void initInputParameters() {
		inputParameterList = new ArrayList(2);
		inputParameterList.add(SCENE_INPUT_KEY);
		inputParameterList.add(ENTRY_POINTS_INPUT_KEY);
	}

	/**
	 * Initialize the List of output parameters.
	 */
	private static void initOutputParameters() {
		outputParameterList = new ArrayList(1);
		outputParameterList.add(CALL_GRAPH_OUTPUT_KEY);
		outputParameterList.add(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param valueAnalyzer DOCUMENT ME!
	 * @param tagiName DOCUMENT ME!
	 */
	private void retrieveReachableClassesAndFields(final IValueAnalyzer valueAnalyzer, final String tagiName) {
		final Collection _temp = new HashSet();

		for (final Iterator _i = valueAnalyzer.getEnvironment().getClasses().iterator(); _i.hasNext();) {
			final SootClass _sc = (SootClass) _i.next();
			_temp.clear();

			for (final Iterator _j = _sc.getFields().iterator(); _j.hasNext();) {
				final SootField _sf = (SootField) _j.next();

				if (_sf.hasTag(tagiName)) {
					_temp.add(_sf);
				}
			}

			if (!_temp.isEmpty()) {
				reachableClass2Fields.put(_sc, new ArrayList(_temp));
			} else {
				reachableClass2Fields.put(_sc, Collections.EMPTY_SET);
			}
		}
	}
}