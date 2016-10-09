package org.cmdm;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

public class BBB_predictorNodeSettings {
	public static class Property {
		/**
         * <code>true</code> if this property should be extracted,
         * <code>false</code> otherwise.
         */
        public boolean extract;

        /** The property's name. */
        public final String name;

        /**
         * The property's type, either {@link Integer}, {@link Double} or
         * {@link String} as last resort.
         */
        public Class<?> type;

        /**
         * Creates a new property.
         *
         * @param extract <code>true</code> if this property should be
         *            extracted, <code>false</code> otherwise
         * @param name the property's name
         * @param type the property's type, either {@link Integer},
         *            {@link Double} or {@link String} as last resort
         */
        public Property(final boolean extract, final String name,
                final Class<?> type) {
            this.extract = extract;
            this.name = name;
            this.type = type;
        }

        /**
         * Creates a new property as a copy of the given property.
         *
         * @param copy the property that should be copied
         */
        public Property(final Property copy) {
            this.extract = copy.extract;
            this.name = copy.name;
            this.type = copy.type;
        }
	}
	
	private boolean m_useRowID = false;

    private boolean m_extractName;

    private boolean m_extractSDF = true;

    private boolean m_extractMol;

    private boolean m_extractCTab;

    private boolean m_extractCounts;

    private boolean m_sourceLocationColumn;

    private boolean m_limitMolecules;

    private int m_limit;

    private boolean m_extractAllProperties;

    private final List<URL> m_urls = new ArrayList<URL>();

    private List<Property> m_properties = new ArrayList<Property>();
    
	private String m_colName = "Molecules";
	
	private boolean m_isLogBBclassify = true;
	
	private boolean m_isLogPSclassify = true;
	
	private boolean m_isLogBBreg = true;
	
	private boolean m_isLogPSreg = true;

    
	
    /**
     * Returns if all properties should be extracted, thus ignoring
     * {@link #properties()}.
     *
     * @return <code>true</code> if all properties should be extracted,
     *         <code>false</code> otherwise
     */
    public boolean extractAllProperties() {
        return m_extractAllProperties;
    }

    /**
     * Sets if all properties should be extracted, thus ignoring
     * {@link #properties()}.
     *
     * @param b <code>true</code> if all properties should be extracted,
     *            <code>false</code> otherwise
     */
    public void extractAllProperties(final boolean b) {
        m_extractAllProperties = b;
    }
    
    /**
     * Returns if the number of molecules to read should be limited.
     *
     * @return <code>true</code> if the number should be limited,
     *         <code>false</code> otherwise
     * @since 2.4
     */
    public boolean limitNumberOfMolecules() {
        return m_limitMolecules;
    }

    /**
     * Sets if the number of molecules to read should be limited.
     *
     * @param b <code>true</code> if the number should be limited,
     *            <code>false</code> otherwise
     * @since 2.4
     */
    public void limitNumberOfMolecules(final boolean b) {
        m_limitMolecules = b;
    }
    
    /**
     * Returns the maximum number of molecules to read.
     *
     * @return the number of molecules
     * @since 2.4
     * @see #limitNumberOfMolecules()
     */
    public int moleculeLimit() {
        return m_limit;
    }
    
    /**
     * Sets the maximum number of molecules to read.
     *
     * @param limit the number of molecules
     * @since 2.4
     * @see #limitNumberOfMolecules(boolean)
     */
    public void moleculeLimit(final int limit) {
        m_limit = limit;
    }

    /**
     * Returns if a column containing the source file name should be added to
     * the output table.
     *
     * @return <code>true</code> if a column should be added, <code>false</code>
     *         otherwise
     * @since 2.4
     */
    public boolean sourceLocationColumn() {
        return m_sourceLocationColumn;
    }

    /**
     * Sets if a column containing the source file name should be added to the
     * output table.
     *
     * @param b <code>true</code> if a column should be added,
     *            <code>false</code> otherwise
     * @since 2.4
     */
    public void sourceLocationColumn(final boolean b) {
        m_sourceLocationColumn = b;
    }

    /**
     * Returns the URLs to read from.
     *
     * @return a list of URLs
     * @since 2.4
     */
    public List<URL> urls() {
        return Collections.unmodifiableList(m_urls);
    }

