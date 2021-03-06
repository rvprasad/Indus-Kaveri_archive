/*******************************************************************************
 * Indus, a toolkit to customize and adapt Java programs.
 * Copyright (c) 2003, 2007 SAnToS Laboratory, Kansas State University
 * 
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 which accompanies 
 * the distribution containing this program, and is available at 
 * http://www.opensource.org/licenses/eclipse-1.0.php.
 *******************************************************************************/

package edu.ksu.cis.indus.kaveri.dependence;

import edu.ksu.cis.indus.common.datastructures.Pair;
import edu.ksu.cis.indus.kaveri.KaveriErrorLog;
import edu.ksu.cis.indus.kaveri.KaveriPlugin;
import edu.ksu.cis.indus.kaveri.common.SECommons;
import edu.ksu.cis.indus.kaveri.driver.EclipseIndusDriver;
import edu.ksu.cis.indus.kaveri.soot.SootConvertor;
import edu.ksu.cis.indus.kaveri.views.IDeltaListener;
import edu.ksu.cis.indus.kaveri.views.PartialStmtData;
import edu.ksu.cis.indus.staticanalyses.dependency.AbstractDependencyAnalysis;
import edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis;
import edu.ksu.cis.indus.tools.slicer.SlicerConfiguration;
import edu.ksu.cis.indus.tools.slicer.SlicerTool;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.TreeItem;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.MonitorStmt;
import soot.jimple.Stmt;

/**
 * @author ganeshan Provides the content for the left pane of the dependence tracking view.
 */
