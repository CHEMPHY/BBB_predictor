package org.cmdm;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;


import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;

import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.descriptors.molecular.ALOGPDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.TPSADescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.BCUTDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.MannholdLogPDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;

/**
 * This is the model implementation of BBB_predictor.
 * 
 *
 * @author Yi Hsiao
 */
public class BBB_predictorNodeModel extends NodeModel {
	private final BBB_predictorNodeSettings m_settings = new BBB_predictorNodeSettings();
	private final BBB_predictorDefaultSDFReader m_sdfReader = new BBB_predictorDefaultSDFReader(m_settings);
    
    /**
     * Constructor for the node model.
     */
    public BBB_predictorNodeModel() {
        // TODO: Specify the amount of input and output ports needed.
        super(0, 2);
    }
    
    BBB_predictorNodeModel(final NodeCreationContext context) {
    	this();
        m_settings.urls(Collections.singletonList(context.getUrl()));
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // TODO: Return a BufferedDataTable for each output port 
    	BufferedDataTable[] result = m_sdfReader.execute(exec);
        String warningMessage = m_sdfReader.clearWarningMessage();
        if (warningMessage != null) {
            setWarningMessage(warningMessage);
        }
        BufferedDataTable table = result[0];
        long rowCount = table.size();
        List<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
        cols.add(table.getDataTableSpec().getColumnSpec(0));
        int colCount = 1;
        if(m_settings.isLogBBclassify()){
        	cols.add(new DataColumnSpecCreator("LogBBclassify", IntCell.TYPE).createSpec());
        	colCount++;
        }
        if(m_settings.isLogPSclassify()){
        	cols.add(new DataColumnSpecCreator("LogPSclassify", IntCell.TYPE).createSpec());
        	colCount++;
        }
        if(m_settings.isLogBBreg()){
        	cols.add(new DataColumnSpecCreator("LogBBreg", DoubleCell.TYPE).createSpec());
        	colCount++;
        }
        if(m_settings.isLogPSreg()){
        	cols.add(new DataColumnSpecCreator("LogPSreg", IntCell.TYPE).createSpec());
        	colCount++;
        }
        DataTableSpec spec = new DataTableSpec(cols.toArray(new DataColumnSpec[cols.size()]));
        BufferedDataContainer buf = exec.createDataContainer(spec);
        CloseableRowIterator iter = table.iterator();
	        for (int j = 0; j < rowCount; j++) {
	        	DataRow mol = iter.next();
	            DataCell[] cells = new DataCell[colCount];
	            cells[0] = mol.getCell(0);
	            int i = 1;
	            String sdf = mol.getCell(0).toString();
	            IteratingSDFReader reader = new IteratingSDFReader(
	            		new ByteArrayInputStream(sdf.getBytes("UTF-8")), DefaultChemObjectBuilder.getInstance());
	            IAtomContainer molecule = (IAtomContainer)reader.next();
				IMolecularDescriptor ALOGP = new ALOGPDescriptor();
				IMolecularDescriptor TPSA = new TPSADescriptor();
				IMolecularDescriptor BCUT = new BCUTDescriptor();
				IMolecularDescriptor MANNHOLDLOGP = new MannholdLogPDescriptor();
				Double alogp = Double.parseDouble(ALOGP.calculate(molecule).getValue().toString().split(",")[0]);
				Double tpsa = Double.parseDouble(TPSA.calculate(molecule).getValue().toString());
				Double bcuts = Double.parseDouble(BCUT.calculate(molecule).getValue().toString().split(",")[0]);
				Double MannholdLOGP = Double.parseDouble(MANNHOLDLOGP.calculate(molecule).getValue().toString());
	            if(m_settings.isLogBBclassify()){
	            	cells[i] = new IntCell(1);
	            	i++;
	            }
	            if(m_settings.isLogPSclassify()){
	            	if(alogp<=-0.4263){
	    				if(bcuts<=11.9){
	    	            	cells[i] = new IntCell(1);
	    				}else{
	    	            	cells[i] = new IntCell(-1);
	    				}
	    			}else{
	    				if(tpsa<=150.54553){
	    	            	cells[i] = new IntCell(1);
	    				}else{
	    	            	cells[i] = new IntCell(-1);
	    				}
	    			}
	            	i++;
	            }
	            if(m_settings.isLogBBreg()){
	            	cells[i] = new DoubleCell(0.05-0.011*tpsa+0.19*MannholdLOGP);
	            	i++;
	            }
	            if(m_settings.isLogPSreg()){
	            	cells[i] = new IntCell(1);
	            }
	            DataRow row = new DefaultRow(new String("Row" + j), cells);
	            buf.addRowToTable(row);
	        }
	        buf.close();
	        result[0] = buf.getTable();
	        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // TODO: generated method stub
    	DataTableSpec[] result = m_sdfReader.configure();
        String warningMessage = m_sdfReader.clearWarningMessage();
        if (warningMessage != null) {
            setWarningMessage(warningMessage);
        }
        DataTableSpec mol = result[0];
        List<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
        if(m_settings.isLogBBclassify()){
        	cols.add(new DataColumnSpecCreator("LogBBclassify", IntCell.TYPE).createSpec());
        }
        if(m_settings.isLogPSclassify()){
        	cols.add(new DataColumnSpecCreator("LogPSclassify", IntCell.TYPE).createSpec());
        }
        if(m_settings.isLogBBreg()){
        	cols.add(new DataColumnSpecCreator("LogBBreg", DoubleCell.TYPE).createSpec());
        }
        if(m_settings.isLogPSreg()){
        	cols.add(new DataColumnSpecCreator("LogPSreg", IntCell.TYPE).createSpec());
        }
        DataTableSpec spec = new DataTableSpec(cols.toArray(new DataColumnSpec[cols.size()]));
        result[0] = new DataTableSpec(mol,spec);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         // TODO: generated method stub
//    	m_settings.saveSettings(settings);
    	m_sdfReader.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    	m_sdfReader.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    	m_sdfReader.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    	m_sdfReader.loadInternals();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    	
    }

}

