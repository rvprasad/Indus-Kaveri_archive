/*******************************************************************************
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003, 2007 SAnToS Laboratory, Kansas State University
 * 
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 which accompanies 
 * the distribution containing this program, and is available at 
 * http://www.opensource.org/licenses/eclipse-1.0.php.
 *******************************************************************************/

package edu.ksu.cis.indus.kaveri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import soot.Type;
import soot.Value;
import soot.toolkits.graph.UnitGraph;
import edu.ksu.cis.indus.common.soot.CompleteStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.ExceptionFlowSensitiveStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;
import edu.ksu.cis.indus.kaveri.driver.KaveriRootMethodTrapper;
import edu.ksu.cis.indus.kaveri.soot.SootState;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;
import edu.ksu.cis.indus.staticanalyses.tokens.TokenUtil;
import edu.ksu.cis.indus.staticanalyses.tokens.soot.SootValueTypeManager;
import edu.ksu.cis.indus.tools.slicer.SlicerTool;

/**
 * The main plugin class.
 */
public class KaveriPlugin<T extends ITokens<T, Value>>
		extends AbstractUIPlugin {

	/**
	 * The plugin instance.
	 */
	private static KaveriPlugin plugin;

	/**
	 * The slicer tool instance.
	 */
	private SlicerTool<T> slicerTool;

	/**
	 * The resource change listener.
	 */
	private IResourceChangeListener listener;

	/**
	 * This is the annotation cache map.
	 */
	private Map cacheMap;

	/**
	 * The indusconfiguration instance.
	 */
	private IndusConfiguration indusConfiguration;

	/**
	 * Comment for <code>resourceBundle.</code>
	 */
	private ResourceBundle resourceBundle;

	/**
	 * The root method trapper instance.
	 */
	private KaveriRootMethodTrapper rmTrapper;

	/**
	 * This tracks the state of soot.
	 */
	private SootState sootState;

	/**
	 * @return provides the object that contains info about Soot's state.
	 */
	public SootState getSootState() {
		return sootState;
	}

	/**
	 * Constructor.
	 */
	public KaveriPlugin() {
		super();
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return KaveriPlugin The plugin
	 */
	public static KaveriPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 * 
	 * @param key The key to lookup
	 * @return String The string correspoding to the key
	 */
	public static String getResourceString(final String key) {
		final ResourceBundle _bundle = KaveriPlugin.getDefault().getResourceBundle();
		String _result = key;

		try {
			if (_bundle != null) {
				_result = _bundle.getString(key);
			}
		} catch (MissingResourceException _e) {
			_result = key;
			KaveriErrorLog.logException("Missing Resource", _e);
		}
		return _result;
	}

	/**
	 * Returns the Indus Configuration Instance.
	 * 
	 * @return Returns the indusConfiguration.
	 */
	public IndusConfiguration getIndusConfiguration() {
		return indusConfiguration;
	}

	/**
	 * Returns the plugin's resource bundle.
	 * 
	 * @return ResourceBundle The Resource bundle
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * Starts the plugin and initialized the default values.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		indusConfiguration = new IndusConfiguration();
		try {
			resourceBundle = ResourceBundle.getBundle("edu.ksu.cis.indus.kaveri");
		} catch (MissingResourceException _x) {
			resourceBundle = null;
			// KaveriErrorLog.logInformation("Missing resource", _x);
		}

		slicerTool = new SlicerTool<T>(TokenUtil.<T, Value, Type> getTokenManager(new SootValueTypeManager()),
				new CompleteStmtGraphFactory());
		cacheMap = new HashMap();
		rmTrapper = new KaveriRootMethodTrapper();

		sootState = new SootState();
		JavaCore.addElementChangedListener(sootState);
		indusConfiguration.getEclipseIndusDriver().addObserver(sootState);
	}

	/**
	 * Loads the defaultConfiguration.xml into slicer tool.
	 * 
	 * @throws IllegalArgumentException When an valid configuration is used.
	 */
	public void loadDefaultConfigurations() throws IllegalArgumentException {
		final StringBuffer _userConfiguration = new StringBuffer();
		final URL _url = KaveriPlugin.getDefault().getBundle().getEntry(
				"data/default_config/default_slicer_configuration.xml");

		try {
			final BufferedReader _configReader = new BufferedReader(new InputStreamReader(_url.openStream()));

			while (_configReader.ready()) {
				_userConfiguration.append(_configReader.readLine());
			}
			_configReader.close();
		} catch (IOException _ioe) {
			_ioe.printStackTrace();
			KaveriErrorLog.logException("Error reading default configuration", _ioe);
		}

		final String _configuration = _userConfiguration.toString();
		final boolean _result = slicerTool.destringizeConfiguration(_configuration);
		if (!_result) {
			throw new IllegalArgumentException("Slicer Tool passed illegal configuration");
		}

	}

	/**
	 * Loads the defaults of the plugin.
	 * 
	 * @throws IllegalArgumentException When an valid configuration is used.
	 */
	public void loadConfigurations() throws IllegalArgumentException {
		final IPreferenceStore _store = getPreferenceStore();
		final String _config = _store.getString("defaultConfiguration");

		if (_config.equals("")) {
			loadDefaultConfigurations();
		} else {
			final boolean _result = slicerTool.destringizeConfiguration(_config);
			if (!_result) {
				MessageDialog.openError(null, "Configuration Reset", "The stored configuration"
						+ " is not compatible with Indus, resetting all the configurations");
				loadDefaultConfigurations();
				storeConfiguration();
			}

		}

	}

	/**
	 * Resets the annotation cache map.
	 */
	public void reset() {
		cacheMap.clear();
	}

	/**
	 * Returns the slicer tool instance.
	 * 
	 * @return Returns the slicerTool.
	 */
	public SlicerTool getSlicerTool() {
		return slicerTool;
	}

	/**
	 * Stores the new configurations.
	 */
	public void storeConfiguration() {
		final String _config = slicerTool.stringizeConfiguration();
		getPreferenceStore().setValue("defaultConfiguration", _config);
		loadConfigurations();
	}

	/**
	 * Create a new instance of the slicer.
	 * 
	 * @param ignoreExceptionList The list of exceptions to ignore.
	 */
	public void createNewSlicer(final Collection ignoreExceptionList) {
		IStmtGraphFactory<? extends UnitGraph> _factory = null;
		if (ignoreExceptionList.isEmpty()) {
			_factory = new CompleteStmtGraphFactory();
		} else {
			_factory = new ExceptionFlowSensitiveStmtGraphFactory(ignoreExceptionList, true);
		}

		slicerTool = new SlicerTool<T>(TokenUtil.<T, Value, Type> getTokenManager(new SootValueTypeManager()), _factory);
		loadConfigurations();
	}

	/**
	 * The plugin has stopped.
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(@SuppressWarnings("unused") final BundleContext context) throws Exception {
		getIndusConfiguration().getRManager().dispose();
		final IJobManager _manager = Platform.getJobManager();
		final String _myJobFamily = "edu.ksu.cis.indus.kaveri.soottagremover";
		_manager.cancel(_myJobFamily);

	}

	/**
	 * @return Returns the cacheMap.
	 */
	public Map getCacheMap() {
		return cacheMap;
	}

	/**
	 * Adds the given object to the map.
	 * 
	 * @param key The key to the map
	 * @param value The value to the map
	 */
	public void addToCacheMap(final Object key, final Object value) {
		cacheMap.put(key, value);
	}

	/**
	 * Get the instance of the rootmethod trapper.
	 * 
	 * @return RootMethodTrapper The root method trapper instance.
	 */
	public KaveriRootMethodTrapper getRmTrapper() {
		return rmTrapper;
	}
}