public class DepTrkDepLstContentProvider
		implements ITreeContentProvider, IDeltaListener {

	private TreeViewer tvRight;

	private boolean isActive = false;

	private DependenceStmtData dsd;

	private RightPaneTreeParent invisibleRoot;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RightPaneTreeParent) {
			return ((RightPaneTreeParent) parentElement).getChildren();
		}
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof RightPaneTreeObject) {
			return ((RightPaneTreeObject) element).getParent();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof RightPaneTreeParent) {
			return ((RightPaneTreeParent) element).hasChildren();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (!isActive) {
			return new Object[0];
		}
		if (inputElement instanceof PartialStmtData) {
			if (invisibleRoot == null) {
				invisibleRoot = new RightPaneTreeParent("");
				initialize();
			}

		}
		return getChildren(invisibleRoot);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		if (dsd != null) {
			dsd.removeListener(this);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
	 *      java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.tvRight = (TreeViewer) viewer;
		if (oldInput != null) {
			((DependenceStmtData) oldInput).removeListener(this);
		}

		if (newInput != null) {
			this.dsd = (DependenceStmtData) newInput;
			((DependenceStmtData) newInput).addListener(this);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.ksu.cis.indus.kaveri.views.IDeltaListener#propertyChanged()
	 */
	public void propertyChanged() {
		if (tvRight != null && isActive) {
			initialize();
			tvRight.refresh();
			tvRight.expandToLevel(3);
			final TreeItem items[] = tvRight.getTree().getItems();
			if (items != null && items.length > 0) {
				tvRight.getTree().showItem(items[0]);
			}
		}
	}

	/**
	 * Initialize the tree model.
	 */
	private void initialize() {
		if (invisibleRoot != null) {
			invisibleRoot.removeAllChildren();
		}

		if (invisibleRoot != null && dsd.getSelectedStatement() != null && dsd.getStmtList() != null
				&& dsd.getStmtList().size() > 2) {
			try {
				handleDependees();
				handleDependents();
			} catch (NullPointerException _ne) {
				MessageDialog.openInformation(null, "Error in dependence module", "An unknown error has occured."
						+ "Please disable this dependence tracking view until a new slice is performed");
				KaveriErrorLog.logException("Error in dependence analysis module", _ne);
			}

		}

	}

	/**
	 * Handle dependents.
	 */
	private void handleDependents() {
		final RightPaneTreeParent _tpDependents = new RightPaneTreeParent("Dependents");
		invisibleRoot.addChild(_tpDependents);

		final RightPaneTreeParent _tpControl = new RightPaneTreeParent("Control");
		final RightPaneTreeParent _tpData = new RightPaneTreeParent("Data");
		final RightPaneTreeParent _tpReady = new RightPaneTreeParent("Ready");
		final RightPaneTreeParent _tpInterference = new RightPaneTreeParent("Interference");
		final RightPaneTreeParent _tpDivergence = new RightPaneTreeParent("Divergence");
		final RightPaneTreeParent _tpSynchronization = new RightPaneTreeParent("Synchronization");

		_tpDependents.addChild(_tpControl);
		_tpDependents.addChild(_tpData);
		_tpDependents.addChild(_tpReady);
		_tpDependents.addChild(_tpInterference);
		_tpDependents.addChild(_tpDivergence);
		_tpDependents.addChild(_tpSynchronization);

		addDependenceChildren(_tpControl, IDependencyAnalysis.DependenceSort.CONTROL_DA, false);
		addDependenceChildren(_tpData, IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA, false);
		addDependenceChildren(_tpData, IDependencyAnalysis.DependenceSort.IDENTIFIER_BASED_DATA_DA, false);
		addDependenceChildren(_tpReady, IDependencyAnalysis.DependenceSort.READY_DA, false);
		addDependenceChildren(_tpInterference, IDependencyAnalysis.DependenceSort.INTERFERENCE_DA, false);
		addDependenceChildren(_tpDivergence, IDependencyAnalysis.DependenceSort.DIVERGENCE_DA, false);
		addDependenceChildren(_tpSynchronization, IDependencyAnalysis.DependenceSort.SYNCHRONIZATION_DA, false);

	}

	/**
	 * Handle Dependees
	 */
	private void handleDependees() {
		final RightPaneTreeParent _tpDependees = new RightPaneTreeParent("Dependees");
		invisibleRoot.addChild(_tpDependees);
		final RightPaneTreeParent _tpControl = new RightPaneTreeParent("Control");
		final RightPaneTreeParent _tpData = new RightPaneTreeParent("Data");
		final RightPaneTreeParent _tpReady = new RightPaneTreeParent("Ready");
		final RightPaneTreeParent _tpInterference = new RightPaneTreeParent("Interference");
		final RightPaneTreeParent _tpDivergence = new RightPaneTreeParent("Divergence");
		final RightPaneTreeParent _tpSynchronization = new RightPaneTreeParent("Synchronization");

		_tpDependees.addChild(_tpControl);
		_tpDependees.addChild(_tpData);
		_tpDependees.addChild(_tpReady);
		_tpDependees.addChild(_tpInterference);
		_tpDependees.addChild(_tpDivergence);
		_tpDependees.addChild(_tpSynchronization);

		addDependenceChildren(_tpControl, IDependencyAnalysis.DependenceSort.CONTROL_DA, true);
		addDependenceChildren(_tpData, IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA, true);
		addDependenceChildren(_tpData, IDependencyAnalysis.DependenceSort.IDENTIFIER_BASED_DATA_DA, true);
		addDependenceChildren(_tpReady, IDependencyAnalysis.DependenceSort.READY_DA, true);
		addDependenceChildren(_tpInterference, IDependencyAnalysis.DependenceSort.INTERFERENCE_DA, true);
		addDependenceChildren(_tpDivergence, IDependencyAnalysis.DependenceSort.DIVERGENCE_DA, true);
		addDependenceChildren(_tpSynchronization, IDependencyAnalysis.DependenceSort.SYNCHRONIZATION_DA, true);

	}

	/**
	 * Adds the children to the given TreeParent as specified by the type of dependence.
	 * 
	 * @param control
	 * @param daType
	 * @param dependee True if dependees are required, false for dependents.
	 */
	private void addDependenceChildren(RightPaneTreeParent control, IDependencyAnalysis.DependenceSort daType,
			final boolean dependee) {
		final Map methodNameLineno = new HashMap(); // Map the key
		// methodname + lineno
		// ==> Java source Tree
		// Node
		final List _jimpleStmtList = dsd.getStmtList().subList(2, dsd.getStmtList().size());
		final SootMethod _sm = (SootMethod) dsd.getStmtList().get(1);
		final Set _depSet = new HashSet();
		int _masterLine = -1;
		if (dsd.getJimpleIndex() == -1) {
			for (int _i = 0; _i < _jimpleStmtList.size(); _i++) {
				final Stmt _stmt = (Stmt) _jimpleStmtList.get(_i);
				_masterLine = SECommons.getLineNumberForStmt(_stmt);
				if (dependee) {
					_depSet.addAll(handleDependees(_sm, _stmt, daType));
				} else {
					// Precondition check for reference based DA
					if (daType.equals(IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA)
							&& !(_stmt instanceof AssignStmt)) {
						continue;
					}
					_depSet.addAll(handleDependents(_sm, _stmt, daType));
				}
			}
		} else {
			if (dsd.getJimpleIndex() >= dsd.getStmtList().size() - 2) {
				return;
			}
			final Stmt _stmt = (Stmt) _jimpleStmtList.get(dsd.getJimpleIndex());
			_masterLine = SECommons.getLineNumberForStmt(_stmt);
			if (dependee) {
				_depSet.addAll(handleDependees(_sm, _stmt, daType));
			} else {
				if (daType.equals(IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA)
						&& !(_stmt instanceof AssignStmt)) {
					return;
				}
				_depSet.addAll(handleDependents(_sm, _stmt, daType));
			}
		}

		for (Iterator iter = _depSet.iterator(); iter.hasNext();) {
			final Object _obj = iter.next();

			if (_obj instanceof Stmt || (_obj instanceof Pair && ((Pair) _obj).getFirst() instanceof Local)) {
				final Stmt _stmt;
				if (_obj instanceof Stmt) {
					_stmt = (Stmt) _obj;
				} else {
					_stmt = (Stmt) ((Pair) _obj).getSecond();
				}
				if (SECommons.getLineNumberForStmt(_stmt) == _masterLine && _masterLine != -1) {
					continue;
				}
				final RightPaneTreeObject _toStmt = new RightPaneTreeObject(_stmt.toString() + " [[" + _sm.getName() + "]]");
				_toStmt.setSm(_sm);
				final int _lineno = SootConvertor.getLineNumber(_stmt);
				_toStmt.setLineNumber(_lineno);
				if (_lineno == -1) {
					control.addChild(_toStmt);
				} else {
					final String _key = _sm.getName() + _lineno;
					final Object _val = methodNameLineno.get(_key);
					if (_val == null) {
						final RightPaneTreeParent _javaSource = new RightPaneTreeParent(getJavaSourceFor(_sm, _lineno));
						_javaSource.setSm(_sm);
						_javaSource.setLineNumber(_lineno);
						_javaSource.addChild(_toStmt);
						methodNameLineno.put(_key, _javaSource);
						control.addChild(_javaSource);
					} else {
						final RightPaneTreeParent _jsource = (RightPaneTreeParent) _val;
						_jsource.addChild(_toStmt);
					}

				}
			} else if (_obj instanceof Pair) {
				final Pair _pair = (Pair) _obj;
				final Stmt _stmt = (Stmt) _pair.getFirst();
				if (_stmt == null) {
					continue;
				}
				if (SECommons.getLineNumberForStmt(_stmt) == _masterLine && _masterLine != -1) {
					continue;
				}
				final SootMethod _sootM = (SootMethod) _pair.getSecond();
				final RightPaneTreeObject _toStmt = new RightPaneTreeObject(_stmt.toString() + " [[" + _sootM.getName()
						+ "]]");
				_toStmt.setSm(_sootM);
				final int _lineno = SootConvertor.getLineNumber(_stmt);
				_toStmt.setLineNumber(_lineno);
				if (_lineno == -1) {
					control.addChild(_toStmt);
				} else {
					final String _key = _sm.getName() + _lineno;
					final Object _val = methodNameLineno.get(_key);
					if (_val == null) {
						final RightPaneTreeParent _javaSource = new RightPaneTreeParent(getJavaSourceFor(_sootM, _lineno));
						_javaSource.setSm(_sootM);
						_javaSource.setLineNumber(_lineno);
						_javaSource.addChild(_toStmt);
						methodNameLineno.put(_key, _javaSource);
						control.addChild(_javaSource);
					} else {
						final RightPaneTreeParent _jsource = (RightPaneTreeParent) _val;
						_jsource.addChild(_toStmt);
					}

				}

			}
		}

	}

	/**
	 * @param _sm
	 * @param _lineno
	 * @return
	 */
	private String getJavaSourceFor(SootMethod _sm, int _lineno) {
		String _javaSource = "<No Java Statement>";
		final IFile _file = SECommons.getFileContainingClass(_sm, dsd.getJavaFile());
		if (_file != null) {
			final Document _d = SECommons.getDocumentForJavaFile(_file);
			if (_d != null) {
				try {
					final IRegion _r = _d.getLineInformation(_lineno - 1);
					_javaSource = _d.get(_r.getOffset(), _r.getLength()).trim();
				} catch (BadLocationException e) {
					KaveriErrorLog.logException("Bad Location Exception", e);
					SECommons.handleException(e);
				}
			}
		}
		return _javaSource;
	}

	/**
	 * Returns the list of statements linked by the dependence.
	 * 
	 * @param _method
	 * @param _stmt
	 * @param dependenceType The dependecy
	 * @return List The list of dependent statements.
	 */
	public List handleDependents(SootMethod _method, Stmt _stmt, final IDependencyAnalysis.DependenceSort dependenceType) {
		final EclipseIndusDriver _driver = KaveriPlugin.getDefault().getIndusConfiguration().getEclipseIndusDriver();
		final SlicerTool _stool = _driver.getSlicer();
		if (_stool == null || _method == null || _stmt == null) {
			return new LinkedList();
		}
		final SlicerConfiguration _config = (SlicerConfiguration) _stool.getCurrentConfiguration();
		final List _lst = new LinkedList();
		Collection _coll = _config.getDependenceAnalyses(dependenceType);
		if (_coll != null) {
			Iterator it = _coll.iterator();
			while (it.hasNext()) {
				IDependencyAnalysis _crt = (IDependencyAnalysis) it.next();
				if (_crt.isStable()) {
					Collection _ids = _crt.getIds();
					if ((_ids.contains(IDependencyAnalysis.DependenceSort.IDENTIFIER_BASED_DATA_DA) && _stmt instanceof DefinitionStmt)
							|| (_ids.contains(IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA) && _stmt instanceof AssignStmt)
							|| (_ids.contains(IDependencyAnalysis.DependenceSort.INTERFERENCE_DA) && _stmt instanceof AssignStmt)
							|| (_ids.contains(IDependencyAnalysis.DependenceSort.SYNCHRONIZATION_DA) && _stmt instanceof MonitorStmt)
							|| _ids.contains(IDependencyAnalysis.DependenceSort.READY_DA)
							|| _ids.contains(IDependencyAnalysis.DependenceSort.DIVERGENCE_DA)
							|| _ids.contains(IDependencyAnalysis.DependenceSort.CONTROL_DA)) {
						Collection ct = _crt.getDependents(_stmt, _method);
						Iterator _stit = ct.iterator();
						while (_stit.hasNext()) {
							_lst.add(_stit.next());
						}
					}
				}
			}

		}
		return _lst;
	}

	/**
	 * Returns the list of statements linked by the dependence.
	 * 
	 * @param _method
	 * @param _stmt
	 * @param dependenceType The dependecy
	 * @return List The list of dependees
	 */
	public List handleDependees(SootMethod _method, Stmt _stmt, final IDependencyAnalysis.DependenceSort dependenceType) {
		final EclipseIndusDriver _driver = KaveriPlugin.getDefault().getIndusConfiguration().getEclipseIndusDriver();
		final SlicerTool _stool = _driver.getSlicer();
		if (_stool == null || _stmt == null || _method == null) {
			return new LinkedList();
		}
		final SlicerConfiguration _config = (SlicerConfiguration) _stool.getCurrentConfiguration();
		final List _lst = new LinkedList();
		Collection _coll = _config.getDependenceAnalyses(dependenceType);
		if (_coll != null) {
			Iterator it = _coll.iterator();
			while (it.hasNext()) {
				AbstractDependencyAnalysis _crt = (AbstractDependencyAnalysis) it.next();
				if (_crt.isStable()) {
					Collection ct = null;
					Collection _ids = _crt.getIds();
					if (_ids.contains(IDependencyAnalysis.DependenceSort.IDENTIFIER_BASED_DATA_DA)) {
						ct = new HashSet();
						for (final Iterator<ValueBox> _i = _stmt.getUseAndDefBoxes().iterator(); _i.hasNext();) {
							final ValueBox _vb = _i.next();
							final Value _v = _vb.getValue();
							if (_v instanceof Local) {
								ct.addAll(_crt.getDependees(new Pair<Local, Stmt>((Local) _v, _stmt), _method));
							}
						}
					} else if ((_ids.contains(IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA) && _stmt instanceof AssignStmt)
							|| (_ids.contains(IDependencyAnalysis.DependenceSort.INTERFERENCE_DA) && _stmt instanceof AssignStmt)
							|| (_ids.contains(IDependencyAnalysis.DependenceSort.SYNCHRONIZATION_DA) && _stmt instanceof MonitorStmt)
							|| _ids.contains(IDependencyAnalysis.DependenceSort.READY_DA)
							|| _ids.contains(IDependencyAnalysis.DependenceSort.DIVERGENCE_DA)
							|| _ids.contains(IDependencyAnalysis.DependenceSort.CONTROL_DA)) {
						ct = _crt.getDependees(_stmt, _method);
					}

					if (ct != null) {
						Iterator _stit = ct.iterator();
						while (_stit.hasNext()) {
							_lst.add(_stit.next());
						}
					}
				}

			}
		}

		return _lst;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.ksu.cis.indus.kaveri.views.IDeltaListener#isReady()
	 */
	public boolean isReady() {
		return isActive;
	}

	/**
	 * Set the active state of the content provider.
	 * 
	 * @param isActive The isActive to set.
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
}