    /**
     * Sets the URLs to read from.
     *
     * @param urls a list of URLs
     * @since 2.4
     */
    public void urls(final List<URL> urls) {
        m_urls.clear();
        m_urls.addAll(urls);
    }

    /**
     * Returns the file's name (may also be an URL).
     *
     * @return the file's name
     * @deprecated use {@link #urls()} instead, this method returns the first
     *             URL only
     */
    @Deprecated
    public String fileName() {
        if (m_urls.size() > 0) {
            return m_urls.get(0).toString();
        } else {
            return null;
        }
    }

    /**
     * Sets the file's name (may also be an URL).
     *
     * @param name the file's name
     * @deprecated use {@link #urls(List)} instead, this method sets the first
     *             URL only
     */
    @Deprecated
    public void fileName(final String name) {
        m_urls.clear();
        try {
            m_urls.add(new URL(name));
        } catch (MalformedURLException ex) {
            // ignore it
        }
    }

    /**
     * Returns if the molecules' names should be used as row keys.
     *
     * @return <code>true</code> if the names should be taken as row keys,
     *         <code>false</code> if row keys should be generated
     */
    public boolean useRowID() {
        return m_useRowID;
    }

    /**
     * Sets if the molecules' names should be used as row keys.
     *
     * @param b <code>true</code> if the names should be taken as row keys,
     *            <code>false</code> if row keys should be generated
     */
    public void useRowID(final boolean b) {
        m_useRowID = b;
    }

    /**
     * Returns if the molecules' names should be put into a column.
     *
     * @return <code>true</code> if the names should be put into a column,
     *         <code>false</code> if row keys should be generated
     */
    public boolean extractName() {
        return m_extractName;
    }

    /**
     * Sets if the molecule's names should be put into a column.
     *
     * @param b <code>true</code> if the names should be put into a column,
     *            <code>false</code> if row keys should be generated
     */
    public void extractName(final boolean b) {
        m_extractName = b;
    }

    /**
     * Returns if complete SDF molecules should be extracted into a column.
     *
     * @return <code>true</code> if a column containing the complete SDF
     *         structured should be created, <code>false</code> otherwise
     */
    public boolean extractSDF() {
        return m_extractSDF;
    }

    /**
     * Sets if complete SDF molecules should be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the complete SDF
     *            structured should be created, <code>false</code> otherwise
     */
    public void extractSDF(final boolean b) {
        m_extractSDF = b;
    }

    /**
     * Returns if the molecules' Molfile blocks should be extracted into a
     * column.
     *
     * @return <code>true</code> if a column containing the Molfile blocks
     *         should be created, <code>false</code> otherwise
     */
    public boolean extractMol() {
        return m_extractMol;
    }

    /**
     * Sets if the molecules' Molfile blocks should be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the Molfile blocks
     *            should be created, <code>false</code> otherwise
     */
    public void extractMol(final boolean b) {
        m_extractMol = b;
    }

    /**
     * Returns if the molecules' Ctab blocks should be extracted into a column.
     *
     * @return <code>true</code> if a column containing the Ctab blocks should
     *         be created, <code>false</code> otherwise
     */
    public boolean extractCtab() {
        return m_extractCTab;
    }

    /**
     * Sets if the molecules' Ctab blocks should be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the Ctab blocks should
     *            be created, <code>false</code> otherwise
     */
    public void extractCTab(final boolean b) {
        m_extractCTab = b;
    }

    /**
     * Returns if the molecules' atom and bond counts be extracted into a
     * column.
     *
     * @return <code>true</code> if a column containing the counts should be
     *         created, <code>false</code> otherwise
     */
    public boolean extractCounts() {
        return m_extractCounts;
    }

    /**
     * Sets if the molecules' atom and bond counts be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the counts should be
     *            created, <code>false</code> otherwise
     */
    public void extractCounts(final boolean b) {
        m_extractCounts = b;
    }

    /**
     * Clears all properties.
     */
    public void clearProperties() {
        m_properties.clear();
    }

    /**
     * Adds a new property.
     *
     * @param prop a property
     */
    public void addProperty(final Property prop) {
        m_properties.add(prop);
    }

