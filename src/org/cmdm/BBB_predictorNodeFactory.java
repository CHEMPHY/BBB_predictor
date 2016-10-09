package org.cmdm;

import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BBB_predictor" Node.
 * 
 *
 * @author Yi Hsiao
 */
public class BBB_predictorNodeFactory extends ContextAwareNodeFactory<BBB_predictorNodeModel>{
	
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new BBB_predictorNodeDialog();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public BBB_predictorNodeModel createNodeModel() {
        return new BBB_predictorNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<BBB_predictorNodeModel> createNodeView(final int viewIndex,final BBB_predictorNodeModel nodeModel) {
    	return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }
    
    @Override
    public BBB_predictorNodeModel createNodeModel(final NodeCreationContext context) {
        return new BBB_predictorNodeModel(context);
    }

}

