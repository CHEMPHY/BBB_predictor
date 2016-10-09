/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   12.05.2011 (meinl): created
 */
package org.cmdm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.util.BufferedFileReader;
import org.cmdm.BBB_predictorNodeSettings.Property;
import org.knime.chem.base.util.MolSyntaxException;
import org.knime.chem.base.util.sdf.DataItem;
import org.knime.chem.base.util.sdf.SDFAnalyzer;
import org.knime.chem.base.util.sdf.SDFBlock;
import org.knime.chem.types.CtabCell;
import org.knime.chem.types.CtabCellFactory;
import org.knime.chem.types.MolAdapterCell;
import org.knime.chem.types.MolCellFactory;
import org.knime.chem.types.SdfAdapterCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;
import org.knime.core.util.UniqueNameGenerator;

/**
 * This class bundles several methods for reading an SDF file into a data table.
 *
 * @since 2.4
 * @author Thorsten Meinl, University of Konstanz
 */
public class BBB_predictorDefaultSDFReader {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(BBB_predictorDefaultSDFReader.class);

    /** Name of structure column. */
    public static final String MOLECULE_COLUMN = "Molecule";

    /** Name of structure name column. */
    public static final String MOLECULE_NAME_COLUMN = "Molecule name";

//    private final SDFReaderSettings m_settings;
    private final BBB_predictorNodeSettings m_settings;
    private String m_warningMessage;