    /**
     * Returns the collection of all properties.
     *
     * @return an unmodifiable collection
     */
    public Collection<Property> properties() {
        return Collections.unmodifiableCollection(m_properties);
    }
    	
	public String colName(){
		return m_colName;
	}
	
	public void colName(final String colName){
		m_colName = colName;
	}
	
	public boolean isLogBBclassify(){
		return m_isLogBBclassify;
	}
	
	public void isLogBBclassify(final boolean b){
		m_isLogBBclassify = b;
	}
	
	public boolean isLogPSclassify(){
		return m_isLogPSclassify;
	}
	
	public void isLogPSclassify(final boolean b){
		m_isLogPSclassify = b;
	}
	
	public boolean isLogBBreg(){
		return m_isLogBBreg;
	}
	
	public void isLogBBreg(final boolean b){
		m_isLogBBreg = b;
	}
	
	public boolean isLogPSreg(){
		return m_isLogPSreg;
	}
	
	public void isLogPSreg(final boolean b){
		m_isLogPSreg = b;
	}
	
//	public void saveSettings(final NodeSettingsWO settings){
//		settings.addBoolean("isLogBBclassify", m_isLogBBclassify);
//		settings.addBoolean("isLogPSclassify", m_isLogPSclassify);
//		settings.addBoolean("isLogBBreg", m_isLogBBreg);
//		settings.addBoolean("isLogPSreg", m_isLogPSreg);
//	}
	
//	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
//		m_isLogBBclassify = settings.getBoolean("isLogBBclassify");
//		m_isLogPSclassify = settings.getBoolean("isLogPSclassify");
//		m_isLogBBreg = settings.getBoolean("isLogBBreg");
//		m_isLogPSreg = settings.getBoolean("isLogPSreg");
//	}
//	
//	public void loadSettingsForDialog(final NodeSettingsRO settings){
//		m_isLogBBclassify = settings.getBoolean("isLogBBclassify",true);
//		m_isLogPSclassify = settings.getBoolean("isLogPSclassify",true);
//		m_isLogBBreg = settings.getBoolean("isLogBBreg",true);
//		m_isLogPSreg = settings.getBoolean("isLogPSreg",true);
//	}
    
    
    /**
     * Saves all settings into the given node settings object.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        String[] temp = new String[m_urls.size()];
        for (int i = 0; i < m_urls.size(); i++) {
            temp[i] = m_urls.get(i).toString();
        }

        settings.addStringArray("urls", temp);
        settings.addBoolean("useRowID", m_useRowID);
        settings.addBoolean("extractName", m_extractName);
        settings.addBoolean("extractSDF", m_extractSDF);
        settings.addBoolean("extractMol", m_extractMol);
        settings.addBoolean("extractCTab", m_extractCTab);
        settings.addBoolean("extractCounts", m_extractCounts);
        settings.addBoolean("sourceLocationColumn", m_sourceLocationColumn);
        settings.addBoolean("limitMolecules", m_limitMolecules);
        settings.addInt("limit", m_limit);
        settings.addBoolean("extractAllProperties", m_extractAllProperties);

		settings.addBoolean("isLogBBclassify", m_isLogBBclassify);
		settings.addBoolean("isLogPSclassify", m_isLogPSclassify);
		settings.addBoolean("isLogBBreg", m_isLogBBreg);
		settings.addBoolean("isLogPSreg", m_isLogPSreg);
        
        Config props = settings.addConfig("properties");

        props.addInt("count", m_properties.size());
        for (int i = 0; i < m_properties.size(); i++) {
            props.addBoolean("extract_" + i, m_properties.get(i).extract);
            props.addString("name_" + i, m_properties.get(i).name);
            props.addString("type_" + i, m_properties.get(i).type.getName());
        }
    }

    /**
     * Loads all settings from the given node settings object.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if a setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_useRowID = settings.getBoolean("useRowID");
        m_extractName = settings.getBoolean("extractName", false);
        m_extractSDF = settings.getBoolean("extractSDF");
        m_extractMol = settings.getBoolean("extractMol");
        m_extractCTab = settings.getBoolean("extractCTab");
        m_extractCounts = settings.getBoolean("extractCounts");
        /** @since 2.4 */
        m_sourceLocationColumn =
                settings.getBoolean("sourceLocationColumn", false);
        /** @since 2.4 */
        m_limitMolecules = settings.getBoolean("limitMolecules", false);
        /** @since 2.4 */
        m_limit = settings.getInt("limit", 100);
        /** @since 2.4 */
        m_extractAllProperties = settings.getBoolean("extractAllProperties", false);
        
