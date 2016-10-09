package org.cmdm;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

import org.knime.core.util.SwingWorkerWithContext;
import org.knime.base.node.util.BufferedFileReader;
//import org.knime.chem.base.node.io.sdf.SDFReaderSettings;
//import org.knime.chem.base.node.io.sdf.SDFReaderSettings.Property;
import org.cmdm.BBB_predictorNodeSettings.Property;
import org.knime.chem.base.util.sdf.SDFAnalyzer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.MultipleURLList;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;



/**
 * <code>NodeDialog</code> for the "BBB_predictor" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Yi Hsiao
 */
public class BBB_predictorNodeDialog extends NodeDialogPane {
	
	private class SdfScanner extends
	    SwingWorkerWithContext<Map<String, Class<?>>, Object[]> {
	private boolean m_molIdsAreUnique = true;
	
	@Override
	protected Map<String, Class<?>> doInBackgroundWithContext() throws Exception {
	    return scanFile();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doneWithContext() {
	    m_scanProgress.setIndeterminate(false);
	    m_urls.setEnabled(true);
	    m_startScan.setEnabled(true);
	    m_stopScan.setEnabled(false);
	    m_filesPanel.setCursor(Cursor
	            .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	
	    if (!m_molIdsAreUnique) {
	        m_useRowID.setSelected(false);
	    }
	
	    Map<String, Class<?>> properties;
	    try {
	        properties = get();
	    } catch (CancellationException ex) {
	        // ignore it
	        return;
	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(m_filesPanel,
	                "Could not extract properties: "
	                        + ex.getCause().getMessage(), "Error",
	                JOptionPane.ERROR_MESSAGE);
	        LOGGER.error("Could not properly extract properties", ex);
	        return;
	    }
	    m_propsModel.update(properties);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processWithContext(final List<Object[]> chunks) {
	    m_scanProgress.setIndeterminate(false);
	    Object[] last = chunks.get(chunks.size() - 1);
	
	    int progress = (Integer)last[0];
	    m_scanProgress.setValue(progress);
	
	    String text = (String)last[1];
	    m_scanProgress.setString(text);
	
	    Map<String, Class<?>> properties = (Map<String, Class<?>>)last[2];
	    m_propsModel.update(properties);
	}
	
	private Map<String, Class<?>> scanFile() throws Exception {
	    m_molIdsAreUnique = true;
	
	    Map<String, Class<?>> propertyTypes =
	            new LinkedHashMap<String, Class<?>>();
	    DuplicateChecker molIds = new DuplicateChecker();
	    List<URL> urls = new ArrayList<URL>();
	
	    // estimate how much is going to be read
	    long max = 0;
	    for (String s : m_urls.getSelectedURLs()) {
	        try {
	            URL u = MultipleURLList.convertToUrl(s);
	            urls.add(u);
	            BufferedFileReader in =
	                    BufferedFileReader.createNewReader(u);
	            max += in.getFileSize();
	            in.close();
	        } catch (MalformedURLException ex) {
	            JOptionPane.showMessageDialog(m_filesPanel,
	                    "File does not exist or malformed URL: " + s,
	                    "I/O Error", JOptionPane.ERROR_MESSAGE);
	            throw ex;
	        }
	    }
	
	    if (max > 0) {
	        m_scanProgress.setIndeterminate(false);
	    }
	
	    // read from all locations
	    int count = 0;
	    long globalRead = 0;
	    outer: for (URL url : urls) {
	        try {
	            BufferedFileReader in =
	                    BufferedFileReader.createNewReader(url);
	            m_scanProgress.setVisible(true);
	
	            SDFAnalyzer analyzer = new SDFAnalyzer(in);
	
	            String mol;
	            while (((mol = analyzer.nextMolecule()) != null)) {
	                if (Thread.currentThread().isInterrupted()) {
	                    break outer;
	                }
	                try {
	                    synchronized (propertyTypes) {
	                        String molTitle = BBB_predictorDefaultSDFReader.analyzeAndUpdateProperties(mol, propertyTypes);
	                        try {
	                            molIds.addKey(molTitle);
	                        } catch (DuplicateKeyException ex) {
	                            m_molIdsAreUnique = false;
	                        }
	                    }
	                    count++;
	
	                    publish(new Object[]{
	                            (int)Math.round(100.0
	                                    * (globalRead + in
	                                            .getNumberOfBytesRead())
	                                    / max),
	                            "Analyzed " + count + " molecules",
	                            propertyTypes});
	                } catch (Exception ex) {
	                    LOGGER.error(ex.getMessage(), ex);
	                    continue;
	                }
	            }
	            globalRead += in.getNumberOfBytesRead();
	        } catch (IOException ex) {
	            JOptionPane.showMessageDialog(
	                    m_filesPanel,
	                    "Error while reading from " + url + ": "
	                            + ex.getMessage(), "I/O Error",
	                    JOptionPane.ERROR_MESSAGE);
	            molIds.clear();
	            throw ex;
	        }
	    }
	    try {
	        molIds.checkForDuplicates();
	    } catch (DuplicateKeyException ex) {
	        m_molIdsAreUnique = false;
	    } finally {
	        molIds.clear();
	    }
	    return propertyTypes;
	}
	}
	
	private static class PropertiesTableModel extends AbstractTableModel {
	private final List<Property> m_properties = new ArrayList<Property>();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getColumnName(final int column) {
	    switch (column) {
	        case 0:
	            return "Extract?";
	        case 1:
	            return "Name";
	        case 2:
	            return "Type";
	        default:
	            return "???";
	    }
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getColumnClass(final int columnIndex) {
	    switch (columnIndex) {
	        case 0:
	            return Boolean.class;
	        case 1:
	            return String.class;
	        case 2:
	            return Class.class;
	        default:
	            return Object.class;
	    }
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
	    switch (columnIndex) {
	        case 0:
	            return true;
	        case 2:
	            return true;
	        default:
	            return false;
	    }
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getColumnCount() {
	    return 3;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRowCount() {
	    return m_properties.size();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
	    switch (columnIndex) {
	        case 0:
	            return m_properties.get(rowIndex).extract;
	        case 1:
	            return m_properties.get(rowIndex).name;
	        case 2:
	            return m_properties.get(rowIndex).type;
	        default:
	            return "???";
	    }
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setValueAt(final Object value, final int rowIndex,
	        final int columnIndex) {
	    switch (columnIndex) {
	        case 0:
	            m_properties.get(rowIndex).extract = (Boolean)value;
	            break;
	        case 2:
	            m_properties.get(rowIndex).type =
	                    (Class<? extends DataCell>)value;
	            break;
	    }
	}
	
	/**
	 * Updates the table model with the found properties.
	 *
	 * @param props a map of properties with the properties' names as keys
	 *            and their types (String, Double, Integer) as values.
	 */
	public void update(final Map<String, Class<?>> props) {
	    m_properties.clear();
	
	    synchronized (props) {
	        for (Map.Entry<String, Class<?>> e : props.entrySet()) {
	            m_properties
	                    .add(new Property(true, e.getKey(),
	                            (e.getValue() != null) ? e.getValue()
	                                    : String.class));
	        }
	    }
	
	    fireTableDataChanged();
	}
	
	/**
	 * Returns the properties shown in the table.
	 *
	 * @return a list of the shown properties
	 */
	public List<Property> getProperties() {
	    return m_properties;
	}
	
	/**
	 * Updates the table model with the given properties.
	 *
	 * @param props a list of properties
	 */
	public void update(final Iterable<Property> props) {
	    m_properties.clear();
	    for (Property p : props) {
	        m_properties.add(new Property(p));
	    }
	}
	}
	
	private static final NodeLogger LOGGER = NodeLogger
	    .getLogger(BBB_predictorNodeDialog.class);
	
	private final BBB_predictorNodeSettings m_settings = new BBB_predictorNodeSettings();
	
	private final MultipleURLList m_urls = new MultipleURLList(
	    "class org.knime.chem.base.node.io.sdf.SDFReaderNodeDialog", true,
	    "sdf", "SDF", "sdf.gz", "SDF.gz", "mol", "MOL", "mol.gz", "MOL.gz");
	
	private final JCheckBox m_useRowID = new JCheckBox("Use molecule name as row ID");
	
	private final JCheckBox m_extractName = new JCheckBox("Extract molecule name");
	
	private final JCheckBox m_extractSDF = new JCheckBox("Extract SDF blocks");
	
	private final JCheckBox m_extractMol = new JCheckBox("Extract Mol blocks");
	
	private final JCheckBox m_extractCTab = new JCheckBox("Extract CTab blocks");
	
	private final JCheckBox m_extractCounts = new JCheckBox("Extract counts");
	
	private final JCheckBox m_isLogBBclassify = new JCheckBox("Predict LogBB Classification");
	
	private final JCheckBox m_isLogPSclassify = new JCheckBox("Predict LogPS Classification");
	
	private final JCheckBox m_isLogBBreg = new JCheckBox("Predict LogBB Regression");
	
	private final JCheckBox m_isLogPSreg = new JCheckBox("Predict LogPS Regression");
	
	private final JCheckBox m_addSourceLocationColumn = new JCheckBox(
	    "Add column with source location");
	
	private final JCheckBox m_limitMolecules = new JCheckBox(
	    "Limit number of read molecules");
	
	private final JCheckBox m_extractAllProperties = new JCheckBox(
	    "Extract all properties");
	
	private final JSpinner m_limit = new JSpinner(new SpinnerNumberModel(100,
	    1, Integer.MAX_VALUE, 1));
	
	private final PropertiesTableModel m_propsModel =
	    new PropertiesTableModel();
	
	private final JTable m_propertiesTable = new JTable(m_propsModel);
	
	private final JPanel m_filesPanel = new JPanel(new GridBagLayout());
	
	private final JPanel m_propertiesPanel = new JPanel(new GridBagLayout());
	
	private final JProgressBar m_scanProgress = new JProgressBar();
	
	private final JButton m_startScan = new JButton("Scan files");
	
	private final JButton m_stopScan = new JButton("Stop scanning");
	
	private SdfScanner m_sdfScanner;
	
	private final JButton m_selectAll = new JButton("Select all");
	
	private final JButton m_deselectAll = new JButton("Deselect all");
	
	/**
	* Creates a new new dialog.
	*/
	public BBB_predictorNodeDialog() {
		createFilePanel();
		addTab("File selection", m_filesPanel);
//		createPropertiesPanel();
//		addTab("Property handling", m_propertiesPanel);
	}
	
	private void createPropertiesPanel() {
	JPanel p = m_propertiesPanel;
	GridBagConstraints c = new GridBagConstraints();
	
	c.gridx = 0;
	c.gridy = 0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.NONE;
	c.insets = new Insets(0, 2, 0, 2);
	c.weightx = 0;
	c.weighty = 0;
	
	c.gridwidth = 2;
	m_extractAllProperties.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
	        m_startScan.setEnabled(!m_extractAllProperties.isSelected());
	        m_propertiesTable.setEnabled(!m_extractAllProperties
	                .isSelected());
	        m_selectAll.setEnabled(!m_extractAllProperties.isSelected());
	        m_deselectAll.setEnabled(!m_extractAllProperties.isSelected());
	    }
	});
	p.add(m_extractAllProperties, c);
	
	c.gridy++;
	c.gridwidth = 1;
	p.add(m_startScan, c);
	m_startScan.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
	        if (m_urls.getSelectedURLs().size() < 1) {
	            JOptionPane.showMessageDialog(m_filesPanel,
	                    "No files selected", "User Error",
	                    JOptionPane.WARNING_MESSAGE);
	            return;
	        }
	
	        m_startScan.setEnabled(false);
	        m_stopScan.setEnabled(true);
	        m_urls.setEnabled(false);
	        m_scanProgress.setIndeterminate(true);
	        m_scanProgress.setString("Preparing to read...");
	        m_filesPanel.setCursor(Cursor
	                .getPredefinedCursor(Cursor.WAIT_CURSOR));
	        Map<String, Class<?>> properties =
	                new HashMap<String, Class<?>>();
	        m_propsModel.update(properties);
	
	        if (m_sdfScanner != null) {
	            m_sdfScanner.cancel(true);
	        }
	        m_sdfScanner = new SdfScanner();
	        m_sdfScanner.execute();
	    }
	});
	
	c.gridx = 1;
	p.add(m_stopScan, c);
	m_stopScan.setEnabled(false);
	m_stopScan.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
	        m_sdfScanner.cancel(true);
	    }
	});
	
	c.gridx = 2;
	c.insets = new Insets(4, 0, 4, 0);
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1;
	m_scanProgress.setPreferredSize(new Dimension(10, 20));
	m_scanProgress.setStringPainted(true);
	m_scanProgress.setString("");
	p.add(m_scanProgress, c);
	
	c.gridx = 0;
	c.gridy++;
	c.gridwidth = 3;
	c.insets = new Insets(0, 0, 0, 0);
	c.fill = GridBagConstraints.BOTH;
	c.weighty = 1;
	
	JScrollPane sp = new JScrollPane(m_propertiesTable);
	p.add(sp, c);
	
	TableColumn extractColumn =
	        m_propertiesTable.getColumnModel().getColumn(0);
	extractColumn.setMaxWidth(70);
	
	TableColumn typeColumn =
	        m_propertiesTable.getColumnModel().getColumn(2);
	typeColumn.setMaxWidth(80);
	
	m_propertiesTable.setRowHeight(22);
	
	JComboBox comboBox = new JComboBox();
	comboBox.setRenderer(new DefaultListCellRenderer() {
	    @Override
	    public Component getListCellRendererComponent(final JList list,
	            final Object value, final int index,
	            final boolean isSelected, final boolean cellHasFocus) {
	        String typeText = ((Class<?>)value).getSimpleName();
	        return super.getListCellRendererComponent(list, typeText,
	                index, isSelected, cellHasFocus);
	    }
	
	});
	comboBox.addItem(String.class);
	comboBox.addItem(Integer.class);
	comboBox.addItem(Double.class);
	typeColumn.setCellEditor(new DefaultCellEditor(comboBox));
	typeColumn.setCellRenderer(new DefaultTableCellRenderer() {
	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public Component getTableCellRendererComponent(final JTable table,
	            final Object value, final boolean isSelected,
	            final boolean hasFocus, final int row, final int column) {
	        String typeText = ((Class<?>)value).getSimpleName();
	        return super.getTableCellRendererComponent(table, typeText,
	                isSelected, hasFocus, row, column);
	    }
	});
	
	JPanel selectPanel = new JPanel(new GridBagLayout());
	
	c.gridx = 0;
	c.gridy++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weighty = 0;
	p.add(selectPanel, c);
	
	m_selectAll.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
	        for (int i = 0; i < m_propsModel.getRowCount(); i++) {
	            m_propsModel.setValueAt(Boolean.TRUE, i, 0);
	        }
	        m_propsModel.fireTableDataChanged();
	    }
	});
	
	GridBagConstraints c2 = new GridBagConstraints();
	c2.gridx = 0;
	c2.gridy = 0;
	c2.anchor = GridBagConstraints.CENTER;
	c2.insets = new Insets(0, 2, 0, 2);
	c2.fill = GridBagConstraints.HORIZONTAL;
	c2.weightx = 0.5;
	selectPanel.add(m_selectAll, c2);
	
	m_deselectAll.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
	        for (int i = 0; i < m_propsModel.getRowCount(); i++) {
	            m_propsModel.setValueAt(Boolean.FALSE, i, 0);
	        }
	        m_propsModel.fireTableDataChanged();
	    }
	});
	
	c2.gridx = 1;
	selectPanel.add(m_deselectAll, c2);
	}
	
	private void createFilePanel() {
	JPanel p = m_filesPanel;
	
	GridBagConstraints c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 0.5;
	c.weighty = 0.5;
	c.gridwidth = 2;
	p.add(m_urls, c);
	
//	JPanel p2 = new JPanel(new GridBagLayout());
//	GridBagConstraints c2 = new GridBagConstraints();
//	p2.add(m_limitMolecules, c2);
//	((JSpinner.NumberEditor)m_limit.getEditor()).getTextField().setColumns(8);
//	p2.add(m_limit, c2);
	
//	m_limitMolecules.addActionListener(new ActionListener() {
//	    @Override
//	    public void actionPerformed(final ActionEvent e) {
//	        m_limit.setEnabled(m_limitMolecules.isSelected());
//	    }
//	});
//	m_limit.setEnabled(false);
	
	c.fill = GridBagConstraints.NONE;
	c.weightx = 0;
	c.weighty = 0;
//	c.gridy++;
//	p.add(p2, c);
	
	c.gridwidth = 1;
//	c.gridy++;
//	p.add(m_useRowID, c);
	
//	c.gridy++;
//	p.add(m_extractName, c);
	
//	c.gridy++;
//	p.add(m_addSourceLocationColumn, c);
	c.insets = new Insets(0, 30, 0, 0);
	c.gridx = 1;
	c.gridy = 2;
//	p.add(m_extractSDF, c);
	
//	c.gridy++;
//	p.add(m_extractMol, c);
	
//	c.gridy++;
//	p.add(m_extractCTab, c);
	
//	c.gridy++;
//	p.add(m_extractCounts, c);
	
	c.gridy++;
	p.add(m_isLogBBclassify, c);
	
	c.gridy++;
	p.add(m_isLogPSclassify, c);
	
	c.gridy++;
	p.add(m_isLogBBreg, c);
	
	c.gridy++;
	p.add(m_isLogPSreg, c);

	}
	
	/**
	* {@inheritDoc}
	*/
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
	    final DataTableSpec[] specs) throws NotConfigurableException {
	m_settings.loadSettingsForDialog(settings);
	
	m_urls.setSelectedURLs(m_settings.urls());
	m_useRowID.setSelected(m_settings.useRowID());
	m_extractName.setSelected(m_settings.extractName());
	m_extractSDF.setSelected(m_settings.extractSDF());
	m_extractMol.setSelected(m_settings.extractMol());
	m_extractCTab.setSelected(m_settings.extractCtab());
	m_extractCounts.setSelected(m_settings.extractCounts());
	m_addSourceLocationColumn.setSelected(m_settings.sourceLocationColumn());
	m_limitMolecules.setSelected(m_settings.limitNumberOfMolecules());
	m_limit.setValue(m_settings.moleculeLimit());
	m_limit.setEnabled(m_settings.limitNumberOfMolecules());
	m_extractAllProperties.setSelected(m_settings.extractAllProperties());

	m_isLogBBclassify.setSelected(m_settings.isLogBBclassify());
	m_isLogPSclassify.setSelected(m_settings.isLogPSclassify());
	m_isLogBBreg.setSelected(m_settings.isLogBBreg());
	m_isLogPSreg.setSelected(m_settings.isLogPSreg());
	
	m_propsModel.update(m_settings.properties());
	}
	
	/**
	* {@inheritDoc}
	*/
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
	    throws InvalidSettingsException {
	List<URL> urls = new ArrayList<URL>();
	for (String url : m_urls.getSelectedURLs()) {
	    try {
	        urls.add(MultipleURLList.convertToUrl(url));
	    } catch (MalformedURLException ex) {
	        throw new InvalidSettingsException(
	                "Malformed URL or non-existing file: " + url);
	    }
	}
	
	m_settings.urls(urls);
	m_settings.useRowID(m_useRowID.isSelected());
	m_settings.extractName(m_extractName.isSelected());
	m_settings.extractSDF(m_extractSDF.isSelected());
	m_settings.extractMol(m_extractMol.isSelected());
	m_settings.extractCTab(m_extractCTab.isSelected());
	m_settings.extractCounts(m_extractCounts.isSelected());
	m_settings.sourceLocationColumn(m_addSourceLocationColumn.isSelected());
	m_settings.limitNumberOfMolecules(m_limitMolecules.isSelected());
	m_settings.moleculeLimit((Integer)m_limit.getValue());
	m_settings.extractAllProperties(m_extractAllProperties.isSelected());

	m_settings.isLogBBclassify(m_isLogBBclassify.isSelected());
	m_settings.isLogPSclassify(m_isLogPSclassify.isSelected());
	m_settings.isLogBBreg(m_isLogBBreg.isSelected());
	m_settings.isLogPSreg(m_isLogPSreg.isSelected());
	
	m_settings.clearProperties();
	for (BBB_predictorNodeSettings.Property p : m_propsModel.getProperties()) {
	    m_settings.addProperty(new BBB_predictorNodeSettings.Property(p));
	}
	
	m_settings.saveSettings(settings);
	}

	/**
	* {@inheritDoc}
	*/
	@Override
	public void onClose() {
	super.onClose();
		if (m_sdfScanner != null) {
			m_sdfScanner.cancel(true);
		}
	}
	
	
}

