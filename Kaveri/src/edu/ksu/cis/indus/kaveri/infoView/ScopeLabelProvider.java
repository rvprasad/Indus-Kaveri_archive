/*
 * Created on Jun 3, 2005
 *
 * 
 */
package edu.ksu.cis.indus.kaveri.infoView;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.ksu.cis.indus.common.scoping.ClassSpecification;
import edu.ksu.cis.indus.common.scoping.FieldSpecification;
import edu.ksu.cis.indus.common.scoping.MethodSpecification;

/**
 * @author Ganeshan
 *
 * Provides the labelling for the scope.
 */
class ScopeLabelProvider extends LabelProvider implements ITableLabelProvider {
    public String getColumnText(Object obj, int index) {        
        if (obj instanceof ClassSpecification) {
            final ClassSpecification _cs = (ClassSpecification) obj;
            switch (index) {
            case 0:
                return "Class";
            case 1:
                return _cs.getName();
            case 2:
                return _cs.getTypeSpec().getNamePattern();
            }
        } else if (obj instanceof MethodSpecification) {
            final MethodSpecification _ms = (MethodSpecification) obj;
            switch (index) {
            case 0:
                return "Method";
            case 1:
                return _ms.getName();
            case 2:
                return _ms.getMethodNameSpec();
            }

        } else if (obj instanceof FieldSpecification) {
            final FieldSpecification _fs = (FieldSpecification) obj;
            switch (index) {
            case 0:
                return "Field";
            case 1:
                return _fs.getName();
            case 2:
                return _fs.getFieldNameSpec();
            }

        } else {
            return "";
        }
        return getText(obj);
    }

    public Image getColumnImage(Object obj, int index) {
        return null;
    }

    public Image getImage(Object obj) {
        return null;
    }
}