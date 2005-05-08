
/*
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003, 2004, 2005 SAnToS Laboratory, Kansas State University
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

package edu.ksu.cis.indus.tools;

import edu.ksu.cis.indus.interfaces.AbstractStatus;
import edu.ksu.cis.indus.interfaces.IActivePart;

import edu.ksu.cis.indus.tools.IToolProgressListener.ToolProgressEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is an abstract implementation of ITool which the concrete implementations are encouraged to extend.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath </a>
 * @author $Author$
 * @version $Revision$
 */
public abstract class AbstractTool
  extends AbstractStatus
  implements ITool {
	/** 
	 * The logger used by instances of this class to log messages.
	 */
	static final Log LOGGER = LogFactory.getLog(AbstractTool.class);

	/** 
	 * This an object used to control the execution of the tool.
	 */
	protected final Object control = new Object();

	/** 
	 * This is the configuration information associated with this tool instance. Subclasses should provide a valid reference.
	 *
	 * @invariant configurationInfo != null
	 */
	protected IToolConfiguration configurationInfo;

	/** 
	 * This is the configurator associated with this tool instance. Subclasses should provide a valid reference.
	 *
	 * @invariant configurator != null
	 */
	protected IToolConfigurator configurator;

	/** 
	 * A collection of listeners of tools progress.
	 *
	 * @invariant listeners.oclIsKindOf(Collection(IToolProgressListener))
	 */
	final Collection listeners = Collections.synchronizedCollection(new HashSet());

	/** 
	 * This variable is used by the child thread to communicate exception state to the parent thread.
	 */
	Throwable childException;

	/** 
	 * The thread in which the tools is running or ran previously.
	 */
	Thread thread;

	/** 
	 * This indicates if the tool should pause execution.
	 */
	boolean pause;

	/** 
	 * This is the number of messages that have been accepted for delivery.
	 */
	int messageId;

	/** 
	 * This is the number of the message to be delivered next.
	 */
	int token;

	/** 
	 * The object used to realize the "active" part of this object.
	 */
	private final IActivePart.ActivePart activePart = new IActivePart.ActivePart();

	/** 
	 * This is the collection of active parts.
	 *
	 * @invariant activeParts.oclIsKindOf(Collection(IActivePart))
	 */
	private Collection activeParts = new HashSet();

	/** 
	 * The current configuration.  This is the configuration that is currently being used by the tool.
	 */
	private IToolConfiguration currentConfiguration;

	/**
	 * Creates a new AbstractTool object.
	 */
	public AbstractTool() {
		activeParts.add(activePart);
	}

	/**
	 * Retrieves an object that represents the active configuration of the tool.  This need not be the configuration
	 * currently being used by this tool.  For that please use <code>getCurrentConfiguration()</code>.
	 *
	 * @return the active configuration of the tool.
	 *
	 * @post result != null
	 */
	public final IToolConfiguration getActiveConfiguration() {
		IToolConfiguration _result;

		if (configurationInfo instanceof CompositeToolConfiguration) {
			_result = ((CompositeToolConfiguration) configurationInfo).getActiveToolConfiguration();
		} else {
			_result = configurationInfo;
		}
		return _result;
	}

	/**
	 * This implementation will breakdown the topmost composite configuration but any embedded configurations are returned as
	 * is.
	 *
	 * @see ITool#getConfigurations()
	 */
	public Collection getConfigurations() {
		final Collection _result = new HashSet();

		if (configurationInfo instanceof CompositeToolConfiguration) {
			_result.addAll(((CompositeToolConfiguration) configurationInfo).configurations);
		} else {
			_result.add(configurationInfo);
		}
		return _result;
	}

	/**
	 * Retrieves an editor which enables the user to edit the configuration of the tool. This can return <code>null</code>,
	 * if the tool does not have a configurationCollection to edit which is seldom the case.
	 *
	 * @return a configurationCollection editor.
	 */
	public final IToolConfigurator getConfigurator() {
		return configurator;
	}

	/**
	 * Retrieves the configuration being used by this tool.  This may be the active configuration or another configuration
	 * set the by tool.
	 *
	 * @return the current configuration.
	 *
	 * @post result != null
	 */
	public final IToolConfiguration getCurrentConfiguration() {
		return currentConfiguration != null ? currentConfiguration
											: getActiveConfiguration();
	}

	/**
	 * Aborts the execution of the tool.
	 */
	public final void abort() {
		final Iterator _i = activeParts.iterator();
		final int _iEnd = activeParts.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final IActivePart _executor = (IActivePart) _i.next();

			if (_executor != null) {
				fireToolProgressEvent("Aborting " + _executor, null);
				_executor.deactivate();
			}
		}
		resume();
	}

	/**
	 * @see edu.ksu.cis.indus.tools.ITool#addToolProgressListener(edu.ksu.cis.indus.tools.IToolProgressListener)
	 */
	public void addToolProgressListener(final IToolProgressListener listener) {
		listeners.add(listener);
	}

	/**
	 * Pauses the execution of the tool.
	 */
	public final void pause() {
		synchronized (control) {
			pause = true;
		}
	}

	/**
	 * @see edu.ksu.cis.indus.tools.ITool#removeToolProgressListener(edu.ksu.cis.indus.tools.IToolProgressListener)
	 */
	public void removeToolProgressListener(final IToolProgressListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Resumes the execution of the tool.
	 */
	public final void resume() {
		synchronized (control) {
			pause = false;
			control.notify();
		}
	}

	/**
	 * Executes the tool. The tool is multithreaded. However, the user can run it in synchronous mode and  if tool fails in
	 * this mode, any subsequent calls to <code>isStable()</code> until a following call to <code>run()</code> will return
	 * <code>false</code>.
	 *
	 * @param phase is the suggestive phase to start execution in.
     * @param lastPhase is the phase that should be executed last before exiting.
	 * @param synchronous <code>true</code> indicates that this method should behave synchronously and return only after the
	 * 		  tool's run has completed; <code>false</code> indicates that this method can return once the tool has started
	 * 		  it's run.
	 *
	 * @throws RuntimeException when the tool fails.
	 * @throws IllegalStateException when this method is called on a paused tool.
	 */
	public final synchronized void run(final Phase phase, final Phase lastPhase, final boolean synchronous) {
		if (!pause || isNotAlive()) {
			checkConfiguration();
			childException = null;
			unstable();
			activateActiveParts();
			thread =
				new Thread() {
						public final void run() {
							Throwable _temp = null;
							try {
								// we do this to respect any pre-run pause calls. 
								movingToNextPhase();

								execute(phase, lastPhase);
							} catch (final InterruptedException _e) {
								LOGGER.fatal("Interrupted while executing the tool.", _e);
								_temp = _e;
							} catch (final Throwable _e) {
								LOGGER.fatal("Tool failed.", _e);
								_temp = _e;
							} finally {
								if (_temp != null) {
									childException = _temp;
								}
								pause = false;
							}
						}
					};
			thread.start();

			if (synchronous) {
				try {
					thread.join();

					if (childException != null) {
						throw new RuntimeException(childException);
					}
					stable();
				} catch (final InterruptedException _e) {
					LOGGER.error("Interrupted while waiting on the run to complete.", _e);
					throw new RuntimeException(_e);
				}
			} else {
				final Thread _temp =
					new Thread() {
						public void run() {
							try {
								thread.join();

								if (childException != null) {
									throw new RuntimeException(childException);
								}
								stable();
							} catch (final InterruptedException _e) {
								LOGGER.error("Interrupted while waiting on the helper thread.", _e);
							}
						}
					};
				_temp.start();
			}
		} else {
			throw new IllegalStateException("run() should be called when the tool is paused or running.");
		}
	}

	/**
	 * Sets the current configuration.  This method should be called by the sub-classes each time after they have selected a
	 * configuration to use.
	 *
	 * @param config to be considered as the current configuration.  This has to be one of the configuration associated with
	 * 		  this tool.
	 *
	 * @pre config != null
	 */
	protected void setCurrentConfiguration(final IToolConfiguration config) {
		currentConfiguration = config;
	}

	/**
	 * Adds the given object to the collection of active part.
	 *
	 * @param part of in interest
	 */
	protected final void addActivePart(final IActivePart part) {
		activeParts.add(part);
	}

	/**
	 * Removes the given object from the collection of active part.
	 *
	 * @param part of interest.
	 *
	 * @return <code>true</code> if <code>part</code> was removed; <code>false</code>, otherwise.
	 */
	protected final boolean removeActivePart(final IActivePart part) {
		return activeParts.remove(part);
	}

	/**
	 * Checks if the tool can be configured as per the given configuration. Subclasses must override this method and throw an
	 * <code>IllegalStateException</code> if the tool cannot be configured.
	 *
	 * @throws ToolConfigurationException when the tool cannot be configured according to the configuration.
	 */
	protected void checkConfiguration()
	  throws ToolConfigurationException {
	}

	/**
	 * This is the template method in which the actual processing of the tool happens.
	 *
	 * @param phase is the suggestive phase to start execution in.
     * @param lastPhase is the phase that should be executed last before exiting.
	 *
	 * @throws InterruptedException when the execution of the tool is interrupted.
	 */
	protected abstract void execute(final Phase phase, final Phase lastPhase)
	  throws InterruptedException;

	/**
	 * Reports the given tool progress information to any registered listeners. The listeners will receive the events in the
	 * order they were fired.
	 *
	 * @param message about the progress of the tool.
	 * @param info anything the tool may want to convey to the listener.
	 *
	 * @throws RuntimeException is thrown by the message delivery thread when it is interrupted.
	 *
	 * @pre message != null and info != null
	 */
	protected void fireToolProgressEvent(final String message, final Object info) {
		synchronized (listeners) {
			final ToolProgressEvent _evt = new ToolProgressEvent(this, message, info);
			final Collection _listenersList = new HashSet(listeners);
			final Thread _t =
				new Thread() {
					private final int msgId = messageId++;

					public void run() {
						synchronized (listeners) {
							while (token != msgId) {
								try {
									listeners.wait();
								} catch (final InterruptedException _e) {
									LOGGER.fatal("Thread interrupted.  Message will not be delivered - " + _evt, _e);
									token++;
									throw new RuntimeException(_e);
								}
							}

							for (final Iterator _i = _listenersList.iterator(); _i.hasNext();) {
								final IToolProgressListener _listener = (IToolProgressListener) _i.next();
								_listener.toolProgess(_evt);
							}
							token++;
							listeners.notifyAll();
						}
					}
				};
			_t.start();
		}
	}

	/**
	 * Used to suspend the tool execution. This indicates that the tool implementation is moving onto a new phase, hence, it
	 * is at a point where it is safe to pause/suspend execution. If the application had requested the tool to pause via
	 * <code>pause()</code>, this method will suspend the execution of the tool.
	 *
	 * @throws InterruptedException when the thread in which the tool has paused is interrupted.
	 */
	protected final void movingToNextPhase()
	  throws InterruptedException {
		synchronized (control) {
			if (pause) {
				control.wait();
			} else if (!activePart.canProceed()) {
				final String _string = "Tool was interrupted.";
				fireToolProgressEvent(_string, null);
			}
		}
	}

	/**
	 * Checks if the tool's thread is not alive.
	 *
	 * @return <code>true</code> if the tool is not alive; <code>false</code>, otherwise.
	 */
	private boolean isNotAlive() {
		return thread == null || !thread.isAlive();
	}

	/**
	 * Aborts the execution of the tool.
	 */
	private void activateActiveParts() {
		final Iterator _i = activeParts.iterator();
		final int _iEnd = activeParts.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final IActivePart _executor = (IActivePart) _i.next();

			if (_executor != null) {
				fireToolProgressEvent("Aborting " + _executor, null);
				_executor.activate();
			}
		}
	}
}

// End of File