    /**
     * Creates new reader, providing settings object as argument.
     *
     * @param settings The settings to use.
     */
    public BBB_predictorDefaultSDFReader(final BBB_predictorNodeSettings settings) {
        if (settings == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_settings = settings;
    }

    /**
     * @param warningMessage the warningMessage to set
     */
    protected void setWarningMessage(final String warningMessage) {
        m_warningMessage = warningMessage;
    }

    /**
     * @return the warningMessage
     */
    public String clearWarningMessage() {
        String message = m_warningMessage;
        m_warningMessage = null;
        return message;
    }

    /**
     * Performs the node's configure step. Returns a spec representing the
     * output table specs.
     *
     * @return An array of length 2, the 1st element represents the data, which
     *         could be read successfully, the 2nd element the failed ones.
     * @throws InvalidSettingsException If the configuration is invalid.
     */
    public DataTableSpec[] configure() throws InvalidSettingsException {
        if (m_settings.urls().size() < 1) {
            throw new InvalidSettingsException("No file selected");
        }
        for (URL u : m_settings.urls()) {
            try {
                FileUtil.openStreamWithTimeout(u).close();
            } catch (IOException ex) {
                throw new InvalidSettingsException("Cannot access '" + u
                        + "': " + ex.getMessage(), ex);
            }
        }

        if (m_settings.extractAllProperties()) {
            return new DataTableSpec[]{null, createBrokenSpec()};
        } else {
            return new DataTableSpec[]{
                    createSuccessfulSpec(m_settings.properties()),
                    createBrokenSpec()};
        }
    }

    private Collection<Property> determineProperties(
            final ExecutionMonitor exec, final double max) throws IOException,
            CanceledExecutionException {
        exec.setMessage("Determining properties");
        List<Property> props = new ArrayList<Property>();

        Map<String, Class<?>> propertyTypes =
                new LinkedHashMap<String, Class<?>>();
        long readBytes = 0;
        int readMolecules = 0;
        outer: for (URL url : m_settings.urls()) {
            BufferedFileReader in = BufferedFileReader.createNewReader(url);

            SDFAnalyzer analyzer = new SDFAnalyzer(in);

            String mol;
            while ((mol = analyzer.nextMolecule()) != null) {
                exec.checkCanceled();
                exec.setProgress((readBytes + in.getNumberOfBytesRead()) / max);
                try {
                    analyzeAndUpdateProperties(mol, propertyTypes);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    continue;
                }
                readMolecules++;
                if (m_settings.limitNumberOfMolecules()
                        && (readMolecules >= m_settings.moleculeLimit())) {
                    break outer;
                }
            }
            readBytes += in.getNumberOfBytesRead();
        }
        for (Map.Entry<String, Class<?>> e : propertyTypes.entrySet()) {
            props.add(new Property(true, e.getKey(), (e.getValue() != null) ? e
                    .getValue() : String.class));
        }
        return props;
    }

    /**
     * Creates the output spec for good molecules based on the settings from the
     * dialog.
     *
     * @return the output spec
     */
    private DataTableSpec createSuccessfulSpec(
            final Collection<Property> properties) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        Set<String> empty = Collections.emptySet();
        UniqueNameGenerator namer = new UniqueNameGenerator(empty);

        if (m_settings.extractSDF()) {
            colSpecs.add(namer.newColumn(MOLECULE_COLUMN, SdfAdapterCell.RAW_TYPE));
        }

        if (m_settings.extractMol()) {
            colSpecs.add(namer.newColumn("Mol Block", MolAdapterCell.RAW_TYPE));
        }

        if (m_settings.extractCtab()) {
            colSpecs.add(namer.newColumn("Ctab Block", CtabCell.TYPE));
        }

        if (m_settings.extractName()) {
            colSpecs.add(namer.newColumn(
                    MOLECULE_NAME_COLUMN, StringCell.TYPE));
        }

        if (m_settings.extractCounts()) {
            colSpecs.add(namer.newColumn("Atom count", IntCell.TYPE));
            colSpecs.add(namer.newColumn("Bond count", IntCell.TYPE));
        }

        for (Property p : properties) {
            if (p.extract) {
                DataType type;
                if (p.type == Integer.class) {
                    type = IntCell.TYPE;
                } else if (p.type == Double.class) {
                    type = DoubleCell.TYPE;
                } else {
                    type = StringCell.TYPE;
                }

                colSpecs.add(namer.newColumn(p.name, type));
            }
        }

        if (m_settings.sourceLocationColumn()) {
            colSpecs.add(namer.newColumn("Source location", StringCell.TYPE));
        }

        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs
                .size()]));
    }

    /**
     * Creates the output spec for unparseable molecules based on the settings
     * from the dialog.
     *
     * @return the output spec
     */
    private DataTableSpec createBrokenSpec() {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();

        colSpecs.add(new DataColumnSpecCreator("SDF string", StringCell.TYPE)
                .createSpec());
        colSpecs.add(new DataColumnSpecCreator("Error", StringCell.TYPE)
                .createSpec());

        if (m_settings.sourceLocationColumn()) {
            colSpecs.add(new DataColumnSpecCreator("Source file",
                    StringCell.TYPE).createSpec());
        }

        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs
                .size()]));
    }

    /**
     * Performs a node's execute method. Reads the data and returns the data as
     * described in the {@link #configure()} method.
     *
     * @param exec context for progress report, cancellation and table creation.
     * @return The data being read.
     * @throws Exception If that fails for any reason.
     */
    public BufferedDataTable[] execute(final ExecutionContext exec)
            throws Exception {
        double max = 0;
        if (m_settings.limitNumberOfMolecules()) {
            max = m_settings.moleculeLimit();
        } else {
            int readFiles = 0;
            for (URL url : m_settings.urls()) {
                readFiles++;
                BufferedFileReader in = BufferedFileReader.createNewReader(url);
                long size = in.getFileSize();
                if (size > 0) {
                    max += size;
                } else {
                    // estimate
                    max += (max / readFiles);
                }
                in.close();
            }
        }

        Collection<Property> properties;
        ExecutionMonitor subExec;
        if (m_settings.extractAllProperties()) {
            properties = determineProperties(exec.createSubProgress(0.5), max);
            subExec = exec.createSubProgress(0.5);
        } else {
            properties = m_settings.properties();
            subExec = exec;
        }
        DataTableSpec outSpec = createSuccessfulSpec(properties);

        BufferedDataContainer cont =
                createSuccessfulOutputContainer(exec, outSpec);
        BufferedDataContainer brokenCont =
                exec.createDataContainer(createBrokenSpec(), false, 0);

        long readMolecules = -1;
        long readBytes = 0;
        int readFiles = 0;
        outer: for (URL url : m_settings.urls()) {
            BufferedFileReader in = BufferedFileReader.createNewReader(url);
            readFiles++;
            SDFAnalyzer analyzer = new SDFAnalyzer(in);

            String mol;
            while ((mol = analyzer.nextMolecule()) != null) {
                subExec.checkCanceled();
                readMolecules++;
                if (m_settings.limitNumberOfMolecules()
                        && (readMolecules >= m_settings.moleculeLimit())) {
                    in.close();
                    break outer;
                }

                if (max > 0) {
                    double p;
                    if (m_settings.limitNumberOfMolecules()) {
                        p = readMolecules / max;
                    } else {
                        p = (readBytes + in.getNumberOfBytesRead()) / max;
                    }
                    subExec.setProgress(p, "Read " + readMolecules
                            + " molecules");
                } else {
                    subExec.setMessage("Read " + readMolecules + " molecules");
                }

                SDFBlock sdf;
                try {
                    sdf = SDFAnalyzer.analyzeSDF(mol);
                } catch (Exception ex) {
                    String errMessage = ex.getMessage();
                    if (errMessage == null) {
                        errMessage = "<no error message available>";
                    }

                    LOGGER.warn(errMessage, ex);
                    DataCell[] cells =
                            new DataCell[m_settings.sourceLocationColumn() ? 3
                                    : 2];
                    cells[0] = new StringCell(mol);
                    cells[1] = new StringCell(errMessage);
                    if (m_settings.sourceLocationColumn()) {
                        cells[2] =
                                new StringCell(m_settings.urls()
                                        .get(readFiles - 1).toString());
                    }

                    brokenCont.addRowToTable(new DefaultRow(RowKey
                            .createRowKey(readMolecules), cells));
                    continue;
                }

                DataCell[] cells = new DataCell[outSpec.getNumColumns()];

                int k = 0;
                if (m_settings.extractSDF()) {
                    cells[k++] = SdfCellFactory.createAdapterCell(mol);
                }

                if (m_settings.extractMol()) {
                    cells[k++] = MolCellFactory.createAdapterCell(sdf.getMolfileBlock().toString());
                }
                if (m_settings.extractCtab()) {
                    cells[k++] =
                            CtabCellFactory.create(sdf.getMolfileBlock()
                                    .getCtabBlock().toString());
                }

                if (m_settings.extractName()) {
                    cells[k++] =
                            new StringCell(sdf.getMolfileBlock().getTitle());
                }

                if (m_settings.extractCounts()) {
                    cells[k++] =
                            new IntCell(sdf.getMolfileBlock().getCtabBlock()
                                    .getAtomCount());
                    cells[k++] =
                            new IntCell(sdf.getMolfileBlock().getCtabBlock()
                                    .getBondCount());
                }

                Map<String, DataItem> props = sdf.getProperties();

                for (Property p : properties) {
                    if (p.extract) {
                        DataItem item = props.get(p.name);
                        if (item == null) {
                            cells[k++] = DataType.getMissingCell();
                        } else {
                            Object v = props.get(p.name).getValue();
                            if (v == null) {
                                cells[k++] = DataType.getMissingCell();
                            } else if (p.type == Integer.class) {
                                cells[k++] =
                                        new IntCell(((Number)v).intValue());
                            } else if (p.type == Double.class) {
                                cells[k++] =
                                        new DoubleCell(
                                                ((Number)v).doubleValue());
                            } else {
                                cells[k++] =
                                        new StringCell(props.get(p.name)
                                                .getUnparsedValue());
                            }
                        }
                    }
                }

                if (m_settings.sourceLocationColumn()) {
                    cells[k++] = new StringCell(url.toString());
                }

                // encapsulate the row key string in a new string to truncate
                // the data (see bug #1737)
                RowKey key =
                        m_settings.useRowID() ? new RowKey(new String(sdf
                                .getMolfileBlock().getTitle())) : RowKey
                                .createRowKey(readMolecules);

                cont.addRowToTable(new DefaultRow(key, cells));
            }
            readBytes += in.getNumberOfBytesRead();
        }

        cont.close();
        brokenCont.close();
        BufferedDataTable brokenTbl = brokenCont.getTable();
        if (brokenTbl.size() > 0) {
            setWarningMessage("Failed to parse " + brokenTbl.size()
                    + " record(s)");
        }
        return new BufferedDataTable[]{cont.getTable(), brokenCont.getTable()};
    }

    /**
     * Represents a node' loadInternals method. It checks whether the URL used
     * in the execute method is still accessible and, if not, sets an
     * appropriate warning message.
     */
    public void loadInternals() {
        /*
         * This is a special "deal" for the file reader: The file reader, if
         * previously executed, has data at it's output - even if the file that
         * was read doesn't exist anymore. In order to warn the user that the
         * data cannot be recreated we check here if the file exists and set a
         * warning message if it doesn't.
         */
        for (URL location : m_settings.urls()) {
            try {
                if (!"file".equals(location.getProtocol())) {
                    // We can only check files. Other protocols are ignored.
                    return;
                }

                InputStream inStream = location.openStream();
                if (inStream == null) {
                    setWarningMessage("The file '" + location.toString()
                            + "' can't be accessed anymore!");
                } else {
                    inStream.close();
                }
            } catch (IOException ioe) {
                setWarningMessage("The file '" + location
                        + "' can't be accessed anymore!");
            } catch (NullPointerException npe) {
                // thats a bug in the windows open stream
                // a path like c:\blah\ \ (space as dir) causes a NPE.
                setWarningMessage("The file '" + location
                        + "' can't be accessed anymore!");
            }
        }
    }

    /**
     * Loads settings, which passed the
     * {@link #validateSettings(NodeSettingsRO)} test.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If settings are incomplete.
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * Saves settings to argument.
     *
     * @param settings to save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * Validates completeness of argument settings.
     *
     * @param settings To validate
     * @throws InvalidSettingsException If validation fails.
     */
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        BBB_predictorNodeSettings s = new BBB_predictorNodeSettings();
        s.loadSettings(settings);
    }

    /**
     * Create the output container hosting the output data for the first port
     * (successfully parsed molecules). Subclasses can overwrite this method if
     * they want to have a custom memory policy (this implementation uses the
     * user-set memory policy).
     *
     * @param exec The create a container from.
     * @param spec The output spec for the output data.
     * @return A new data container that is filled by the
     *         {@link #execute(ExecutionContext)} method.
     */
    protected BufferedDataContainer createSuccessfulOutputContainer(
            final ExecutionContext exec, final DataTableSpec spec) {
        return exec.createDataContainer(spec, false, 0);
    }

    /**
     * Create the output container hosting the output data for the second port
     * (unparseable molecules). Subclasses can overwrite this method if they
     * want to have a custom memory policy (this implementation uses the
     * user-set memory policy).
     *
     * @param exec The create a container from.
     * @param spec The output spec for the output data.
     * @return A new data container that is filled by the
     *         {@link #execute(ExecutionContext)} method.
     */
    protected BufferedDataContainer createBrokenOutputContainer(
            final ExecutionContext exec, final DataTableSpec spec) {
        return exec.createDataContainer(spec);
    }

    static String analyzeAndUpdateProperties(final String molString,
            final Map<String, Class<?>> propertyTypes)
            throws MolSyntaxException {
        SDFBlock sdf = SDFAnalyzer.analyzeSDF(molString);

        for (Map.Entry<String, DataItem> p : sdf.getProperties().entrySet()) {
            Class<?> clazz = propertyTypes.get(p.getKey());
            if (clazz == null) {
                if (p.getValue().getValue() != null) {
                    propertyTypes.put(p.getKey(), p.getValue().getValue()
                            .getClass());
                } else {
                    propertyTypes.put(p.getKey(), null);
                }
            } else {
                Object value = p.getValue().getValue();
                if ((value != null) && value.getClass() != clazz) {
                    if (clazz == Integer.class) {
                        propertyTypes.put(p.getKey(), value.getClass());
                    } else if (value.getClass() == String.class) {
                        propertyTypes.put(p.getKey(), String.class);
                    }
                }
            }
        }
        return sdf.getMolfileBlock().getTitle();
    }
}