		m_isLogBBclassify = settings.getBoolean("isLogBBclassify");
		m_isLogPSclassify = settings.getBoolean("isLogPSclassify");
		m_isLogBBreg = settings.getBoolean("isLogBBreg");
		m_isLogPSreg = settings.getBoolean("isLogPSreg");
        
        m_properties.clear();
        Config props = settings.getConfig("properties");
        int count = props.getInt("count");

        for (int i = 0; i < count; i++) {
            try {
                m_properties.add(new Property(props.getBoolean("extract_" + i),
                        props.getString("name_" + i), Class.forName(props
                                .getString("type_" + i))));
            } catch (ClassNotFoundException ex) {
                throw new InvalidSettingsException(ex);
            }
        }

        m_urls.clear();
        if (settings.containsKey("urls")) {
            for (String s : settings.getStringArray("urls")) {
                try {
                    m_urls.add(new URL(s));
                } catch (MalformedURLException ex) {
                    try {
                        m_urls.add(new URL("file:" + s));
                    } catch (MalformedURLException ex1) {
                        throw new InvalidSettingsException("Unparseable URL: "
                                + s, ex);
                    }
                }
            }
        } else {
            String s = settings.getString("filename");
            try {
                m_urls.add(new URL(s));
            } catch (MalformedURLException ex) {
                try {
                    m_urls.add(new URL("file:" + s));
                } catch (MalformedURLException ex1) {
                    throw new InvalidSettingsException("Unparseable URL: " + s,
                            ex);
                }
            }
        }
    }

    /**
     * Loads all settings from the given node settings object, using default
     * values if a setting is missing.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_useRowID = settings.getBoolean("useRowID", true);
        m_extractName = settings.getBoolean("extractName", false);
        m_extractSDF = settings.getBoolean("extractSDF", true);
        m_extractMol = settings.getBoolean("extractMol", false);
        m_extractCTab = settings.getBoolean("extractCTab", false);
        m_extractCounts = settings.getBoolean("extractCounts", false);
        m_sourceLocationColumn =
                settings.getBoolean("sourceLocationColumn", false);
        m_limitMolecules = settings.getBoolean("limitMolecules", false);
        m_limit = settings.getInt("limit", 100);

        m_extractAllProperties = settings.getBoolean("extractAllProperties", false);
		m_isLogBBclassify = settings.getBoolean("isLogBBclassify",true);
		m_isLogPSclassify = settings.getBoolean("isLogPSclassify",true);
		m_isLogBBreg = settings.getBoolean("isLogBBreg",true);
		m_isLogPSreg = settings.getBoolean("isLogPSreg",true);
        m_properties.clear();

        try {
            Config props = settings.getConfig("properties");

            int count = props.getInt("count");

            for (int i = 0; i < count; i++) {
                m_properties.add(new Property(props.getBoolean("extract_" + i),
                        props.getString("name_" + i), Class.forName(props
                                .getString("type_" + i))));
            }
        } catch (Exception ex) {
            // just ignore it
        }

        m_urls.clear();
        if (settings.containsKey("urls")) {
            for (String s : settings.getStringArray("urls", new String[0])) {
                try {
                    m_urls.add(new URL(s));
                } catch (MalformedURLException ex) {
                    try {
                        m_urls.add(new URL("file:" + s));
                    } catch (MalformedURLException ex1) {
                        // ignore it
                    }
                }
            }
        } else if (settings.containsKey("filename")) {
            String s = settings.getString("filename", "");
            try {
                m_urls.add(new URL(s));
            } catch (MalformedURLException ex) {
                try {
                    m_urls.add(new URL("file:" + s));
                } catch (MalformedURLException ex1) {
                    // ignore it
                }
            }
        }
    }

}
