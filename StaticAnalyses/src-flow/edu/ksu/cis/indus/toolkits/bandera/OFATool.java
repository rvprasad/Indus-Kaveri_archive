/*******************************************************************************
 * Indus, a program analysis and transformation toolkit for Java.
 * Copyright (c) 2001, 2007 Venkatesh Prasad Ranganath
 * 
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 which accompanies 
 * the distribution containing this program, and is available at 
 * http://www.opensource.org/licenses/eclipse-1.0.php.
 * 
 * For questions about the license, copyright, and software, contact 
 * 	Venkatesh Prasad Ranganath at venkateshprasad.ranganath@gmail.com
 *                                 
 * This software was developed by Venkatesh Prasad Ranganath in SAnToS Laboratory 
 * at Kansas State University.
 *******************************************************************************/

package edu.ksu.cis.indus.toolkits.bandera;

import edu.ksu.cis.bandera.tool.Tool;
import edu.ksu.cis.bandera.tool.ToolConfigurationView;
import edu.ksu.cis.bandera.tool.ToolIconView;
import edu.ksu.cis.bandera.util.BaseObservable;
import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.common.datastructures.Pair.PairManager;
import edu.ksu.cis.indus.common.soot.BasicBlockGraphMgr;
import edu.ksu.cis.indus.common.soot.CompleteStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.Util;
import edu.ksu.cis.indus.interfaces.IActivePart;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.processing.Environment;
import edu.ksu.cis.indus.processing.IProcessor;
import edu.ksu.cis.indus.processing.OneAllStmtSequenceRetriever;
import edu.ksu.cis.indus.processing.TagBasedProcessingFilter;
import edu.ksu.cis.indus.staticanalyses.callgraphs.CallGraphInfo;
import edu.ksu.cis.indus.staticanalyses.callgraphs.OFABasedCallInfoCollector;
import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.OFAnalyzer;
import edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzer;
import edu.ksu.cis.indus.staticanalyses.processing.ValueAnalyzerBasedProcessingController;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokenManager;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;
import edu.ksu.cis.indus.staticanalyses.tokens.TokenUtil;
import edu.ksu.cis.indus.staticanalyses.tokens.soot.SootValueTypeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.toolkits.graph.CompleteUnitGraph;
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
	 * This key denotes the basic block graph manager.
	 */
	public static final String BASIC_BLOCK_GRAPH_MGR_OUTPUT_KEY = "basicBlockGraphMgr";

	/**
	 * This key denotes the call graph in the given Scene from the given entry points.
	 */
	public static final String CALL_GRAPH_OUTPUT_KEY = "callGraph";

	/**
	 * This key denotes the Set of SootMethods that are entry points.
	 */
	public static final String ENTRY_POINTS_INPUT_KEY = "entryPoints";

	/**
	 * This key denotes the map of from reachable classes to the reachable fields in them in the given Scene from the given
	 * entry points.
	 */
	public static final String REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY = "reachableClassesAndFields";

	/**
	 * This key denotes the Scene.
	 */
	public static final String SCENE_INPUT_KEY = "scene";

	/**
	 * The list of input parameters.
	 */
	private static List<String> inputParameterList;

	/**
	 * The list of output parameters.
	 */
	private static List<String> outputParameterList;

	static {
		initInputParameters();
		initOutputParameters();
	}

	/**
	 * The only active part in this tool corresponding to flow analysis.
	 */
	IActivePart activePart;

	/**
	 * The basic block graph manager.
	 */
	private BasicBlockGraphMgr basicBlockGraphMgr = new BasicBlockGraphMgr();

	/**
	 * The call graph.
	 */
	private ICallGraphInfo callgraph;

	/**
	 * A set of methods that serve as the entry points to the system represented by the scene.
	 */
	private Set<SootMethod> entryPoints;

	/**
	 * A map from reachable classes to a collection of reachabled fields in them.
	 * 
	 * @invariant reachableClass2Fields.values()->forall(o | o->forall(p | p is reachable))
	 */
	private final Map<SootClass, Collection<SootField>> reachableClass2Fields = new HashMap<SootClass, Collection<SootField>>();

	/**
	 * The Scene that holds the application to be analyzed.
	 */
	private Scene scene;

	/**
	 * Initialize the List of input parameters.
	 */
	private static void initInputParameters() {
		inputParameterList = new ArrayList<String>(2);
		inputParameterList.add(SCENE_INPUT_KEY);
		inputParameterList.add(ENTRY_POINTS_INPUT_KEY);
	}

	/**
	 * Initialize the List of output parameters.
	 */
	private static void initOutputParameters() {
		outputParameterList = new ArrayList<String>(1);
		outputParameterList.add(CALL_GRAPH_OUTPUT_KEY);
		outputParameterList.add(BASIC_BLOCK_GRAPH_MGR_OUTPUT_KEY);
		outputParameterList.add(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY);
	}

	/**
	 * Get the configuration String for this tool. At this time, it will always return null.
	 * 
	 * @return Always returns null.
	 * @see edu.ksu.cis.bandera.tool.Tool#getConfiguration()
	 */
	public String getConfiguration() {
		return null;
	}

	/**
	 * Get the list of input parameters for this Tool.
	 * 
	 * @return A List of input parameter names for this Tool.
	 * @see edu.ksu.cis.bandera.tool.Tool#getInputParameterList()
	 */
	public List<String> getInputParameterList() {
		return inputParameterList;
	}

	/**
	 * Get the output Map .
	 * 
	 * @return The Map of output generated by this Tool.
	 * @post result.get(CALL_GRAPH_OUTPUT_KEY).oclIsKindOf(ICallGraphInfo)
	 * @post result.get(CALL_GRAPH_OUTPUT_KEY) != null
	 * @post result.get(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY).oclIsKindOf(Map(SootClass, Collection(SootFields)))
	 * @post result.get(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY) != null
	 * @see edu.ksu.cis.bandera.tool.Tool#getOutputMap()
	 */
	public Map<Object, Object> getOutputMap() {
		final Map<Object, Object> _m = new HashMap<Object, Object>(3);
		_m.put(CALL_GRAPH_OUTPUT_KEY, callgraph);
		_m.put(REACHABLE_CLASSES_AND_FIELDS_OUTPUT_KEY, Collections.unmodifiableMap(reachableClass2Fields));
		_m.put(BASIC_BLOCK_GRAPH_MGR_OUTPUT_KEY, basicBlockGraphMgr);
		return _m;
	}

	/**
	 * Get the list of output parameters for this Tool.
	 * 
	 * @return A List of output parameter names for this Tool.
	 * @see edu.ksu.cis.bandera.tool.Tool#getOutputParameterList()
	 */
	public List<String> getOutputParameterList() {
		return outputParameterList;
	}

	/**
	 * Always return <code>null</code> since there is no ToolConfigurationView at this time.
	 * 
	 * @return Always returns null.
	 * @see edu.ksu.cis.bandera.tool.Tool#getToolConfigurationView()
	 */
	public ToolConfigurationView getToolConfigurationView() {
		return null;
	}

	/**
	 * Always return <code>null</code> since there is no ToolIconView at this time.
	 * 
	 * @return Always returns null.
	 * @see edu.ksu.cis.bandera.tool.Tool#getToolIconView()
	 */
	public ToolIconView getToolIconView() {
		return null;
	}

	/**
	 * Quit the generation of the set of reachable SootMethods.
	 * 
	 * @see edu.ksu.cis.bandera.tool.Tool#quit()
	 */
	public void quit() {
		activePart.deactivate();
	}

	/**
	 * Run the call graph generation to create a Set of SootMethod objects that are reachable.
	 * 
	 * @throws IllegalStateException when the preconditions are not satisfied (this is usually from not calling with
	 *             setInputMap with proper inputs).
	 * @pre scene != null && scene is not empty
	 * @pre entryPoints != null && entryPoints.size() >= 1
	 * @see edu.ksu.cis.bandera.tool.Tool#run()
	 */
	public void run() throws IllegalStateException {
		this.<ITokens> execute();
	}

	/**
	 * There is no configuration information at this time so the parameter is ignored.
	 * 
	 * @param configurationString Ignored at this time.
	 * @see edu.ksu.cis.bandera.tool.Tool#setConfiguration(java.lang.String)
	 */
	@Empty public void setConfiguration(@SuppressWarnings("unused") final String configurationString) {
		// Does nothing
	}

	/**
	 * Set the Map of input to use in the next run of this Tool. This will include a Scene (SCENE_INPUT_KEY) and a Set of
	 * SootMethod objects that represent the entry points (ENTRY_POINTS_INPUT_KEY).
	 * 
	 * @param inputMap The Map of input values to use in the next run of this tool.
	 * @throws Exception <i>to satisfy interface specification</i>
	 * @throws IllegalArgumentException when the input map does not contain information in the required format.
	 * @see edu.ksu.cis.bandera.tool.Tool#setInputMap(java.util.Map)
	 */
	public void setInputMap(final Map inputMap) throws Exception {
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

		if (_c == null || _c.size() < 1) {
			throw new IllegalArgumentException("Cannot use an empty scene.");
		}

		final Object _entryPointsObject = inputMap.get(ENTRY_POINTS_INPUT_KEY);

		if (_entryPointsObject == null) {
			throw new IllegalArgumentException("The set of entry points is required.");
		}

		if (!(_entryPointsObject instanceof Set)) {
			throw new IllegalArgumentException("The set of entry points must be of type Set.");
		}

		final Set<SootMethod> _tempEntryPoints = (Set) _entryPointsObject;

		if (_tempEntryPoints.size() < 1) {
			throw new IllegalArgumentException("The set of entry points must have at least one entry point");
		}

		scene = _tempScene;
		entryPoints = _tempEntryPoints;
	}

	/**
	 * Executes the core logic. This is a mere container to use generified Indus libraries.
	 * 
	 * @param <T> is a dummy type parameter
	 */
	private <T extends ITokens<T, Value>> void execute() {
		if (scene == null) {
			throw new IllegalStateException("Cannot run with a null Scene.");
		}

		if (entryPoints == null) {
			throw new IllegalStateException("Cannot run with a null Set of entry points.");
		}

		final String _tagName = "CallGraphXMLizer:FA";
		final IStmtGraphFactory<CompleteUnitGraph> _factory = new CompleteStmtGraphFactory();
		final ITokenManager<T, Value, Type> _tokenManager = TokenUtil
				.<T, Value, Type> getTokenManager(new SootValueTypeManager());
		final IValueAnalyzer<Value> _aa = OFAnalyzer.getFSOSAnalyzer(_tagName, _tokenManager, _factory);
		final ValueAnalyzerBasedProcessingController _pc = new ValueAnalyzerBasedProcessingController();
		final Collection<IProcessor> _processors = new ArrayList<IProcessor>();
		final OneAllStmtSequenceRetriever _ssr = new OneAllStmtSequenceRetriever();
		basicBlockGraphMgr.setStmtGraphFactory(_factory);
		_ssr.setBbgFactory(basicBlockGraphMgr);
		_pc.setStmtSequencesRetriever(_ssr);

		_pc.setAnalyzer(_aa);
		_pc.setProcessingFilter(new TagBasedProcessingFilter(_tagName));

		final CallGraphInfo _cgi = new CallGraphInfo(new PairManager(false, true));
		final OFABasedCallInfoCollector _callGraphInfoCollector = new OFABasedCallInfoCollector();
		final Map<Object, Object> _info = new HashMap<Object, Object>();
		_info.put(ICallGraphInfo.ID, callgraph);

		activePart = _aa.getActivePart();
		_aa.reset();
		_aa.analyze(new Environment(scene), entryPoints);
		_processors.clear();
		_processors.add(_callGraphInfoCollector);
		_pc.reset();
		_pc.driveProcessors(_processors);
		_processors.clear();
		_cgi.reset();
		_cgi.createCallGraphInfo(_callGraphInfoCollector.getCallInfo());
		callgraph = _cgi;

		retrieveReachableClassesAndFields(_aa, _tagName);
	}

	/**
	 * Retrieves the reachable classes and the fields these classes.
	 * 
	 * @param valueAnalyzer used to determine reachability.
	 * @param tagName used to identify reachable parts.
	 * @pre valueAnalyzer != null and tagName != null
	 */
	private void retrieveReachableClassesAndFields(final IValueAnalyzer<Value> valueAnalyzer, final String tagName) {
		for (final Iterator<SootClass> _i = valueAnalyzer.getEnvironment().getClasses().iterator(); _i.hasNext();) {
			final SootClass _sc = _i.next();
			final Collection<SootField> _fields = _sc.getFields();
			final Collection<SootField> _temp = Util.getHostsWithTag(_fields, tagName);
			reachableClass2Fields.put(_sc, _temp);
		}
	}
}

// End of File
