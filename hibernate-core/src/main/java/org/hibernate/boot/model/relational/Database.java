/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.Table;

/**
 * Keeps track of boot-time descriptors for various database namespaces and objects.
 *
 * @implNote Note about precedence of the namespace used for the various database
 * objects...<ol>
 *     <li>
 *         Explicit namespace specified for the object.  {@link Table#catalog()},
 *         {@link Table#schema()}, etc.
 *     </li>
 *     <li>
 *         Default namespace specified by `persistence-unit-defaults#catalog` and
 *         `persistence-unit-defaults#schema` from orm.xml
 *     </li>
 *     <li>
 *         Default namespace specified in the mapping file (orm.xml and hbm.xml) which
 *         maps the database object - `entity-mappings#catalog` and
 *         `entity-mappings#schema` from orm.xml; `hibernate-mapping#schema`
 *         and `hibernate-mapping#schema` from hbm.xml
 *     </li>
 *     <li>
 *         Defaults specified as properties `hibernate.default_catalog` and
 *         `hibernate.default_schema`
 *     </li>
 * </ol>
 */
public class Database {

	private final Dialect dialect;
	private final TypeConfiguration typeConfiguration;
	private final JdbcEnvironment jdbcEnvironment;
	private final Map<Namespace.Name,Namespace> namespaceMap = new TreeMap<>();
	private final Map<String,AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new HashMap<>();
	private final ServiceRegistry serviceRegistry;
	private final PhysicalNamingStrategy physicalNamingStrategy;

	private Namespace implicitNamespace;
	private List<InitCommand> initCommands;

	public Database(MetadataBuildingOptions buildingOptions) {
		this( buildingOptions, buildingOptions.getServiceRegistry().getService( JdbcEnvironment.class ) );
	}

	public Database(MetadataBuildingOptions buildingOptions, JdbcEnvironment jdbcEnvironment) {
		this.serviceRegistry = buildingOptions.getServiceRegistry();
		this.typeConfiguration = buildingOptions.getTypeConfiguration();
		this.jdbcEnvironment = jdbcEnvironment;
		this.physicalNamingStrategy = buildingOptions.getPhysicalNamingStrategy();
		this.dialect = determineDialect( buildingOptions );

		final Identifier defaultCatalogName = toIdentifier( buildingOptions.getMappingDefaults().getImplicitCatalogName() );
		final Identifier defaultSchemaName = toIdentifier( buildingOptions.getMappingDefaults().getImplicitSchemaName() );

		implicitNamespace = makeNamespace( new Namespace.Name( defaultCatalogName, defaultSchemaName )  );
	}

	private static Dialect determineDialect(MetadataBuildingOptions buildingOptions) {
		final Dialect dialect = buildingOptions.getServiceRegistry().getService( JdbcServices.class ).getDialect();
		if ( dialect != null ) {
			return dialect;
		}

		// Use H2 dialect as default
		return new H2Dialect();
	}

	private Namespace makeNamespace(Namespace.Name name) {
		final Namespace namespace = new Namespace( this.getPhysicalNamingStrategy(), this.getJdbcEnvironment(), name );
		namespaceMap.put( name, namespace );
		return namespace;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	/**
	 * Wrap the raw name of a database object in it's Identifier form accounting for quoting from
	 * any of:<ul>
	 *     <li>explicit quoting in the name itself</li>
	 *     <li>global request to quote all identifiers</li>
	 * </ul>
	 * <p/>
	 * NOTE : quoting from database keywords happens only when building physical identifiers
	 *
	 * @param text The raw object name
	 *
	 * @return The wrapped Identifier form
	 */
	public Identifier toIdentifier(String text) {
		return text == null
				? null
				: jdbcEnvironment.getIdentifierHelper().toIdentifier( text );
	}

	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}

	public Iterable<Namespace> getNamespaces() {
		return namespaceMap.values();
	}

	public Namespace getDefaultNamespace() {
		return implicitNamespace;
	}

	public Namespace locateNamespace(Identifier catalogName, Identifier schemaName) {
		if ( catalogName == null && schemaName == null ) {
			return getDefaultNamespace();
		}

		final Namespace.Name name = new Namespace.Name( catalogName, schemaName );
		Namespace namespace = namespaceMap.get( name );
		if ( namespace == null ) {
			namespace = makeNamespace( name );
		}
		return namespace;
	}

	public Namespace adjustDefaultNamespace(Identifier catalogName, Identifier schemaName) {
		final Namespace.Name name = new Namespace.Name( catalogName, schemaName );
		if ( implicitNamespace.getName().equals( name ) ) {
			return implicitNamespace;
		}

		Namespace namespace = namespaceMap.get( name );
		if ( namespace == null ) {
			namespace = makeNamespace( name );
		}
		implicitNamespace = namespace;
		return implicitNamespace;
	}

	public Namespace adjustDefaultNamespace(String implicitCatalogName, String implicitSchemaName) {
		return adjustDefaultNamespace( toIdentifier( implicitCatalogName ), toIdentifier( implicitSchemaName ) );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		auxiliaryDatabaseObjects.put( auxiliaryDatabaseObject.getExportIdentifier(), auxiliaryDatabaseObject );
	}

	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		return auxiliaryDatabaseObjects == null
				? Collections.emptyList()
				: auxiliaryDatabaseObjects.values();
	}

	public Collection<InitCommand> getInitCommands() {
		return initCommands == null
				? Collections.emptyList()
				: initCommands;
	}

	public void addInitCommand(InitCommand initCommand) {
		if ( initCommands == null ) {
			initCommands = new ArrayList<>();
		}
		initCommands.add( initCommand );
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}
}
